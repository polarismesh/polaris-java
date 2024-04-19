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

package com.tencent.polaris.plugins.connector.consul;

import com.ecwid.consul.ConsulException;
import com.ecwid.consul.SingleUrlParameters;
import com.ecwid.consul.UrlParameters;
import com.ecwid.consul.transport.HttpResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.NewService.Check;
import com.ecwid.consul.v1.health.model.HealthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.*;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceInstancesResponse;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import org.slf4j.Logger;

import java.util.*;

import static com.ecwid.consul.json.GsonFactory.getGson;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.*;
import static com.tencent.polaris.plugins.connector.consul.ConsulServerUtils.findHost;
import static com.tencent.polaris.plugins.connector.consul.ConsulServerUtils.getMetadata;

/**
 * An implement of {@link ServerConnector} to connect to Consul Server.It provides methods to manage resources
 * relate to a service:
 * <ol>
 * <li>registerEventHandler/deRegisterEventHandler to subscribe instance/config for a service.
 * <li>registerInstance/deregisterInstance to register/deregister an instance.
 * <li>heartbeat to send heartbeat manually.
 * </ol>
 *
 * @author Haotian Zhang
 */
public class ConsulAPIConnector extends DestroyableServerConnector {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulAPIConnector.class);

    /**
     * If server connector initialized.
     */
    private boolean initialized = false;

    /**
     *
     */
    private boolean ieRegistered = false;

    private String id;
    private boolean isRegisterEnable = true;
    private boolean isDiscoveryEnable = true;

    private ConsulClient consulClient;

    private ConsulRawClient consulRawClient;

    private ConsulContext consulContext;

    private ObjectMapper mapper;

    private List<String> lastServices = new ArrayList<>();

    @Override
    public String getName() {
        return SERVER_CONNECTOR_CONSUL;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isRegisterEnable() {
        return isRegisterEnable;
    }

    @Override
    public boolean isDiscoveryEnable() {
        return isDiscoveryEnable;
    }

    @Override
    public boolean isReportServiceContractEnable() {
        return false;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVER_CONNECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        if (!initialized) {
            List<ServerConnectorConfigImpl> serverConnectorConfigs = ctx.getConfig().getGlobal().getServerConnectors();
            if (CollectionUtils.isNotEmpty(serverConnectorConfigs)) {
                for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
                    if (SERVER_CONNECTOR_CONSUL.equals(serverConnectorConfig.getProtocol())) {
                        mapper = new ObjectMapper();
                        initActually(ctx, serverConnectorConfig);
                    }
                }
            }
        }
    }

    private void initActually(InitContext ctx, ServerConnectorConfig connectorConfig) {
        id = connectorConfig.getId();
        if (ctx.getConfig().getProvider().getRegisterConfigMap().containsKey(id)) {
            isRegisterEnable = ctx.getConfig().getProvider().getRegisterConfigMap().get(id).isEnable();
        }
        if (ctx.getConfig().getConsumer().getDiscoveryConfigMap().containsKey(id)) {
            isDiscoveryEnable = ctx.getConfig().getConsumer().getDiscoveryConfigMap().get(id).isEnable();
        }

        String address = connectorConfig.getAddresses().get(0);
        int lastIndex = address.lastIndexOf(":");
        String agentHost = address.substring(0, lastIndex);
        int agentPort = Integer.parseInt(address.substring(lastIndex + 1));
        LOG.debug("Consul Server : [" + address + "]");
        consulRawClient = new ConsulRawClient(agentHost, agentPort);
        consulClient = new ConsulClient(consulRawClient);

        // Init context.
        consulContext = new ConsulContext();
        consulContext.setConnectorConfig(connectorConfig);
        Map<String, String> metadata = connectorConfig.getMetadata();
        if (metadata.containsKey(SERVICE_NAME_KEY) && StringUtils.isNotBlank(metadata.get(SERVICE_NAME_KEY))) {
            consulContext.setServiceName(metadata.get(SERVICE_NAME_KEY));
        }
        if (metadata.containsKey(INSTANCE_ID_KEY) && StringUtils.isNotBlank(metadata.get(INSTANCE_ID_KEY))) {
            consulContext.setInstanceId(metadata.get(INSTANCE_ID_KEY));
        }
        if (metadata.containsKey(IP_ADDRESS_KEY) && StringUtils.isNotBlank(metadata.get(IP_ADDRESS_KEY))) {
            consulContext.setIpAddress(metadata.get(IP_ADDRESS_KEY));
        }
        if (metadata.containsKey(PREFER_IP_ADDRESS_KEY) && StringUtils.isNotBlank(
                metadata.get(PREFER_IP_ADDRESS_KEY))) {
            consulContext.setPreferIpAddress(Boolean.parseBoolean(metadata.get(PREFER_IP_ADDRESS_KEY)));
        }
        if (StringUtils.isNotBlank(connectorConfig.getToken())) {
            consulContext.setAclToken(connectorConfig.getToken());
        }
        if (metadata.containsKey(TAGS_KEY) && StringUtils.isNotBlank(metadata.get(TAGS_KEY))) {
            try {
                String[] tags = mapper.readValue(metadata.get(TAGS_KEY), String[].class);
                consulContext.setTags(new LinkedList<>(Arrays.asList(tags)));
            } catch (Exception e) {
                LOG.warn("Convert tags from metadata failed.", e);
            }

        }
        if (metadata.containsKey(QUERY_TAG_KEY) && StringUtils.isNotBlank(metadata.get(QUERY_TAG_KEY))) {
            consulContext.setQueryTag(metadata.get(QUERY_TAG_KEY));
        }
        if (metadata.containsKey(QUERY_PASSING_KEY) && StringUtils.isNotBlank(metadata.get(QUERY_PASSING_KEY))) {
            consulContext.setQueryPassing(Boolean.valueOf(metadata.get(QUERY_PASSING_KEY)));
        }
        initialized = true;
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        // do nothing
    }

    @Override
    public void registerServiceHandler(ServiceEventHandler handler) throws PolarisException {
        // do nothing
    }

    @Override
    public void deRegisterServiceHandler(ServiceEventKey eventKey) throws PolarisException {
        // do nothing
    }

    @Override
    public CommonProviderResponse registerInstance(CommonProviderRequest req, Map<String, String> customHeader)
            throws PolarisException {
        if (isRegisterEnable() && !ieRegistered) {
            ServiceKey serviceKey = new ServiceKey(req.getNamespace(), req.getService());
            try {
                LOG.info("Registering service to Consul");
                NewService service = buildRegisterInstanceRequest(req);
                String json = getGson().toJson(service);
                HttpResponse rawResponse;
                if (StringUtils.isNotBlank(consulContext.getAclToken())) {
                    String token = consulContext.getAclToken();
                    UrlParameters tokenParam = token != null ? new SingleUrlParameters("token", token) : null;
                    rawResponse = consulRawClient.makePutRequest("/v1/agent/service/register", json,
                            tokenParam);
                } else {
                    rawResponse = consulRawClient.makePutRequest("/v1/agent/service/register", json);
                }
                if (rawResponse.getStatusCode() != 200) {
                    try {
                        LOG.warn("Register service to consul failed. RawResponse: {}",
                                mapper.writeValueAsString(rawResponse));
                    } catch (JsonProcessingException ignore) {
                    }
                    throw new OperationException(rawResponse);
                }
                CommonProviderResponse resp = new CommonProviderResponse();
                consulContext.setInstanceId(service.getId());
                String checkId = consulContext.getInstanceId();
                if (!checkId.startsWith("service:")) {
                    checkId = "service:" + checkId;
                }
                consulContext.setCheckId(checkId);
                resp.setInstanceID(service.getId());
                resp.setExists(false);
                LOG.info("Registered service to Consul: " + service);
                ieRegistered = true;
                return resp;
            } catch (ConsulException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("fail to register host %s:%d service %s", req.getHost(), req.getPort(),
                                serviceKey), e);
            }
        }
        return null;
    }

    private NewService buildRegisterInstanceRequest(CommonProviderRequest req) {
        NewService service = new NewService();
        String appName = req.getService();
        // Generate ip address
        if (consulContext.isPreferIpAddress()) {
            service.setAddress(consulContext.getIpAddress());
        } else {
            service.setAddress(req.getHost());
        }
        // Generate instance id
        if (StringUtils.isBlank(req.getInstanceID())) {
            if (StringUtils.isBlank(consulContext.getInstanceId())) {
                consulContext.setInstanceId(
                        appName + "-" + service.getAddress().replace(".", "-") + "-" + req.getPort());
            }
            service.setId(consulContext.getInstanceId());
        } else {
            service.setId(req.getInstanceID());
        }

        service.setPort(req.getPort());
        if (StringUtils.isBlank(consulContext.getServiceName())) {
            consulContext.setServiceName(appName);
        }
        service.setName(consulContext.getServiceName());

        // put extended metadata with key of "consul".
        Map<String, String> meta = new HashMap<>(req.getMetadata());
        meta.putAll(req.getExtendedMetadata().get(SERVER_CONNECTOR_CONSUL));
        service.setMeta(meta);
        service.setTags(consulContext.getTags());
        if (null != req.getTtl()) {
            Check check = new Check();
            check.setTtl(req.getTtl() * 1.5 + "s");
            service.setCheck(check);
        }
        Map<String, String> metadata = consulContext.getConnectorConfig().getMetadata();
        if (metadata.containsKey(CHECK_KEY) && StringUtils.isNotBlank(metadata.get(CHECK_KEY))) {
            try {
                NewService.Check check = mapper.readValue(metadata.get(CHECK_KEY), NewService.Check.class);
                service.setCheck(check);
            } catch (Exception e) {
                LOG.warn("Convert check from metadata failed.", e);
            }

        }
        return service;
    }

    @Override
    public void deregisterInstance(CommonProviderRequest req) throws PolarisException {
        if (ieRegistered) {
            ServiceKey serviceKey = new ServiceKey(req.getNamespace(), req.getService());
            try {
                LOG.info("Unregistering service to Consul: " + consulContext.getInstanceId());
                this.consulClient.agentServiceDeregister(consulContext.getInstanceId(), consulContext.getAclToken());
                LOG.info("Unregistered service to Consul: " + consulContext.getInstanceId());
                ieRegistered = false;
            } catch (ConsulException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("fail to deregister host %s:%d service %s", req.getHost(), req.getPort(),
                                serviceKey), e);
            }
        }
    }

    @Override
    public void heartbeat(CommonProviderRequest req) throws PolarisException {
        if (ieRegistered) {
            ServiceKey serviceKey = new ServiceKey(req.getNamespace(), req.getService());
            try {
                this.consulClient.agentCheckPass(consulContext.getCheckId(), null, consulContext.getAclToken());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Heartbeat service to Consul: " + consulContext.getCheckId());
                }
            } catch (ConsulException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("fail to heartbeat id %s, host %s:%d service %s",
                                req.getInstanceID(), req.getHost(), req.getPort(), serviceKey), e);
            }
        }
    }

    @Override
    public ServiceInstancesResponse syncGetServiceInstances(ServiceUpdateTask serviceUpdateTask) {
        List<DefaultInstance> instanceList = new ArrayList<>();
        String serviceId = serviceUpdateTask.getServiceEventKey().getService();
        String tag = consulContext.getQueryTag();
        String token = consulContext.getAclToken();
        boolean onlyPassing = consulContext.getQueryPassing();
        UrlParameters tokenParam = StringUtils.isNotBlank(token) ? new SingleUrlParameters("token", token) : null;
        UrlParameters tagParams = StringUtils.isNotBlank(tag) ? new SingleUrlParameters("tag", tag) : null;
        UrlParameters passingParams = onlyPassing ? new SingleUrlParameters("passing") : null;
        try {
            HttpResponse rawResponse = consulRawClient.makeGetRequest("/v1/health/service/" + serviceId, tagParams,
                    passingParams, QueryParams.DEFAULT, tokenParam);
            List<HealthService> value;
            if (rawResponse.getStatusCode() == 200) {
                value = getGson().fromJson(rawResponse.getContent(),
                        new TypeToken<List<HealthService>>() {
                        }.getType());
            } else {
                String rawResponseStr = "";
                try {
                    rawResponseStr = mapper.writeValueAsString(rawResponse);
                } catch (JsonProcessingException ignore) {
                }
                LOG.error("get service server list occur error. serviceId: {}. RawResponse: {}", serviceId,
                        rawResponseStr);
                throw new OperationException(rawResponse);
            }
            if (CollectionUtils.isNotEmpty(value)) {
                for (HealthService service : value) {
                    DefaultInstance instance = new DefaultInstance();
                    String host = findHost(service);
                    instance.setId(service.getService().getId());
                    instance.setService(service.getService().getService());
                    instance.setHost(host);
                    instance.setPort(service.getService().getPort());
                    instance.setMetadata(getMetadata(service));
                    instance.setHealthy(true);
                    instance.setIsolated(false);
                    instanceList.add(instance);
                }
            }
            return new ServiceInstancesResponse(String.valueOf(rawResponse.getConsulIndex()), instanceList);
        } catch (ConsulException e) {
            LOG.error("Get service instances of {} sync failed.", serviceId, e);
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                    String.format("Get service instances of %s sync failed.",
                            serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
    }

    @Override
    public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
        Services services = new ServicesByProto(new ArrayList<>());
        try {
            List<String> serviceList;
            String aclToken = consulContext.getAclToken();
            if (StringUtils.isNotBlank(aclToken)) {
                serviceList = new ArrayList<>(consulClient.getCatalogServices(QueryParams.DEFAULT, aclToken).getValue()
                        .keySet());
            } else {
                serviceList = new ArrayList<>(consulClient.getCatalogServices(QueryParams.DEFAULT).getValue()
                        .keySet());
            }

            for (String s : serviceList) {
                ServiceInfo serviceInfo = new ServiceInfo();
                serviceInfo.setService(s);
                services.getServices().add(serviceInfo);
            }
        } catch (ConsulException e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                    String.format("Get services of %s instances sync failed.",
                            serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
        return services;
    }

    @Override
    public ReportClientResponse reportClient(ReportClientRequest req) throws PolarisException {
        return null;
    }

    @Override
    public ReportServiceContractResponse reportServiceContract(ReportServiceContractRequest req) throws PolarisException {
        return null;
    }

    @Override
    public void updateServers(ServiceEventKey svcEventKey) {
        // do nothing
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void retryServiceUpdateTask(ServiceUpdateTask updateTask) {
        // do nothing
    }

    @Override
    protected void submitServiceHandler(ServiceUpdateTask updateTask, long delayMs) {
        // do nothing
    }

    @Override
    public void addLongRunningTask(ServiceUpdateTask serviceUpdateTask) {
        // do nothing
    }
}
