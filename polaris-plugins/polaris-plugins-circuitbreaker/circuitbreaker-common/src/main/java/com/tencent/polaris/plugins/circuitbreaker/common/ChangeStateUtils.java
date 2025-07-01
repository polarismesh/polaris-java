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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreakResult;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreakResult.ResultKey;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 状态转换相关逻辑
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public class ChangeStateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeStateUtils.class);

    /**
     * 构建熔断结果
     *
     * @param stateMachine 熔断状态机
     * @param instances 实例集
     * @param parameter 熔断参数
     * @param <T> 配置类型
     * @return 结果
     */
    public static <T extends Verifier> CircuitBreakResult buildCircuitBreakResult(
            StateMachine<T> stateMachine, Collection<Instance> instances, StateMachine.Parameter parameter) {
        long curTimeMs = System.currentTimeMillis();
        CircuitBreakResult result = new CircuitBreakResult(curTimeMs,
                parameter.getHalfOpenMaxReqCount());
        String cbName = parameter.getCircuitBreakerName();
        for (Instance instance : instances) {
            Set<StatusDimension> statusDimensions = stateMachine.getStatusDimensions(instance, parameter);
            if (CollectionUtils.isEmpty(statusDimensions)) {
                continue;
            }
            String instanceId = instance.getId();
            for (StatusDimension statusDimension : statusDimensions) {
                if (stateMachine.closeToOpen(instance, statusDimension, parameter)) {
                    result.getInstancesToOpen().put(new ResultKey(instanceId, statusDimension), instance);
                    LOG.info("circuitBreaker: instance {} and dimension {} changed from close to open by {}",
                            instanceId, statusDimension, cbName);
                    continue;
                }
                if (stateMachine.openToHalfOpen(instance, statusDimension, parameter)) {
                    result.getInstancesToHalfOpen().put(new ResultKey(instanceId, statusDimension), instance);
                    LOG.info("circuitBreaker: instance {} and dimension {} changed from open to halfOpen by {}",
                            instanceId, statusDimension, cbName);
                    continue;
                }
                if (stateMachine.halfOpenToOpen(instance, statusDimension, parameter)) {
                    result.getInstancesToOpen().put(new ResultKey(instanceId, statusDimension), instance);
                    LOG.info("circuitBreaker: instance {} and dimension {} changed from halfOpen to open by {}",
                            instanceId, statusDimension, cbName);
                    continue;
                }
                if (stateMachine.halfOpenToClose(instance, statusDimension, parameter)) {
                    result.getInstancesToClose().put(new ResultKey(instanceId, statusDimension), instance);
                    LOG.info("circuitBreaker: instance {} and dimension {} changed from halfOpen to close by {}",
                            instanceId, statusDimension, cbName);
                }
            }
        }
        return result;
    }

    /**
     * 获取实例ID
     *
     * @param instanceGauge 实例统计数据
     * @param localRegistry 本地缓存插件
     * @return ID
     */
    public static InstanceByProto getInstance(InstanceGauge instanceGauge, LocalRegistry localRegistry) {
        ServiceEventKey serviceEventKey = new ServiceEventKey(
                new ServiceKey(instanceGauge.getNamespace(), instanceGauge.getService()), EventType.INSTANCE);
        ResourceFilter resourceFilter = new ResourceFilter(serviceEventKey, true, true);
        ServiceInstances instances = localRegistry.getInstances(resourceFilter);
        if (!instances.isInitialized()) {
            return null;
        }
        ServiceInstancesByProto serviceInstancesByProto = (ServiceInstancesByProto) instances;
        Instance instance = instanceGauge.getInstance();
        if (instance instanceof InstanceByProto) {
            return (InstanceByProto) instance;
        }
        InstanceByProto instanceByProto;
        String instanceId = instanceGauge.getInstanceId();
        if (StringUtils.isNotBlank(instanceId)) {
            instanceByProto = serviceInstancesByProto.getInstance(instanceId);
        } else {
            Node node = new Node(instanceGauge.getHost(), instanceGauge.getPort());
            instanceByProto = serviceInstancesByProto.getInstance(node);
        }
        if (null != instanceByProto) {
            instanceGauge.setInstance(instanceByProto);
        }
        return instanceByProto;
    }

    /**
     * 构建状态维度
     *
     * @param instanceGauge 统计数据
     * @param level 熔断级别
     * @return 维度
     */
    public static StatusDimension buildStatusDimension(InstanceGauge instanceGauge, StatusDimension.Level level) {
        switch (level) {
            case CALLER_METHOD:
                return new StatusDimension(instanceGauge.getMethod(), instanceGauge.getCallerService());
            case ALL_CALLER:
                return new StatusDimension(instanceGauge.getMethod(), null);
            case ALL_METHOD:
                return new StatusDimension("", instanceGauge.getCallerService());
            default:
                return StatusDimension.EMPTY_DIMENSION;
        }
    }
}
