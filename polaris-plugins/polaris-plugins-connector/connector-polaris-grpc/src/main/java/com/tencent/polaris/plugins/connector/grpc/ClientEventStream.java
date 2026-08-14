/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.plugin.server.ClientEventHandler;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.service.manage.ClientProto.ClientEvent;
import com.tencent.polaris.specification.api.v1.service.manage.PolarisGRPCGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WatchClientEvents 双向事件流，用于配置生效实时查询。
 * 建流后发送 WATCH 首帧自证身份，持续接收服务端 PUSH 查询并回 ACK。
 * 断流时经 {@link GrpcConnector} 的任务调度机制重建（对齐 SpecStreamClient 的连接管理模式），
 * 重建成功后重发 WATCH 首帧；UNIMPLEMENTED 表示服务端未发布该 RPC，永久停连。
 * 本流不进空闲关流清理链路——服务端按需触发，可能数小时无帧，存活性只依赖 channel keepalive。
 * <p>
 * 处理逻辑协议无关，由 {@link ClientEventHandler} 实现；本类只负责建流、收发与资源管理。
 * 构造与建流分离：先构造（纯赋值）并由 {@link GrpcConnector} 登记到活跃流引用，再调 {@link #start()} 建流，
 * 避免构造期内异步 onError 到达时重建请求因引用未登记而被丢弃。
 *
 * @author evelynwei
 */
public class ClientEventStream implements StreamObserver<ClientEvent>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientEventStream.class);

    /**
     * 同步锁：StreamObserver 非线程安全，onNext/onCompleted 全部经此锁串行
     */
    private final Object clientLock = new Object();

    /**
     * 连接是否可用
     */
    private final AtomicBoolean endStream = new AtomicBoolean(false);

    private final GrpcConnector grpcConnector;

    private final String clientId;

    private final ClientEventHandler handler;

    /**
     * 查询处理线程：单线程，与接收循环顺序一致
     */
    private final ExecutorService queryExecutor = Executors
            .newSingleThreadExecutor(new NamedThreadFactory("client-event-query"));

    /**
     * 连接对象
     */
    private final Connection connection;

    /**
     * GRPC stream 客户端（上行），start() 时赋值
     */
    private volatile StreamObserver<ClientEvent> requestObserver;

    private final AtomicBoolean unimplemented = new AtomicBoolean(false);

    public ClientEventStream(GrpcConnector grpcConnector, Connection connection, ClientEventHandler handler) {
        this.grpcConnector = grpcConnector;
        this.connection = connection;
        this.clientId = grpcConnector.getClientInstanceId();
        this.handler = handler;
    }

    /**
     * 建立双向流并发送 WATCH 首帧。调用前必须已登记到 GrpcConnector 的活跃流引用。
     * 失败时自行关流（closeStream，幂等释放连接）并重抛异常，由调用方走重建调度。
     */
    public void start() {
        if (isEndStream()) {
            return;
        }
        try {
            PolarisGRPCGrpc.PolarisGRPCStub stub = PolarisGRPCGrpc.newStub(connection.getChannel());
            stub = GrpcUtil.attachAccessToken(grpcConnector.getConnectorConfig().getToken(), stub);
            requestObserver = stub.watchClientEvents(this);
            // 发送 WATCH 首帧自证身份，client_id 与 ReportClient 上报一致
            sendWatch();
        } catch (RuntimeException | Error t) {
            closeStream(false);
            throw t;
        }
    }

    private void sendWatch() {
        StreamObserver<ClientEvent> observer = requestObserver;
        if (observer == null) {
            return;
        }
        synchronized (clientLock) {
            observer.onNext(ClientEvent.newBuilder()
                    .setType(ClientEvent.ClientEventType.WATCH)
                    .setClientId(clientId)
                    .build());
        }
    }

    @Override
    public void onNext(ClientEvent event) {
        // 忽略非 PUSH（服务端理论上只下发 PUSH）；UNIMPLEMENTED 等错误在 onError 统一处理
        if (event.getType() != ClientEvent.ClientEventType.PUSH) {
            return;
        }
        if (isEndStream()) {
            // 关流后不再接收处理，避免 submit 到已 shutdown 的 executor
            return;
        }
        try {
            queryExecutor.submit(() -> handlePush(event));
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // 与关流并发的在途帧，直接丢弃
            LOG.debug("[ClientEvent] push rejected, stream closing, clientId = {}", clientId);
        }
    }

    private void handlePush(ClientEvent event) {
        try {
            String ackContent = handler.onPush(event.getIndex(), event.getContent());
            if (ackContent == null) {
                LOG.error("[ClientEvent] handler returned null ack, index = {}, clientId = {}", event.getIndex(),
                        clientId);
                return;
            }
            sendAck(event.getIndex(), ackContent);
        } catch (Throwable t) {
            // 对齐 Go：单条处理失败只记日志不回 ACK（此时无法构造可信应答），由服务端按超时处理
            LOG.error("[ClientEvent] handle push failed, index = {}, clientId = {}", event.getIndex(), clientId, t);
        }
    }

    private void sendAck(long index, String ackContent) {
        if (isEndStream()) {
            return;
        }
        StreamObserver<ClientEvent> observer = requestObserver;
        if (observer == null) {
            return;
        }
        try {
            synchronized (clientLock) {
                observer.onNext(ClientEvent.newBuilder()
                        .setType(ClientEvent.ClientEventType.ACK)
                        .setClientId(clientId)
                        .setIndex(index)
                        .setContent(ackContent)
                        .build());
            }
        } catch (Throwable t) {
            LOG.warn("[ClientEvent] send ack failed, index = {}, clientId = {}", index, clientId, t);
        }
    }

    @Override
    public void onError(Throwable t) {
        exceptionCallback(t);
    }

    @Override
    public void onCompleted() {
        exceptionCallback(new io.grpc.StatusRuntimeException(Status.INTERNAL.withDescription("EOF")));
    }

    /**
     * 异常回调：关流、分级日志、上报连接故障、触发重建（对齐 SpecStreamClient#exceptionCallback）。
     *
     * @param t 异常
     */
    private void exceptionCallback(Throwable t) {
        closeStream(false);
        // UNIMPLEMENTED 表示服务端未发布该 RPC（能力不匹配而非连接故障）：不上报故障、永久停连
        if (GrpcUtil.hasGrpcCode(t, Status.Code.UNIMPLEMENTED)) {
            unimplemented.set(true);
            LOG.warn("[ClientEvent] unimplemented by server, watcher disabled, clientId = {}", clientId);
            return;
        }
        // 分级日志（瞬时网络错误/NOT_FOUND 启动竞态记 warn）与失败计数统一由 GrpcConnector 管理
        grpcConnector.noteClientEventFailure(t);
        // report down
        connection.reportFail(ErrorCode.NETWORK_ERROR);
        // 触发重建：复用 GrpcConnector 的任务调度（延迟、线程池、destroy 检查由其统一处理）
        grpcConnector.retryClientEventStream(this);
    }

    /**
     * 关闭流（对齐 SpecStreamClient#closeStream）。连接释放幂等（CAS 保证只 release 一次）。
     *
     * @param closeSend 是否发送 EOF
     */
    public void closeStream(boolean closeSend) {
        queryExecutor.shutdownNow();
        boolean endStreamOK = endStream.compareAndSet(false, true);
        if (!endStreamOK) {
            return;
        }
        if (closeSend) {
            StreamObserver<ClientEvent> observer = requestObserver;
            if (observer != null) {
                try {
                    synchronized (clientLock) {
                        observer.onCompleted();
                    }
                } catch (Throwable t) {
                    LOG.debug("[ClientEvent] onCompleted failed, clientId = {}", clientId, t);
                }
            }
        }
        connection.release(GrpcUtil.OP_KEY_WATCH_CLIENT_EVENTS);
    }

    private boolean isEndStream() {
        return endStream.get();
    }

    /**
     * 服务端未实现该 RPC 后不再重建。
     *
     * @return 是否已停止
     */
    public boolean isUnimplemented() {
        return unimplemented.get();
    }

    /**
     * 关流并停止重建（对齐 closeStream(true)，且不再触发 retry）。
     */
    @Override
    public void close() {
        closeStream(true);
    }
}
