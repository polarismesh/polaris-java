package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.client.pb.RequestProto.DiscoverRequest.DiscoverRequestType;
import com.tencent.polaris.client.pb.ResponseProto;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse.DiscoverResponseType;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GrpcUtil
 *
 * @author vickliu
 * @date
 */
public class GrpcUtil {

    public static final String OP_KEY_REGISTER_INSTANCE = "RegisterInstance";

    public static final String OP_KEY_DEREGISTER_INSTANCE = "DeregisterInstance";

    public static final String OP_KEY_INSTANCE_HEARTBEAT = "InstanceHeartbeat";

    public static final String OP_KEY_DISCOVER = "Discover";

    public static final String OP_KEY_REPORT_CLIENT = "ReportClient";

    /**
     * 请求ID的key
     */
    private static final Metadata.Key<String> KEY_REQUEST_ID =
            Metadata.Key.of("request-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 实例注册的请求ID前缀
     */
    private static final String REQ_ID_PREFIX_REGISTERINSTANCE = "1";

    /**
     * 实例反注册的请求ID前缀
     */
    private static final String REQ_ID_PREFIX_DEREGISTERINSTANCE = "2";

    /**
     * 实例心跳的请求ID前缀
     */
    private static final String REQ_ID_PREFIX_HEARTBEAT = "3";

    /**
     * 获取实例的请求ID前缀
     */
    private static final String REQ_ID_PREFIX_GETINSTANCES = "4";

    /**
     * 实例发现ID发生器
     */
    private static final AtomicLong SEED_INSTANCE_REQ_ID = new AtomicLong();

    /**
     * 实例注册ID发生器
     */
    private static final AtomicLong SEED_REGISTER_REQ_ID = new AtomicLong();

    /**
     * 实例反注册ID发生器
     */
    private static final AtomicLong SEED_DEREGISTER_REQ_ID = new AtomicLong();

    /**
     * 心跳上报ID发生器
     */
    private static final AtomicLong SEED_HEARTBEAT_REQ_ID = new AtomicLong();

    /**
     * 获取下一个实例请求ID
     *
     * @return string
     */
    public static String nextGetInstanceReqId() {
        return String.format("%s%d", REQ_ID_PREFIX_GETINSTANCES, SEED_INSTANCE_REQ_ID.incrementAndGet());
    }

    /**
     * 获取下一个注册请求ID
     *
     * @return string
     */
    public static String nextInstanceRegisterReqId() {
        return String.format("%s%d", REQ_ID_PREFIX_REGISTERINSTANCE, SEED_REGISTER_REQ_ID.incrementAndGet());
    }

    /**
     * 获取下一个反注册请求ID
     *
     * @return string
     */
    public static String nextInstanceDeRegisterReqId() {
        return String.format("%s%d", REQ_ID_PREFIX_DEREGISTERINSTANCE, SEED_DEREGISTER_REQ_ID.incrementAndGet());
    }

    /**
     * 获取下一个心跳请求ID
     *
     * @return string
     */
    public static String nextHeartbeatReqId() {
        return String.format("%s%d", REQ_ID_PREFIX_HEARTBEAT, SEED_HEARTBEAT_REQ_ID.incrementAndGet());
    }


    /**
     * 为GRPC请求添加请求头部
     *
     * @param <T> 桩类型
     * @param stub 请求桩
     * @param nextID 请求ID
     */
    public static <T extends AbstractStub<T>> void attachRequestHeader(T stub, String nextID) {
        Metadata extraHeaders = new Metadata();
        extraHeaders.put(KEY_REQUEST_ID, nextID);
        MetadataUtils.attachHeaders(stub, extraHeaders);
    }


    public static void checkResponse(ResponseProto.Response response) throws PolarisException {
        if (!response.hasCode()) {
            return;
        }
        int respCode = response.getCode().getValue();
        if (respCode == ServerCodes.EXECUTE_SUCCESS || respCode == ServerCodes.EXISTED_RESOURCE) {
            return;
        }
        String info = response.getInfo().getValue();
        throw new PolarisException(ErrorCode.SERVER_USER_ERROR,
                String.format("server error %d: %s", respCode, info));
    }


    public static DiscoverRequestType buildDiscoverRequestType(
            ServiceEventKey.EventType type) {
        switch (type) {
            case INSTANCE:
                return DiscoverRequestType.INSTANCE;
            case ROUTING:
                return DiscoverRequestType.ROUTING;
            case RATE_LIMITING:
                return DiscoverRequestType.RATE_LIMIT;
            case CIRCUIT_BREAKING:
                return DiscoverRequestType.CIRCUIT_BREAKER;
            default:
                return DiscoverRequestType.UNKNOWN;
        }
    }

    public static EventType buildEventType(DiscoverResponseType responseType) {
        switch (responseType) {
            case INSTANCE:
                return EventType.INSTANCE;
            case ROUTING:
                return EventType.ROUTING;
            case RATE_LIMIT:
                return EventType.RATE_LIMITING;
            case CIRCUIT_BREAKER:
                return EventType.CIRCUIT_BREAKING;
            default:
                return EventType.UNKNOWN;
        }
    }

}
