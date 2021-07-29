/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.test.mock.discovery;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pb.CircuitBreakerProto;
import com.tencent.polaris.client.pb.CircuitBreakerProto.CircuitBreaker;
import com.tencent.polaris.client.pb.ModelProto;
import com.tencent.polaris.client.pb.ModelProto.Location;
import com.tencent.polaris.client.pb.PolarisGRPCGrpc;
import com.tencent.polaris.client.pb.RateLimitProto;
import com.tencent.polaris.client.pb.RateLimitProto.RateLimit;
import com.tencent.polaris.client.pb.RequestProto.DiscoverRequest;
import com.tencent.polaris.client.pb.ResponseProto;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse.DiscoverResponseType;
import com.tencent.polaris.client.pb.RoutingProto;
import com.tencent.polaris.client.pb.ServiceProto;
import com.tencent.polaris.client.pb.ServiceProto.Instance;
import com.tencent.polaris.client.pojo.Node;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamingService extends PolarisGRPCGrpc.PolarisGRPCImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(NamingService.class);

    private final Map<ServiceKey, List<Instance>> services = new ConcurrentHashMap<>();

    private final Map<ServiceKey, RoutingProto.Routing> serviceRoutings = new ConcurrentHashMap<>();

    private final Map<ServiceKey, CircuitBreakerProto.CircuitBreaker> serviceCircuitBreakers = new ConcurrentHashMap<>();

    private final Map<ServiceKey, RateLimitProto.RateLimit> serviceRateLimits = new ConcurrentHashMap<>();

    public void addService(ServiceKey serviceKey) {
        services.put(serviceKey, new ArrayList<>());
    }

    public void setRouting(ServiceKey serviceKey, RoutingProto.Routing routing) {
        serviceRoutings.put(serviceKey, routing);
    }

    public void setCircuitBreaker(ServiceKey serviceKey, CircuitBreaker circuitBreaker) {
        serviceCircuitBreakers.put(serviceKey, circuitBreaker);
    }

    public void setRateLimit(ServiceKey serviceKey, RateLimit rateLimit) {
        serviceRateLimits.put(serviceKey, rateLimit);
    }

    public static class InstanceParameter {

        private boolean healthy;
        private boolean isolated;
        private int weight;
        private String protocol;
        private String version;
        private LocationInfo locationInfo;
        private Map<String, String> metadata;

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public boolean isIsolated() {
            return isolated;
        }

        public void setIsolated(boolean isolated) {
            this.isolated = isolated;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public LocationInfo getLocationInfo() {
            return locationInfo;
        }

        public void setLocationInfo(LocationInfo locationInfo) {
            this.locationInfo = locationInfo;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }

    private ServiceProto.Instance buildInstance(ServiceKey svcKey, Node node, InstanceParameter parameter) {
        ServiceProto.Instance.Builder builder = ServiceProto.Instance.newBuilder();
        String instId = UUID.randomUUID().toString();
        builder.setId(StringValue.newBuilder().setValue(instId).build());
        builder.setNamespace(StringValue.newBuilder().setValue(svcKey.getNamespace()).build());
        builder.setService(StringValue.newBuilder().setValue(svcKey.getService()).build());
        builder.setHost(StringValue.newBuilder().setValue(node.getHost()).build());
        builder.setPort(UInt32Value.newBuilder().setValue(node.getPort()).build());
        builder.setHealthy(BoolValue.newBuilder().setValue(parameter.isHealthy()).build());
        builder.setIsolate(BoolValue.newBuilder().setValue(parameter.isIsolated()).build());
        if (StringUtils.isNotBlank(parameter.getProtocol())) {
            builder.setProtocol(StringValue.newBuilder().setValue(parameter.getProtocol()).build());
            builder.putMetadata("protocol", parameter.getProtocol());
        }
        if (StringUtils.isNotBlank(parameter.getVersion())) {
            builder.setVersion(StringValue.newBuilder().setValue(parameter.getVersion()).build());
            builder.putMetadata("version", parameter.getVersion());
        }
        builder.setWeight(UInt32Value.newBuilder().setValue(parameter.getWeight()).build());
        LocationInfo locationInfo = parameter.getLocationInfo();
        if (null != locationInfo) {
            Location.Builder locationBuilder = ModelProto.Location.newBuilder();
            locationBuilder.setRegion(StringValue.newBuilder().setValue(locationInfo.getRegion()).build());
            locationBuilder.setZone(StringValue.newBuilder().setValue(locationInfo.getZone()).build());
            locationBuilder.setCampus(StringValue.newBuilder().setValue(locationInfo.getCampus()).build());
            builder.setLocation(locationBuilder.build());
        }
        Map<String, String> metadata = parameter.getMetadata();
        if (null != metadata) {
            builder.putAllMetadata(metadata);
        }
        return builder.build();
    }

    public void addInstance(ServiceKey svcKey, Node node, InstanceParameter parameter) {
        ServiceProto.Instance instance = buildInstance(svcKey, node, parameter);
        List<Instance> existsInstances = services.get(svcKey);
        if (null == existsInstances) {
            List<Instance> instances = new ArrayList<>();
            instances.add(instance);
            services.put(svcKey, instances);
        } else {
            existsInstances.add(instance);
        }
    }

    /**
     * 批量增加服务实例
     *
     * @param svcKey 服务名
     * @param portStart 起始端口
     * @param instCount 实例数
     * @param parameter 实例参数
     * @return 批量服务实例的IP和端口
     */
    public List<Node> batchAddInstances(ServiceKey svcKey, int portStart, int instCount, InstanceParameter parameter) {
        List<Node> nodes = new ArrayList<>();
        List<Instance> instances = new ArrayList<>();
        for (int i = 0; i < instCount; i++) {
            Node node = new Node("127.0.0.1", portStart + i);
            ServiceProto.Instance nextInstance = buildInstance(svcKey, node, parameter);
            instances.add(nextInstance);
            nodes.add(node);
        }
        List<Instance> existsInstances = services.get(svcKey);
        if (null == existsInstances) {
            services.put(svcKey, instances);
        } else {
            existsInstances.addAll(instances);
        }
        return nodes;
    }

    public void setInstanceHealthyStatus(
            ServiceKey svcKey, Node node, Boolean healthyStatus, Boolean isolated, Integer weight) {
        List<ServiceProto.Instance> instances = services.get(svcKey);
        if (CollectionUtils.isEmpty(instances)) {
            return;
        }
        List<ServiceProto.Instance> newInstances = new ArrayList<>();
        instances.forEach(instance -> {
            if (StringUtils.equals(node.getHost(), instance.getHost().getValue())
                    && node.getPort() == instance.getPort().getValue()) {
                ServiceProto.Instance.Builder builder = instance.toBuilder();
                if (null != healthyStatus) {
                    builder.setHealthy(BoolValue.newBuilder().setValue(healthyStatus));
                }
                if (null != isolated) {
                    builder.setIsolate(BoolValue.newBuilder().setValue(isolated).build());
                }
                if (null != weight) {
                    builder.setWeight(UInt32Value.newBuilder().setValue(weight).build());
                }
                newInstances.add(builder.build());
            } else {
                newInstances.add(instance);
            }
        });
        services.put(svcKey, newInstances);
    }

    @Override
    public void registerInstance(ServiceProto.Instance request,
            StreamObserver<ResponseProto.Response> responseObserver) {
        ServiceKey serviceKey = new ServiceKey(request.getNamespace().getValue(), request.getService().getValue());
        if (!services.containsKey(serviceKey)) {
            responseObserver.onNext(
                    buildResponse(ServerCodes.NOT_FOUND_RESOURCE, String.format("service %s not found", serviceKey),
                            request));
            responseObserver.onCompleted();
            return;
        }
        List<ServiceProto.Instance> instances = services.get(serviceKey);
        if (CollectionUtils.isNotEmpty(instances)) {
            for (ServiceProto.Instance instance : instances) {
                if (instance.getHost().getValue().equals(request.getHost().getValue()) &&
                        instance.getPort().getValue() == request.getPort().getValue()) {
                    responseObserver.onNext(buildResponse(ServerCodes.EXISTED_RESOURCE,
                            String.format("instance %s:%d exists", request.getHost().getValue(),
                                    request.getPort().getValue()), instance));
                    responseObserver.onCompleted();
                    return;
                }
            }
        }
        ServiceProto.Instance.Builder builder = ServiceProto.Instance.newBuilder();
        builder.mergeFrom(request);
        String instId = UUID.randomUUID().toString();
        builder.setId(StringValue.newBuilder().setValue(instId).build());
        ServiceProto.Instance nextInstance = builder.build();
        instances.add(nextInstance);

        ResponseProto.Response.Builder response = ResponseProto.Response.newBuilder();
        response.setCode(UInt32Value.newBuilder().setValue(ServerCodes.EXECUTE_SUCCESS).build());
        response.setInstance(nextInstance);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }


    private ResponseProto.Response buildResponse(int code, String info, ServiceProto.Instance instance) {
        ResponseProto.Response.Builder response = ResponseProto.Response.newBuilder();
        response.setCode(UInt32Value.newBuilder().setValue(code).build());
        if (StringUtils.isNotBlank(info)) {
            response.setInfo(StringValue.newBuilder().setValue(info).build());
        }
        if (null != instance) {
            response.setInstance(instance);
        }
        return response.build();
    }

    @Override
    public void deregisterInstance(ServiceProto.Instance request,
            StreamObserver<ResponseProto.Response> responseObserver) {
        ServiceKey serviceKey = new ServiceKey(request.getNamespace().getValue(), request.getService().getValue());
        if (!services.containsKey(serviceKey)) {
            responseObserver.onNext(
                    buildResponse(ServerCodes.NOT_FOUND_RESOURCE, String.format("service %s not found", serviceKey),
                            request));
            responseObserver.onCompleted();
            return;
        }
        int rIndex = -1;
        List<ServiceProto.Instance> instances = services.get(serviceKey);
        for (int i = 0; i < instances.size(); i++) {
            ServiceProto.Instance instance = instances.get(i);
            if (StringUtils.isNotBlank(request.getId().getValue())) {
                if (StringUtils.equals(request.getId().getValue(), request.getId().getValue())) {
                    rIndex = i;
                    break;
                }
            } else if (StringUtils.equals(request.getHost().getValue(), instance.getHost().getValue())
                    && request.getPort().getValue() == instance.getPort().getValue()) {
                rIndex = i;
                break;
            }
        }
        if (rIndex != -1) {
            instances.remove(rIndex);
        }
        if (CollectionUtils.isEmpty(instances)) {
            //实例被删光则删除服务
            services.remove(serviceKey);
        }
        ResponseProto.Response.Builder response = ResponseProto.Response.newBuilder();
        response.setCode(UInt32Value.newBuilder().setValue(ServerCodes.EXECUTE_SUCCESS).build());
        response.setInstance(request);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void heartbeat(ServiceProto.Instance request, StreamObserver<ResponseProto.Response> responseObserver) {
        ServiceKey serviceKey = new ServiceKey(request.getNamespace().getValue(), request.getService().getValue());
        if (!services.containsKey(serviceKey)) {
            responseObserver.onNext(
                    buildResponse(ServerCodes.NOT_FOUND_RESOURCE, String.format("service %s not found", serviceKey),
                            request));
            responseObserver.onCompleted();
            return;
        }
        ResponseProto.Response.Builder response = ResponseProto.Response.newBuilder();
        response.setCode(UInt32Value.newBuilder().setValue(ServerCodes.EXECUTE_SUCCESS).build());
        response.setInstance(request);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    private ResponseProto.DiscoverResponse buildServiceResponse(
            int code, String info, DiscoverRequest req) {

        ResponseProto.DiscoverResponse.Builder builder = ResponseProto.DiscoverResponse.newBuilder();
        builder.setCode(UInt32Value.newBuilder().setValue(code).build());

        CircuitBreakerProto.CircuitBreaker circuitBreaker;
        RateLimitProto.RateLimit rateLimit;
        RoutingProto.Routing routing;
        List<ServiceProto.Instance> instances;

        ServiceProto.Service service = req.getService();
        ServiceKey serviceKey = new ServiceKey(service.getNamespace().getValue(), service.getName().getValue());

        switch (req.getType()) {
            case INSTANCE:
                instances = services.get(serviceKey);
                if (CollectionUtils.isNotEmpty(instances)) {
                    builder.addAllInstances(instances);
                }
                builder.setType(DiscoverResponseType.INSTANCE);
                break;
            case ROUTING:
                routing = serviceRoutings.get(serviceKey);
                if (null != routing) {
                    builder.setRouting(routing);
                }
                builder.setType(DiscoverResponseType.ROUTING);
                break;
            case CIRCUIT_BREAKER:
                circuitBreaker = serviceCircuitBreakers.get(serviceKey);
                if (null != circuitBreaker) {
                    builder.setCircuitBreaker(circuitBreaker);
                }
                builder.setType(DiscoverResponseType.CIRCUIT_BREAKER);
                break;
            case RATE_LIMIT:
                rateLimit = serviceRateLimits.get(serviceKey);
                if (null != rateLimit) {
                    builder.setRateLimit(rateLimit);
                }
                builder.setType(DiscoverResponseType.RATE_LIMIT);
                break;
            default:
                break;
        }
        if (StringUtils.isNotBlank(info)) {
            builder.setInfo(StringValue.newBuilder().setValue(info).build());
        }
        builder.setService(service);

        return builder.build();
    }


    @Override
    public StreamObserver<DiscoverRequest> discover(StreamObserver<ResponseProto.DiscoverResponse> responseObserver) {
        return new StreamObserver<DiscoverRequest>() {

            @Override
            public void onNext(DiscoverRequest req) {
                ServiceProto.Service service = req.getService();
                ServiceKey serviceKey = new ServiceKey(service.getNamespace().getValue(), service.getName().getValue());
                if (!services.containsKey(serviceKey)) {
                    responseObserver.onNext(
                            buildServiceResponse(ServerCodes.NOT_FOUND_RESOURCE,
                                    String.format("service %s not found", serviceKey), req));
                    return;
                }
                responseObserver.onNext(
                        buildServiceResponse(ServerCodes.EXECUTE_SUCCESS,
                                "", req));
            }

            @Override
            public void onError(Throwable t) {
                LOG.error("receive client error", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }


}
