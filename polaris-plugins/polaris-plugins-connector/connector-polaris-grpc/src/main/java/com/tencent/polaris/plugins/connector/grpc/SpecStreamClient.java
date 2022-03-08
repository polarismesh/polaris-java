package com.tencent.polaris.plugins.connector.grpc;

import com.google.protobuf.StringValue;
import com.google.protobuf.TextFormat;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pb.PolarisGRPCGrpc;
import com.tencent.polaris.client.pb.RequestProto;
import com.tencent.polaris.client.pb.ResponseProto;
import com.tencent.polaris.client.pb.ServiceProto;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Type;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于cluster/healthcheck/heartbeat的服务发现
 *
 * @author Haotian Zhang
 */
public class SpecStreamClient implements StreamObserver<ResponseProto.DiscoverResponse> {


    private static final Logger LOG = LoggerFactory.getLogger(SpecStreamClient.class);

    /**
     * 同步锁
     */
    private final Object clientLock = new Object();

    /**
     * 在流中没有结束的任务
     */
    private final Map<ServiceEventKey, ServiceUpdateTask> pendingTask = new HashMap<>();

    /**
     * 连接是否可用
     */
    private final AtomicBoolean endStream = new AtomicBoolean(false);

    /**
     * GRPC stream客户端
     */
    private final StreamObserver<RequestProto.DiscoverRequest> discoverClient;

    /**
     * 连接对象
     */
    private final Connection connection;

    /**
     * 请求ID
     */
    private final String reqId;

    /**
     * 最近更新时间
     */
    private final AtomicLong lastRecvTimeMs = new AtomicLong(0);

    /**
     * 创建时间
     */
    private final long createTimeMs;

    /**
     * 连接空闲时间
     */
    private final long connectionIdleTimeoutMs;

    /**
     * 构造函数
     *
     * @param connection 连接
     * @param connectionIdleTimeoutMs 连接超时
     * @param serviceUpdateTask 初始更新任务
     */
    public SpecStreamClient(Connection connection, long connectionIdleTimeoutMs,
            ServiceUpdateTask serviceUpdateTask) {
        this.connection = connection;
        this.connectionIdleTimeoutMs = connectionIdleTimeoutMs;
        createTimeMs = System.currentTimeMillis();
        reqId = GrpcUtil.nextGetInstanceReqId();
        PolarisGRPCGrpc.PolarisGRPCStub namingStub = PolarisGRPCGrpc.newStub(connection.getChannel());
        GrpcUtil.attachRequestHeader(namingStub, GrpcUtil.nextGetInstanceReqId());
        discoverClient = namingStub.discover(this);
        pendingTask.put(serviceUpdateTask.getServiceEventKey(), serviceUpdateTask);
    }

    /**
     * 关闭流
     *
     * @param closeSend 是否发送EOF
     */
    public void closeStream(boolean closeSend) {
        boolean endStreamOK = endStream.compareAndSet(false, true);
        if (!endStreamOK) {
            return;
        }
        if (closeSend) {
            LOG.info("[ServerConnector]connection {} start to closeSend", connection.getConnID());
            discoverClient.onCompleted();
        }
        connection.release(GrpcUtil.OP_KEY_DISCOVER);
    }

    private boolean isEndStream() {
        return endStream.get();
    }


    public String getReqId() {
        return reqId;
    }

    /**
     * 发送请求
     *
     * @param serviceUpdateTask 服务更新任务
     */
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        ServiceEventKey serviceEventKey = serviceUpdateTask.getServiceEventKey();
        ServiceProto.Service.Builder builder = ServiceProto.Service.newBuilder();
        builder.setName(StringValue.newBuilder().setValue(serviceEventKey.getServiceKey().getService()).build());
        builder.setNamespace(StringValue.newBuilder().setValue(serviceEventKey.getServiceKey().getNamespace()).build());

        RequestProto.DiscoverRequest.Builder req = RequestProto.DiscoverRequest.newBuilder();
        req.setType(GrpcUtil.buildDiscoverRequestType(serviceEventKey.getEventType())); // switch
        req.setService(builder);
        if (serviceUpdateTask.getTaskType() == Type.FIRST) {
            LOG.info("[ServerConnector]send request(id={}) to {} for service {}", reqId, connection.getConnID(),
                    serviceEventKey);
        } else {
            LOG.debug("[ServerConnector]send request(id={}) to {} for service {}", reqId, connection.getConnID(),
                    serviceEventKey);
        }
        discoverClient.onNext(req.build());
    }

    /**
     * 检查该应答是否合法，不合法则走异常流程
     *
     * @param response 服务端应答
     * @return 合法返回true，否则返回false
     */
    private ValidResult validMessage(ResponseProto.DiscoverResponse response) {
        ErrorCode errorCode = ErrorCode.Success;
        if (response.hasCode()) {
            errorCode = ServerCodes.convertServerErrorToRpcError(response.getCode().getValue());
        }
        ServiceProto.Service service;
        if (CollectionUtils.isNotEmpty(response.getServicesList())) {
            service = response.getServicesList().get(0);
        } else {
            service = response.getService();
        }
        if (null == service || StringUtils.isEmpty(service.getNamespace().getValue()) || StringUtils
                .isEmpty(service.getName().getValue())) {
            return new ValidResult(null, ErrorCode.INVALID_SERVER_RESPONSE,
                    "service is empty, response text is " + response.toString());
        }
        EventType eventType = GrpcUtil.buildEventType(response.getType());
        if (eventType == EventType.UNKNOWN) {
            return new ValidResult(null, ErrorCode.INVALID_SERVER_RESPONSE,
                    "invalid event type " + response.getType());
        }
        ServiceEventKey serviceEventKey = new ServiceEventKey(
                new ServiceKey(service.getNamespace().getValue(), service.getName().getValue()), eventType);
        if (errorCode == ErrorCode.SERVER_ERROR) {
            //返回了500错误
            return new ValidResult(serviceEventKey, errorCode, "invalid event type " + response.getType());
        }

        return new ValidResult(serviceEventKey, ErrorCode.Success, "");
    }

    /**
     * 异常回调
     *
     * @param validResult 异常
     */
    public void exceptionCallback(ValidResult validResult) {
        this.closeStream(false);
        LOG.error("[ServerConnector]exceptionCallback: errCode {}, info {}, serviceEventKey {}",
                validResult.getErrorCode(), validResult.getMessage(), validResult.getServiceEventKey());
        //report down
        connection.reportFail();
        List<ServiceUpdateTask> notifyTasks = new ArrayList<>();
        synchronized (clientLock) {
            ServiceEventKey serviceEventKey = validResult.getServiceEventKey();
            if (null == serviceEventKey) {
                if (CollectionUtils.isNotEmpty(pendingTask.values())) {
                    notifyTasks.addAll(pendingTask.values());
                    pendingTask.clear();
                }
            } else {
                ServiceUpdateTask task = pendingTask.remove(serviceEventKey);
                if (null != task) {
                    notifyTasks.add(task);
                }
            }
        }
        for (ServiceUpdateTask notifyTask : notifyTasks) {
            notifyTask.retry();
        }
    }

    /**
     * 正常回调
     *
     * @param response 应答说
     */
    public void onNext(ResponseProto.DiscoverResponse response) {
        lastRecvTimeMs.set(System.currentTimeMillis());
        ValidResult validResult = validMessage(response);
        if (validResult.errorCode != ErrorCode.Success) {
            exceptionCallback(validResult);
            return;
        }
        ServiceProto.Service service = response.getService();
        ServiceKey serviceKey = new ServiceKey(service.getNamespace().getValue(), service.getName().getValue());
        EventType eventType = GrpcUtil.buildEventType(response.getType());
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, eventType);
        ServiceUpdateTask updateTask;
        synchronized (clientLock) {
            updateTask = pendingTask.remove(serviceEventKey);
        }
        if (null == updateTask) {
            LOG.error("[ServerConnector]callback not found for:{}", TextFormat.shortDebugString(service));
            return;
        }
        if (updateTask.getTaskType() == Type.FIRST) {
            LOG.info("[ServerConnector]receive response for {}", serviceEventKey);
        } else {
            LOG.debug("[ServerConnector]receive response for {}", serviceEventKey);
        }
        PolarisException error;
        if (!response.hasCode() || response.getCode().getValue() == ServerCodes.EXECUTE_SUCCESS) {
            error = null;
        } else {
            int respCode = response.getCode().getValue();
            String info = response.getInfo().getValue();
            error = ServerErrorResponseException.build(respCode,
                    String.format("[ServerConnector]code %d, fail to query service %s from server(%s): %s", respCode,
                            serviceKey, connection.getConnID(), info));
        }
        boolean svcDeleted = updateTask.notifyServerEvent(new ServerEvent(serviceEventKey, response, error));
        if (!svcDeleted) {
            updateTask.addUpdateTaskSet();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        exceptionCallback(new ValidResult(null, ErrorCode.NETWORK_ERROR,
                String.format("stream %s received error from server(%s), error is %s", getReqId(),
                        connection.getConnID().toString(), throwable.getMessage())));
    }

    @Override
    public void onCompleted() {
        exceptionCallback(new ValidResult(null, ErrorCode.NETWORK_ERROR,
                String.format("stream %s EOF by server(%s)", getReqId(), connection.getConnID().toString())));
    }

    /**
     * 同步清理过期的流对象
     */
    public void syncCloseExpireStream() {
        synchronized (clientLock) {
            closeExpireStream();
        }
    }

    private boolean closeExpireStream() {
        //检查是否过期
        long lastRecvMs = lastRecvTimeMs.get();
        long nowMs = System.currentTimeMillis();
        long connIdleTime;
        if (lastRecvMs == 0) {
            //如果没有收到过消息，就通过创建时间来判断
            connIdleTime = nowMs - createTimeMs;
        } else {
            connIdleTime = nowMs - lastRecvMs;
        }
        if (connIdleTime >= connectionIdleTimeoutMs) {
            //连接已经过期
            closeStream(true);
            return true;
        }
        return false;
    }

    /**
     * checkAvailable
     *
     * @param serviceUpdateTask 更新任务
     * @return 是否可用
     */
    public boolean checkAvailable(ServiceUpdateTask serviceUpdateTask) {
        if (isEndStream()) {
            return false;
        }
        synchronized (clientLock) {
            if (isEndStream()) {
                return false;
            }
            if (closeExpireStream()) {
                return false;
            }
            ServiceEventKey serviceEventKey = serviceUpdateTask.getServiceEventKey();
            ServiceUpdateTask lastUpdateTask = pendingTask.get(serviceEventKey);
            if (null != lastUpdateTask) {
                LOG.warn("[ServerConnector]pending task {} has been overwritten", lastUpdateTask);
            }
            pendingTask.put(serviceEventKey, serviceUpdateTask);
        }
        return true;
    }

    private static class ValidResult {

        final ServiceEventKey serviceEventKey;
        final ErrorCode errorCode;
        final String message;

        public ValidResult(ServiceEventKey serviceEventKey, ErrorCode errorCode, String message) {
            this.serviceEventKey = serviceEventKey;
            this.errorCode = errorCode;
            this.message = message;
        }

        public ServiceEventKey getServiceEventKey() {
            return serviceEventKey;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }
}
