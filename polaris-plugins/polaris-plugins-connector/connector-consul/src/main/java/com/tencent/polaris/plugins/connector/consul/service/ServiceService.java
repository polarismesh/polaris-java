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

package com.tencent.polaris.plugins.connector.consul.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.consul.ConsulContext;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.plugins.connector.common.constant.ConnectorConstant.SERVER_CONNECTOR_TYPE;

/**
 * @author Haotian Zhang
 */
public class ServiceService extends ConsulService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceService.class);

    private final AtomicLong catalogConsulIndex = new AtomicLong(-1L);

    public ServiceService(ConsulClient consulClient, ConsulRawClient consulRawClient, ConsulContext consulContext,
                          String threadName, ObjectMapper mapper) {
        super(consulClient, consulRawClient, consulContext, threadName, mapper);
    }

    @Override
    public void sendRequest(ServiceUpdateTask serviceUpdateTask) {
        try {
            Long index = catalogConsulIndex.get();
            QueryParams queryParams = new QueryParams(consulContext.getWaitTime(), index);
            String aclToken = consulContext.getAclToken();
            Response<Map<String, List<String>>> response;
            int code = ServerCodes.DATA_NO_CHANGE;
            if (StringUtils.isNotBlank(aclToken)) {
                response = consulClient.getCatalogServices(queryParams, aclToken);
            } else {
                response = consulClient.getCatalogServices(queryParams);
            }

            Long consulIndex = response.getConsulIndex();
            if (!index.equals(consulIndex)) {
                code = ServerCodes.EXECUTE_SUCCESS;
            }

            String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();

            ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
            newServiceBuilder.setNamespace(StringValue.of(namespace));
            newServiceBuilder.setName(StringValue.of(serviceUpdateTask.getServiceEventKey().getService()));
            newServiceBuilder.setRevision(StringValue.of(String.valueOf(consulIndex)));
            ServiceProto.Service newService = newServiceBuilder.build();

            List<String> orginalServiceList = new ArrayList<>(response.getValue().keySet());
            List<ServiceProto.Service> serviceList = new ArrayList<>();
            for (String s : orginalServiceList) {
                ServiceProto.Service service = ServiceProto.Service.newBuilder()
                        .setNamespace(StringValue.of(namespace))
                        .setName(StringValue.of(s))
                        .putMetadata(SERVER_CONNECTOR_TYPE, SERVER_CONNECTOR_CONSUL).build();
                serviceList.add(service);
            }

            ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
            newDiscoverResponseBuilder.setService(newService);
            newDiscoverResponseBuilder.addAllServices(serviceList);
            newDiscoverResponseBuilder.setCode(UInt32Value.of(code));

            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_CONSUL);
            boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
            if (consulIndex != null) {
                catalogConsulIndex.set(consulIndex);
            }
            if (!svcDeleted) {
                serviceUpdateTask.addUpdateTaskSet();
            }
        } catch (Throwable throwable) {
            LOG.error("Get services sync failed. Will sleep for {} ms.", consulContext.getConsulErrorSleep(), throwable);
            try {
                Thread.sleep(consulContext.getConsulErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: {}", e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    "Get services sync failed.");
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error, SERVER_CONNECTOR_CONSUL);
            serviceUpdateTask.notifyServerEvent(serverEvent);
            serviceUpdateTask.retry();
        }
    }
}
