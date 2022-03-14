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

package com.tencent.polaris.factory.api;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class APIFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIFacade.class);

    private static ConsumerAPI consumerAPI;

    private static ProviderAPI providerAPI;

    private static SDKContext sdkContext;

    private static LimitAPI limitAPI;

    private static final Object lock = new Object();

    private static final AtomicBoolean inited = new AtomicBoolean(false);

    private static final Map<String, ServiceListener> linstenerMap = new ConcurrentHashMap<>();

    public static void initByConfiguration(Object configuration) {
        if (inited.get()) {
            return;
        }
        synchronized (lock) {
            if (inited.get()) {
                return;
            }
            sdkContext = SDKContext.initContextByConfig((Configuration) configuration);
            inited.set(true);
            consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
            providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);
            limitAPI = LimitAPIFactory.createLimitAPIByContext(sdkContext);
        }
    }

    public static void destroy() {
        synchronized (lock) {
            if (!inited.get()) {
                return;
            }
            sdkContext.close();
            inited.set(false);
        }
    }

    public static boolean register(String namespace, String service, String host, int port, String protocol,
            String version, int weight, Map<String, String> metadata, int ttl, String token) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, register fail");
            return false;
        }
        InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
        instanceRegisterRequest.setNamespace(namespace);
        instanceRegisterRequest.setService(service);
        instanceRegisterRequest.setHost(host);
        instanceRegisterRequest.setPort(port);
        instanceRegisterRequest.setProtocol(protocol);
        instanceRegisterRequest.setVersion(version);
        instanceRegisterRequest.setTtl(ttl);
        instanceRegisterRequest.setWeight(weight);
        instanceRegisterRequest.setMetadata(metadata);
        instanceRegisterRequest.setToken(token);
        InstanceRegisterResponse instanceRegisterResponse = providerAPI.register(instanceRegisterRequest);
        LOGGER.info("response after register is {}", instanceRegisterResponse);
        return true;
    }

    public static boolean heartbeat(String namespace, String service, String host, int port, String token) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, heartbeat fail");
            return false;
        }
        InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
        heartbeatRequest.setNamespace(namespace);
        heartbeatRequest.setService(service);
        heartbeatRequest.setHost(host);
        heartbeatRequest.setPort(port);
        heartbeatRequest.setToken(token);
        ((ProviderAPI) providerAPI).heartbeat(heartbeatRequest);
        LOGGER.info("heartbeat instance, address is {}:{}", host, port);
        return true;
    }

    public static boolean deregister(String namespace, String service, String host, int port, String token) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, deregister fail");
            return false;
        }
        InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();
        deregisterRequest.setNamespace(namespace);
        deregisterRequest.setService(service);
        deregisterRequest.setHost(host);
        deregisterRequest.setPort(port);
        deregisterRequest.setToken(token);
        providerAPI.deRegister(deregisterRequest);
        LOGGER.info("deregister instance, address is {}:{}", host, port);
        return false;
    }

    public static boolean getQuota(String namespace, String service, String method, Map<String, String> labels,
            int count) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, getQuota fail");
            return false;
        }
        QuotaRequest quotaRequest = new QuotaRequest();
        quotaRequest.setNamespace(namespace);
        quotaRequest.setService(service);
        quotaRequest.setMethod(method);
        quotaRequest.setLabels(labels);
        quotaRequest.setCount(count);
        QuotaResponse quota = limitAPI.getQuota(quotaRequest);
        return quota.getCode() == QuotaResultCode.QuotaResultOk;
    }

    public static boolean updateServiceCallResult(String namespace, String service, String method, String host,
            int port, long delay, boolean success, int code) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, updateServiceCallResult fail");
            return false;
        }
        ServiceCallResult serviceCallResult = new ServiceCallResult();
        serviceCallResult.setNamespace(namespace);
        serviceCallResult.setService(service);
        serviceCallResult.setMethod(method);
        serviceCallResult.setHost(host);
        serviceCallResult.setPort(port);
        serviceCallResult.setDelay(delay);
        serviceCallResult.setRetStatus(success ? RetStatus.RetSuccess : RetStatus.RetFail);
        serviceCallResult.setRetCode(code);
        consumerAPI.updateServiceCallResult(serviceCallResult);
        LOGGER.debug("success to call updateServiceCallResult, status:{}", serviceCallResult.getRetStatus());
        return true;
    }

    public static List<?> getInstances(String namespace, String service, Map<String, String> srcLabels,
            Map<String, String> dstLabels) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, updateServiceCallResult fail");
            return null;
        }
        GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
        getInstancesRequest.setNamespace(namespace);
        getInstancesRequest.setService(service);
        if (MapUtils.isNotEmpty(srcLabels)) {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setMetadata(srcLabels);
            getInstancesRequest.setServiceInfo(serviceInfo);
        }
        if (MapUtils.isNotEmpty(dstLabels)) {
            getInstancesRequest.setMetadata(dstLabels);
        }
        InstancesResponse instancesResp = consumerAPI.getInstances(getInstancesRequest);
        ServiceInstances serviceInstances = instancesResp.toServiceInstances();
        return serviceInstances.getInstances();
    }

    public static List<?> getAllInstances(String namespace, String service) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, getAllInstances fail");
            return null;
        }
        GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
        getAllInstancesRequest.setNamespace(namespace);
        getAllInstancesRequest.setService(service);
        InstancesResponse instancesResp = consumerAPI.getAllInstance(getAllInstancesRequest);
        ServiceInstances serviceInstances = instancesResp.toServiceInstances();
        return serviceInstances.getInstances();
    }

    public static List<?> watchService(String namespace, String service, Object listener, Method method) {
        ServiceListener serviceListener = new APIFacade.ServiceWatcher(listener, method);
        WatchServiceRequest request = WatchServiceRequest.builder().
                namespace(namespace).
                service(service).
                listeners(Collections.singletonList(serviceListener))
                .build();
        WatchServiceResponse response = consumerAPI.watchService(request);
        String serviceKeyStr = namespace + ":" + service;
        linstenerMap.put(serviceKeyStr, serviceListener);
        return response.getResponse().toServiceInstances().getInstances();
    }

    public static boolean unWatchService(String namespace, String service) {
        String serviceKeyStr = namespace + ":" + service;
        ServiceListener listener = linstenerMap.get(serviceKeyStr);
        if (null == listener) {
            return true;
        }
        WatchServiceRequest request = WatchServiceRequest.builder().
                namespace(namespace).
                service(service).
                listeners(Collections.singletonList(listener))
                .build();
        return consumerAPI.unWatchService(request);
    }

    private static class ServiceWatcher implements ServiceListener {
        private Object listener;
        private Method method;

        ServiceWatcher(Object listener, Method method) {
            this.listener = listener;
            this.method = method;
        }

        public void onEvent(ServiceChangeEvent event) {
            try {
                this.method.invoke(this.listener, event);
            } catch (Exception var3) {
                APIFacade.LOGGER.error("fail to invoke method {}, exception is: {}", this.method.getName(), var3.getMessage());
            }
        }
    }

    public static class ConfigurationModifier {

        public static void setAddresses(Object configuration, List<String> addresses) {
            ((ConfigurationImpl) configuration).setDefault();
            ((ConfigurationImpl) configuration).getGlobal().getServerConnector().setAddresses(addresses);
        }
    }

    public static class InstanceParser {

        public static String getHost(Object instance) {
            return ((Instance) instance).getHost();
        }

        public static int getPort(Object instance) {
            return ((Instance) instance).getPort();
        }

        public static String getProtocol(Object instance) {
            return ((Instance) instance).getProtocol();
        }

        public static int getWeight(Object instance) {
            return ((Instance) instance).getWeight();
        }

        public static Map<String, String> getMetadata(Object instance) {
            return ((Instance) instance).getMetadata();
        }
    }
}
