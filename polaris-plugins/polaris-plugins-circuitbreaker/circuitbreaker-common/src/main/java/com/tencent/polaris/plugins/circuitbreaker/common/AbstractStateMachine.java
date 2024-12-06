/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceLocalValue;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 默认状态机实现，包括熔断后到半开到打开的逻辑，子类需要去实现具体到达熔断状态的逻辑
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public abstract class AbstractStateMachine<T extends Verifier> implements StateMachine<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractStateMachine.class);

    protected final ConfigGroup<T> configGroup;

    protected final int pluginId;

    protected final ConfigSetLocator<T> configSetLocator;

    public AbstractStateMachine(ConfigGroup<T> configGroup, int pluginId, ConfigSetLocator<T> configSetLocator) {
        this.configGroup = configGroup;
        this.pluginId = pluginId;
        this.configSetLocator = configSetLocator;
    }

    @Override
    public Set<StatusDimension> getStatusDimensions(Instance instance, Parameter parameter) {
        HalfOpenCounter halfOpenCounter = getHalfOpenCounter(parameter.getPluginId(), instance);
        if (null == halfOpenCounter) {
            return Collections.emptySet();
        }
        return halfOpenCounter.getStatusDimensions();
    }

    @Override
    public boolean openToHalfOpen(Instance instance, StatusDimension statusDimension, Parameter parameter) {
        String cbName = parameter.getCircuitBreakerName();
        CircuitBreakerStatus cbStatus = instance.getCircuitBreakerStatus(statusDimension);
        if (null == cbStatus || !cbStatus.getCircuitBreaker().equals(cbName)
                || cbStatus.getStatus() != CircuitBreakerStatus.Status.OPEN) {
            return false;
        }
        boolean detectSuccess = false;
        if (instance instanceof InstanceByProto) {
            DetectResult detectResult = ((InstanceByProto) instance).getDetectResult();
            detectSuccess = null != detectResult && detectResult.getRetStatus() == RetStatus.RetSuccess;
        }
        //清空halfOpen的计数器
        HalfOpenCounter halfOpenCounter = getHalfOpenCounter(parameter.getPluginId(), instance);
        if (null == halfOpenCounter) {
            return false;
        }
        long startTimeMs = cbStatus.getStartTimeMs();
        HalfOpenConfig halfOpenConfig = getHalfOpenConfigOnHalfOpen(instance, statusDimension);
        boolean matched;
        if (detectSuccess && halfOpenConfig.getWhenToDetect() != When.never) {
            matched = true;
        } else {
            matched = parameter.getCurrentTimeMs() - startTimeMs >= halfOpenConfig.getSleepWindowMs();
        }
        if (matched) {
            //时间窗已经过去, 则恢复半开
            halfOpenCounter.resetHalfOpen(statusDimension);
        }
        return matched;
    }

    @Override
    public boolean halfOpenToOpen(Instance instance, StatusDimension statusDimension, Parameter parameter) {
        HalfOpenCounter halfOpenCounter = getHalfOpenCounterByParameter(
                instance, statusDimension, parameter);
        if (halfOpenCounter == null) {
            return false;
        }
        //获取最近是否存在失败，存在足够错误则熔断器重新开启
        long failCountAfterHalfOpen = halfOpenCounter.getHalfOpenFailCount(statusDimension);
        return failCountAfterHalfOpen >= getHalfOpenConfigOnHalfOpen(instance, statusDimension).getHalfOpenFailCount();
    }

    private HalfOpenCounter getHalfOpenCounterByParameter(Instance instance, StatusDimension statusDimension,
            Parameter parameter) {
        String cbName = parameter.getCircuitBreakerName();
        CircuitBreakerStatus cbStatus = instance.getCircuitBreakerStatus(statusDimension);
        if (null == cbStatus || !cbStatus.getCircuitBreaker().equals(cbName)
                || cbStatus.getStatus() != CircuitBreakerStatus.Status.HALF_OPEN) {
            return null;
        }
        return getHalfOpenCounter(parameter.getPluginId(), instance);
    }

    @Override
    public boolean halfOpenToClose(Instance instance, StatusDimension statusDimension, Parameter parameter) {
        HalfOpenCounter halfOpenCounter = getHalfOpenCounterByParameter(
                instance, statusDimension, parameter);
        if (halfOpenCounter == null) {
            return false;
        }
        //获取最近是否存在失败，存足够成功数则熔断器重新关闭
        HalfOpenConfig halfOpenConfig = getHalfOpenConfigOnHalfOpen(instance, statusDimension);
        long sucCountAfterHalfOpen = halfOpenCounter.getHalfOpenSuccessCount(statusDimension);
        boolean matched = sucCountAfterHalfOpen >= halfOpenConfig.getHalfOpenSuccessCount();
        if (matched) {
            halfOpenCounter.resetCounter(statusDimension);
        }
        return matched;
    }

    protected HalfOpenCounter getHalfOpenCounter(int pluginId, Instance instance) {
        InstanceByProto instanceByProto = (InstanceByProto) instance;
        InstanceLocalValue instanceLocalValue = instanceByProto.getInstanceLocalValue();
        if (null == instanceLocalValue) {
            return null;
        }
        Object pluginValue = instanceLocalValue.getPluginValue(pluginId, null);
        if (null == pluginValue) {
            return null;
        }
        return (HalfOpenCounter) pluginValue;
    }

    private HalfOpenConfig getHalfOpenConfigOnHalfOpen(Instance instance, StatusDimension statusDimension) {
        RuleIdentifier ruleIdentifier = new RuleIdentifier(instance.getNamespace(), instance.getService(),
                statusDimension.getCallerService(), statusDimension.getMethod());
        ConfigSet<T> configSet = configSetLocator.getConfigSet(ruleIdentifier);
        return configSet.getHalfOpenConfig();
    }

    protected HalfOpenCounter getHalfOpenCounterOnClose(Instance instance, StatusDimension statusDimension) {
        CircuitBreakerStatus cbStatus = instance.getCircuitBreakerStatus(statusDimension);
        if (null != cbStatus && cbStatus.getStatus() != CircuitBreakerStatus.Status.CLOSE) {
            return null;
        }
        //统计错误率
        return getHalfOpenCounter(pluginId, instance);
    }

    protected <T extends Verifier> ConfigSet<T> getConfigSetByLocator(Instance instance,
            StatusDimension statusDimension, ConfigSetLocator<T> configSetLocator) {
        RuleIdentifier ruleIdentifier = new RuleIdentifier(instance.getNamespace(), instance.getService(),
                statusDimension.getCallerService(), statusDimension.getMethod());
        return configSetLocator.getConfigSet(ruleIdentifier);
    }

}
