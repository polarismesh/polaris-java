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

package com.tencent.polaris.discovery.client.util;


import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.CommonValidator;
import java.util.Set;

/**
 * 校验API入参是否正确.
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class Validator {

    private static final int MAX_PORT = 65536;

    /**
     * 校验获取资源请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败
     */
    public static void validateGetResourcesRequest(GetResourcesRequest request) throws PolarisException {
        Set<ServiceEventKey> svcEventKeys = request.getSvcEventKeys();
        if (CollectionUtils.isEmpty(svcEventKeys)) {
            return;
        }
        for (ServiceEventKey svcEventKey : svcEventKeys) {
            ServiceKey serviceKey = svcEventKey.getServiceKey();
            ServiceEventKey.EventType eventType = svcEventKey.getEventType();
            if (null == serviceKey || null == eventType) {
                throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "missing service key or event type");
            }
            CommonValidator.validateNamespaceService(serviceKey.getNamespace(), serviceKey.getService());
        }
    }

    /**
     * 校验获取单个服务实例的请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateGetOneInstanceRequest(GetOneInstanceRequest request) throws PolarisException {
        checkCommon(request);
    }

    /**
     * 校验获取批量服务实例的请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateGetInstancesRequest(GetInstancesRequest request) throws PolarisException {
        checkCommon(request);
    }

    /**
     * 校验获取批量服务实例的请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateGetAllInstancesRequest(GetAllInstancesRequest request) throws PolarisException {
        checkCommon(request);
    }

    /**
     * 校验获取服务规则的请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败
     */
    public static void validateGetServiceRuleRequest(GetServiceRuleRequest request) throws PolarisException {
        checkCommon(request);
        if (request.getRuleType() == ServiceEventKey.EventType.INSTANCE) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "event type can not be instance");
        }
    }

    /**
     * 校验服务监听的请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败
     */
    public static void validateWatchServiceRequest(WatchServiceRequest request) throws PolarisException {
        CommonValidator.validateNamespaceService(request.getNamespace(), request.getService());
    }

    /**
     * 校验用户上报的调用结果
     *
     * @param serviceCallResult 调用结果
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateServiceCallResult(ServiceCallResult serviceCallResult) throws PolarisException {
        CommonValidator.validateNamespaceService(serviceCallResult.getNamespace(), serviceCallResult.getService());
        validateHostPort(serviceCallResult.getHost(), serviceCallResult.getPort());
        if (null == serviceCallResult.getRetStatus()) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "retStatus can not be blank");
        }
        if (null != serviceCallResult.getDelay() && serviceCallResult.getDelay() < 0) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "delay can not be less than 0");
        }
    }

    /**
     * 校验端口信息
     *
     * @param port 端口类型
     * @throws PolarisException 校验失败异常
     */
    private static void validateHostPort(String host, Integer port) throws PolarisException {
        if (StringUtils.isBlank(host)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "host can not be blank");
        }
        if (port == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "port can not be null");
        }
        if (port <= 0 || port >= MAX_PORT) {
            throw new PolarisException(
                    ErrorCode.API_INVALID_ARGUMENT, "port value should be in range (0, 65536).");
        }
    }

    /**
     * 校验实例注册请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateInstanceRegisterRequest(InstanceRegisterRequest request) throws PolarisException {
        checkCommon(request);
        validateHostPort(request.getHost(), request.getPort());
    }

    /**
     * 校验实例反注册请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateInstanceDeregisterRequest(InstanceDeregisterRequest request) throws PolarisException {
        if (StringUtils.isNotBlank(request.getInstanceID())) {
            return;
        }
        CommonValidator.validateNamespaceService(request.getNamespace(), request.getService());
        validateHostPort(request.getHost(), request.getPort());
    }

    /**
     * 校验实例心跳请求
     *
     * @param request 请求对象
     * @throws PolarisException 校验失败会抛出异常
     */
    public static void validateHeartbeatRequest(InstanceHeartbeatRequest request) throws PolarisException {
        if (StringUtils.isNotBlank(request.getInstanceID())) {
            return;
        }
        validateHostPort(request.getHost(), request.getPort());
        CommonValidator.validateNamespaceService(request.getNamespace(), request.getService());
    }

    private static void checkCommon(BaseEntity entity) throws PolarisException {
        CommonValidator.validateNamespaceService(entity.getNamespace(), entity.getService());
    }
}
