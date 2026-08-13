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

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.plugin.server.ClientEventHandler;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.service.manage.ClientProto.ClientEvent;
import com.tencent.polaris.specification.api.v1.service.manage.PolarisGRPCGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WatchClientEvents 双向事件流，用于配置生效实时查询。
 * 建流后发送 WATCH 首帧自证身份，持续接收服务端 PUSH 查询并回 ACK。
 * 断流时经 {@link GrpcConnector} 的任务调度机制重建（对齐 SpecStreamClient 的连接管理模式），
 * 重建成功后重发 WATCH 首帧；UNIMPLEMENTED 表示服务端未发布该 RPC，永久停连。
 * 本流不进空闲关流清理链路——服务端按需触发，可能数小时无帧，存活性只依赖 channel keepalive。
 * <p>
 * 处理逻辑协议无关，由 {@link ClientEventHandler} 实现；本类只负责建流、重连、收发与资源管理。
 *
 * @author evelynwei
 */
public class ClientEventStream implements StreamObserver<ClientEvent>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientEventStream.class);

    /** 服务端 client 缓存未命中（NOT_FOUND）记 warn 的连续失败次数上限，属预期内启动竞态可自愈。 */
    private static final int NOT_FOUND_WARN_COUNT = 3;

    /** 单条处理失败时的降级应答，避免服务端同步等待超时。 */
    private static final String FALLBACK_ACK = "{\"applied\":false,\"reason\":\"internal_error\"}";

    /**
     * 同步锁
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
     * 查询处理线程：单线程，与接收循环顺序一致，且 StreamObserver 非线程安全
     */
    private final ExecutorService queryExecutor = Executors
            .newSingleThreadExecutor(new NamedThreadFactory("client-event-query"));

    /**
     * 连接对象
     */
    private final Connection connection;

    /**
     * GRPC stream 客户端（上行）
     */
    private final StreamObserver<ClientEvent> requestObserver;

    /**
     * 最近更新时间
     */
    private final AtomicLong lastRecvTimeMs = new AtomicLong(0);

    /**
     * 创建时间
     */
    private final long createTimeMs;

    private final AtomicBoolean unimplemented = new AtomicBoolean(false);

    /** 连续建流/接收失败次数，用于 NOT_FOUND 分级（仅 GrpcConnector 调度线程读写） */
    private int failCount;

    public ClientEventStream(GrpcConnector grpcConnector, Connection connection, ClientEventHandler handler) {
        this.grpcConnector = grpcConnector;
        this.connection = connection;
        this.clientId = grpcConnector.getClientInstanceId();
        this.handler = handler;
        this.createTimeMs = System.currentTimeMillis();
        PolarisGRPCGrpc.PolarisGRPCStub stub = PolarisGRPCGrpc.newStub(connection.getChannel());
        stub = GrpcUtil.attachAccessToken(grpcConnector.getConnectorConfig().getToken(), stub);
        this.requestObserver = stub.watchClientEvents(this);
        // 发送 WATCH 首帧自证身份，client_id 与 ReportClient 上报一致
        sendWatch();
    }

    private void sendWatch() {
        requestObserver.onNext(ClientEvent.newBuilder()
                .setType(ClientEvent.ClientEventType.WATCH)
                .setClientId(clientId)
                .build());
    }

    @Override
    public void onNext(ClientEvent event) {
        lastRecvTimeMs.set(System.currentTimeMillis());
        // 忽略非 PUSH（服务端理论上只下发 PUSH）；UNIMPLEMENTED 等错误在 onError 统一处理
        if (event.getType() != ClientEvent.ClientEventType.PUSH) {
            return;
        }
        queryExecutor.submit(() -> handlePush(event));
    }

    private void handlePush(ClientEvent event) {
        try {
            String ackContent = handler.onPush(event.getIndex(), event.getContent());
            sendAck(event.getIndex(), ackContent);
        } catch (Throwable t) {
            LOG.error("[ClientEvent] handle push failed, index = {}, clientId = {}", event.getIndex(), clientId, t);
            sendAck(event.getIndex(), FALLBACK_ACK);
        }
    }

    private void sendAck(long index, String ackContent) {
        if (isEndStream()) {
            return;
        }
        try {
            synchronized (clientLock) {
                requestObserver.onNext(ClientEvent.newBuilder()
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
        exceptionCallback(new StatusRuntimeException(Status.INTERNAL.withDescription("EOF")));
    }

    /**
     * 异常回调：关流、上报连接故障、触发重建（对齐 SpecStreamClient#exceptionCallback）。
     *
     * @param t 异常
     */
    private void exceptionCallback(Throwable t) {
        closeStream(false);
        if (null != t && null != t.getMessage() && t.getMessage().contains("EOF")) {
            LOG.debug("[ClientEvent] stream EOF, clientId = {}", clientId);
        } else {
            LOG.error("[ClientEvent] stream exception, clientId = {}", clientId, t);
        }
        // report down
        connection.reportFail(ErrorCode.NETWORK_ERROR);
        // UNIMPLEMENTED 表示服务端未发布该 RPC，永久停连（含被包装在 cause 链里的情况）
        if (isGrpcCode(t, Status.Code.UNIMPLEMENTED)) {
            unimplemented.set(true);
            LOG.warn("[ClientEvent] unimplemented by server, watcher disabled, clientId = {}", clientId);
            return;
        }
        // 触发重建：复用 GrpcConnector 的任务调度（延迟、线程池、destroy 检查由其统一处理）
        grpcConnector.retryClientEventStream(this);
    }

    /**
     * 关闭流（对齐 SpecStreamClient#closeStream）。
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
            LOG.info("[ClientEvent] connection {} start to closeSend", connection.getConnID());
            requestObserver.onCompleted();
        }
        connection.release(GrpcUtil.OP_KEY_WATCH_CLIENT_EVENTS);
    }

    private boolean isEndStream() {
        return endStream.get();
    }

    /**
     * 判断错误是否携带指定 gRPC status code。服务端错误可能被包装，逐层解 cause 判断。
     */
    private boolean isGrpcCode(Throwable t, Status.Code code) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof StatusRuntimeException
                    && ((StatusRuntimeException) cur).getStatus().getCode() == code) {
                return true;
            }
        }
        return false;
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
     * 记录一次建流失败并返回是否应按 warn 记录（NOT_FOUND 启动竞态，前 N 次可自愈）。
     *
     * @param t 建流异常
     * @return true 记 warn，false 记 error
     */
    public boolean recordConnectFailure(Throwable t) {
        failCount++;
        return failCount <= NOT_FOUND_WARN_COUNT && isGrpcCode(t, Status.Code.NOT_FOUND);
    }

    /**
     * 建流成功后重置失败计数。
     */
    public void resetFailCount() {
        failCount = 0;
    }

    /**
     * 关流并停止重建（对齐 closeStream(true)，且不再触发 retry）。
     */
    @Override
    public void close() {
        closeStream(true);
    }
}
