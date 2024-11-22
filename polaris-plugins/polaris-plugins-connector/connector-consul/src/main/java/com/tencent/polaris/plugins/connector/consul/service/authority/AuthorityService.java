/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.connector.consul.service.authority;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.plugins.connector.consul.service.ConsulService;
import com.tencent.polaris.plugins.connector.consul.service.authority.entity.AuthRule;
import com.tencent.polaris.plugins.connector.consul.service.authority.entity.AuthRuleGroup;
import com.tencent.polaris.plugins.connector.consul.service.authority.entity.AuthTag;
import com.tencent.polaris.plugins.connector.consul.service.authority.entity.TsfAuthConstant;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConstant;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.security.BlockAllowListProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMatchStringType;

/**
 * @author Haotian Zhang
 */
public class AuthorityService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorityService.class);

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final Map<AuthorityKey, Long> authorityConsulIndexMap = new ConcurrentHashMap<>();

    public AuthorityService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                            String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        String authorityRuleKey = String.format("authority/%s/%s/data", consulContext.getNamespace(), consulContext.getServiceName());
        AuthorityKey authorityKey = new AuthorityKey();
        authorityKey.setNamespace(consulContext.getNamespace());
        authorityKey.setService(consulContext.getServiceName());
        Long currentIndex = getAuthorityConsulIndex(authorityKey);
        QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), currentIndex);
        int code = ServerCodes.DATA_NO_CHANGE;
        try {
            LOG.debug("Begin get authority rules of {} sync", authorityRuleKey);
            Response<GetValue> response = consulClient.getKVValue(authorityRuleKey, consulContext.getAclToken(), queryParams);
            if (response != null) {
                if (LOG.isDebugEnabled()) {
                    String responseStr = "Response{" +
                            "value='" + response.getValue() + '\'' +
                            ", consulIndex=" + response.getConsulIndex() + '\'' +
                            ", consulKnownLeader=" + response.isConsulKnownLeader() + '\'' +
                            ", consulLastContact=" + response.getConsulLastContact() +
                            '}';
                    LOG.debug("tsf authority rule, consul kv namespace, response: {}", responseStr);
                }

                Long newIndex = response.getConsulIndex();
                // create service.
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(StringValue.of(consulContext.getNamespace()));
                newServiceBuilder.setName(StringValue.of(consulContext.getServiceName()));
                newServiceBuilder.setRevision(StringValue.of(String.valueOf(newIndex)));
                // create discover response.
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(newServiceBuilder);
                // 重写index
                List<BlockAllowListProto.BlockAllowListRule> ruleList = new ArrayList<>();
                if (Objects.nonNull(newIndex)) {
                    if (!Objects.equals(currentIndex, newIndex)) {
                        code = ServerCodes.EXECUTE_SUCCESS;
                        GetValue getValue = response.getValue();
                        if (Objects.nonNull(getValue)) {
                            String decodedValue = getValue.getDecodedValue();
                            LOG.info("[TSF Auth] New consul config: {}", decodedValue);
                            if (!StringUtils.isEmpty(decodedValue)) {
                                ruleList = parseResponse(decodedValue, consulContext.getNamespace(), consulContext.getServiceName(), newIndex);
                            }
                        } else {
                            LOG.info("empty authority rule: {}", response);
                        }
                    } else {
                        LOG.debug("[TSF Auth] Consul data is not changed");
                    }
                } else {
                    LOG.warn("[TSF Auth] Consul data is abnormal. {}", response);
                }
                if (CollectionUtils.isNotEmpty(ruleList)) {
                    newDiscoverResponseBuilder.addAllBlockAllowListRule(ruleList);
                }
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (newIndex != null) {
                    setAuthorityConsulIndex(authorityKey, currentIndex, newIndex);
                }
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        } catch (Throwable e) {
            LOG.error("[TSF Auth] tsf authority rule load error. Will sleep for {} ms. Key path:{}",
                    consulContext.getConsulErrorSleep(), authorityRuleKey, e);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get authority sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private List<BlockAllowListProto.BlockAllowListRule> parseResponse(String decodedValue, String namespace, String service, Long index) {
        List<BlockAllowListProto.BlockAllowListRule> ruleList = Lists.newArrayList();

        // yaml -> List<AuthRuleGroup>
        Yaml yaml = new Yaml();
        List<AuthRuleGroup> authRuleGroupList;
        try {
            String authJsonString = mapper.writeValueAsString(yaml.load(decodedValue));
            authRuleGroupList = mapper.readValue(authJsonString, new TypeReference<List<AuthRuleGroup>>() {
            });
        } catch (Exception ex) {
            LOG.error("tsf authority rule load error.", ex);
            throw new PolarisException(ErrorCode.INVALID_RESPONSE, "tsf authority rule load error.", ex);
        }

        // List<AuthRuleGroup> -> List<BlockAllowListProto.BlockAllowListRule>
        if (CollectionUtils.isNotEmpty(authRuleGroupList)) {
            for (AuthRuleGroup authRuleGroup : authRuleGroupList) {
                for (AuthRule authRule : authRuleGroup.getRules()) {
                    BlockAllowListProto.BlockAllowListRule.Builder ruleBuilder = BlockAllowListProto.BlockAllowListRule.newBuilder();
                    ruleBuilder.setId(authRule.getRuleId());
                    ruleBuilder.setNamespace(namespace);
                    ruleBuilder.setService(service);
                    ruleBuilder.setEnable(true);
                    ruleBuilder.setName(authRule.getRuleName());
                    BlockAllowListProto.BlockAllowConfig.Builder blockAllowConfigBuilder = BlockAllowListProto.BlockAllowConfig.newBuilder();
                    blockAllowConfigBuilder.setBlockAllowPolicy(parseBlockAllowPolicy(authRuleGroup.getType()));
                    if (CollectionUtils.isNotEmpty(authRule.getTags())) {
                        List<BlockAllowListProto.BlockAllowConfig.MatchArgument> list = new ArrayList<>();
                        for (AuthTag authTag : authRule.getTags()) {
                            // build MatchArgument
                            BlockAllowListProto.BlockAllowConfig.MatchArgument.Builder matchArgumentBuilder = BlockAllowListProto.BlockAllowConfig.MatchArgument.newBuilder();
                            if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_SERVICE_NAME)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CALLER_SERVICE);
                                matchArgumentBuilder.setKey("*");
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_NAMESPACE_SERVICE_NAME)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CALLER_SERVICE);
                                matchArgumentBuilder.setKey("*");
                                String[] tagValues = authTag.getTagValue().split(",");
                                StringBuilder serviceNameStringBuilder = new StringBuilder();
                                for (String tagValue : tagValues) {
                                    if (StringUtils.isNotEmpty(tagValue)) {
                                        String[] split = tagValue.split("/");
                                        if (split.length == 2) {
                                            serviceNameStringBuilder.append(split[1]).append(",");
                                        } else {
                                            serviceNameStringBuilder.append(tagValue).append(",");
                                        }
                                    }
                                }
                                String serviceNameString = serviceNameStringBuilder.toString();
                                if (serviceNameString.endsWith(",")) {
                                    serviceNameString = serviceNameString.substring(0, serviceNameString.length() - 1);
                                }
                                authTag.setTagValue(serviceNameString);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_APPLICATION_ID)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CALLER_METADATA);
                                matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_APPLICATION_ID);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_APPLICATION_VERSION)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CALLER_METADATA);
                                matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_PROG_VERSION);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_GROUP_ID)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CALLER_METADATA);
                                matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_GROUP_ID);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_CONNECTION_IP)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CALLER_IP);
                                matchArgumentBuilder.setKey(MessageMetadataContainer.LABEL_KEY_CALLER_IP);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.DESTINATION_APPLICATION_VERSION)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CUSTOM);
                                matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_PROG_VERSION);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.DESTINATION_GROUP_ID)) {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CUSTOM);
                                matchArgumentBuilder.setKey(TsfMetadataConstants.TSF_GROUP_ID);
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.DESTINATION_INTERFACE)) {
                                ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                                matchStringBuilder.setType(parseMatchStringType(authTag.getTagOperator()));
                                matchStringBuilder.setValue(StringValue.of(authTag.getTagValue()));
                                ModelProto.API.Builder apiBuilder = ModelProto.API.newBuilder();
                                apiBuilder.setPath(matchStringBuilder);
                                apiBuilder.setProtocol("*");
                                apiBuilder.setMethod("*");
                                blockAllowConfigBuilder.setApi(apiBuilder.build());
                                continue;
                            } else if (StringUtils.equals(authTag.getTagField(), TagConstant.SYSTEM_FIELD.REQUEST_HTTP_METHOD)) {
                                ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                                matchStringBuilder.setType(ModelProto.MatchString.MatchStringType.EXACT);
                                matchStringBuilder.setValue(StringValue.of("*"));
                                ModelProto.API.Builder apiBuilder = ModelProto.API.newBuilder();
                                apiBuilder.setPath(matchStringBuilder);
                                apiBuilder.setProtocol("*");
                                String method = authTag.getTagValue();
                                if (authTag.getTagOperator().equals(TagConstant.OPERATOR.NOT_EQUAL)) {
                                    method = "!" + method;
                                }
                                apiBuilder.setMethod(method);
                                blockAllowConfigBuilder.setApi(apiBuilder.build());
                                continue;
                            } else {
                                matchArgumentBuilder.setType(BlockAllowListProto.BlockAllowConfig.MatchArgument.Type.CUSTOM);
                                matchArgumentBuilder.setKey(authTag.getTagField());
                            }
                            ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                            matchStringBuilder.setType(parseMatchStringType(authTag.getTagOperator()));
                            matchStringBuilder.setValue(StringValue.of(authTag.getTagValue()));
                            matchArgumentBuilder.setValue(matchStringBuilder);
                            list.add(matchArgumentBuilder.build());
                        }
                        blockAllowConfigBuilder.addAllArguments(list);
                    }
                    ruleBuilder.addBlockAllowConfig(blockAllowConfigBuilder);
                    ruleList.add(ruleBuilder.build());
                }
            }
        }
        return ruleList;
    }

    private BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy parseBlockAllowPolicy(String type) {
        switch (type) {
            case TsfAuthConstant.TYPE.BLACK_LIST:
                return BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.BLOCK_LIST;
            case TsfAuthConstant.TYPE.WHITE_LIST:
            default:
                return BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST;
        }
    }

    private Long getAuthorityConsulIndex(AuthorityKey authorityKey) {
        Long index = authorityConsulIndexMap.get(authorityKey);
        if (index != null) {
            return index;
        }
        setAuthorityConsulIndex(authorityKey, null, -1L);
        return -1L;
    }

    private void setAuthorityConsulIndex(AuthorityKey authorityKey, Long lastIndex, Long newIndex) {
        LOG.debug("AuthorityKey: {}; lastIndex: {}; newIndex: {}", authorityKey, lastIndex, newIndex);
        authorityConsulIndexMap.put(authorityKey, newIndex);
    }

    static class AuthorityKey {
        private String namespace = "";
        private String service = "";

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AuthorityKey that = (AuthorityKey) o;
            return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getService(), that.getService());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNamespace(), getService());
        }
    }
}
