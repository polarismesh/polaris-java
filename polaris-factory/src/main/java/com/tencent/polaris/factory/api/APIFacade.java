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
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIFacade.class);

    private static ConsumerAPI consumerAPI;

    private static ProviderAPI providerAPI;

    private static SDKContext sdkContext;

    private static LimitAPI limitAPI;

    private static final Object lock = new Object();

    private static final AtomicBoolean inited = new AtomicBoolean(false);

    public static void initByConfigText(String configStr) {
        if (inited.get()) {
            return;
        }
        synchronized (lock) {
            if (inited.get()) {
                return;
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(configStr.getBytes());
            Configuration configuration = ConfigAPIFactory.loadConfig(bis);
            SDKContext sdkContext = SDKContext.initContextByConfig(configuration);
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
            String version, Map<String, String> metadata, int ttl, String token) {
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

    public static boolean updateServiceCallResult(String namespace, String service, String host, int port, long delay,
            boolean success, int code) {
        if (!inited.get()) {
            LOGGER.info("polaris not inited, updateServiceCallResult fail");
            return false;
        }
        ServiceCallResult serviceCallResult = new ServiceCallResult();
        serviceCallResult.setNamespace(namespace);
        serviceCallResult.setService(service);
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
        }
        if (MapUtils.isNotEmpty(dstLabels)) {
            getInstancesRequest.setMetadata(dstLabels);
        }
        InstancesResponse instancesResp = consumerAPI.getInstances(getInstancesRequest);
        ServiceInstances serviceInstances = instancesResp.toServiceInstances();
        return serviceInstances.getInstances();
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

        public static Map<String, String> getMetadata(Object instance) {
            return ((Instance) instance).getMetadata();
        }
    }
}
