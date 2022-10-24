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

package com.tencent.polaris.plugins.circuitbreaker.errrate;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreakResult;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.InstanceLocalValue;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.pojo.Subset;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.client.pb.CircuitBreakerProto.CbPolicy;
import com.tencent.polaris.client.pb.CircuitBreakerProto.CbPolicy.ErrRateConfig;
import com.tencent.polaris.client.pb.CircuitBreakerProto.DestinationSet;
import com.tencent.polaris.client.pb.CircuitBreakerProto.RecoverConfig;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.plugins.circuitbreaker.common.ChangeStateUtils;
import com.tencent.polaris.plugins.circuitbreaker.common.CircuitBreakUtils;
import com.tencent.polaris.plugins.circuitbreaker.common.CircuitBreakUtils.RuleDestinationResult;
import com.tencent.polaris.plugins.circuitbreaker.common.ConfigGroup;
import com.tencent.polaris.plugins.circuitbreaker.common.ConfigSet;
import com.tencent.polaris.plugins.circuitbreaker.common.ConfigSetLocator;
import com.tencent.polaris.plugins.circuitbreaker.common.HalfOpenConfig;
import com.tencent.polaris.plugins.circuitbreaker.common.HalfOpenCounter;
import com.tencent.polaris.plugins.circuitbreaker.common.RuleIdentifier;
import com.tencent.polaris.plugins.circuitbreaker.common.StateMachine;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.SliceWindow;
import java.util.Collection;
import java.util.function.Function;

/**
 * 基于错误率的熔断器
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public class ErrRateCircuitBreaker extends Destroyable implements CircuitBreaker, PluginConfigProvider,
        ConfigSetLocator<Config> {

    private int id;

    private ConfigGroup<Config> configGroup;

    private long metricWindowMs;

    private Extensions extensions;

    private FlowControlParam flowControlParam;

    private StateMachine<Config> stateMachine;

    private LocalRegistry localRegistry;

    private Function<Integer, Object> create;

    private final String metricWindowName = String.format("%s_%s", getName(), "metric");

    private CircuitBreakerConfig circuitBreakerConfig;

    @Override
    public void init(InitContext ctx) throws PolarisException {
        circuitBreakerConfig = ctx.getConfig().getConsumer().getCircuitBreaker();
        OutlierDetectionConfig outlierDetection = ctx.getConfig().getConsumer().getOutlierDetection();
        metricWindowMs = circuitBreakerConfig.getCheckPeriod();
        HalfOpenConfig halfOpenConfig = new HalfOpenConfig(circuitBreakerConfig, outlierDetection);
        Config cfg = circuitBreakerConfig.getPluginConfig(getName(), Config.class);
        if (cfg == null) {
            throw new PolarisException(ErrorCode.INVALID_CONFIG,
                    String.format("plugin %s config is missing", getName()));
        }
        ConfigSet<Config> configSet = new ConfigSet<>(StatusDimension.Level.SERVICE, false, halfOpenConfig, cfg);
        create = new Function<Integer, Object>() {
            @Override
            public Object apply(Integer integer) {
                return new ErrRateCounter(metricWindowName, configSet.getPlugConfig(), getBucketIntervalMs());
            }
        };
        configGroup = new ConfigGroup<>(configSet);
        stateMachine = new StateMachineImpl(configGroup, id, this, metricWindowMs);
        flowControlParam = new DefaultFlowControlParam(ctx.getConfig().getGlobal().getAPI());
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        this.extensions = extensions;
        localRegistry = extensions.getLocalRegistry();
    }

    @Override
    public boolean stat(InstanceGauge gauge) {
        InstanceByProto instance = ChangeStateUtils.getInstance(gauge, localRegistry);
        if (null == instance) {
            return false;
        }
        InstanceLocalValue instanceLocalValue = instance.getInstanceLocalValue();
        if (null == instanceLocalValue) {
            return false;
        }
        ConfigSet<Config> configSet = CircuitBreakUtils.getConfigSet(gauge, this);
        StatusDimension statusDimension = ChangeStateUtils.buildStatusDimension(gauge, configSet.getLevel());
        if (CircuitBreakUtils.instanceClose(instance, statusDimension)) {
            Object pluginValue = instanceLocalValue.getPluginValue(id, create);
            ErrRateCounter errRateCounter = (ErrRateCounter) pluginValue;
            SliceWindow metricWindow = errRateCounter.getSliceWindow(statusDimension);
            metricWindow.addGauge((bucket -> {
                bucket.addMetric(Dimension.keyRequestCount.ordinal(), 1);
                if (gauge.getRetStatus() == RetStatus.RetFail) {
                    return bucket
                            .addMetric(Dimension.keyFailCount.ordinal(), 1);
                }
                return bucket.getMetric(Dimension.keyFailCount.ordinal());
            }));
        } else if (CircuitBreakUtils.instanceHalfOpen(instance, statusDimension)) {
            //半开计数器
            Object pluginValue = instanceLocalValue.getPluginValue(id, create);
            HalfOpenCounter consecutiveCounter = (HalfOpenCounter) pluginValue;
            RetStatus retStatus = gauge.getRetStatus();
            return consecutiveCounter
                    .triggerHalfOpenConversion(statusDimension, retStatus, configSet.getHalfOpenConfig());
        }
        return false;
    }

    public long getBucketIntervalMs() {
        double bucketIntervalMs =
                (double) metricWindowMs / (double) configGroup.getLocalConfig().getPlugConfig().getMetricNumBuckets();
        return (long) Math.ceil(bucketIntervalMs);
    }

    @Override
    public CircuitBreakResult checkInstance(Collection<Instance> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }
        StateMachine.Parameter parameter = new StateMachine.Parameter(id, getName(),
                configGroup.getLocalConfig().getHalfOpenConfig().getHalfOpenMaxReqCount());
        return ChangeStateUtils.buildCircuitBreakResult(stateMachine, instances, parameter);
    }

    @Override
    public CircuitBreakResult checkSubset(Collection<Subset> subsets) {
        return null;
    }

    @Override
    public String getName() {
        return DefaultPlugins.CIRCUIT_BREAKER_ERROR_RATE;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return Config.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CIRCUIT_BREAKER.getBaseType();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public ConfigSet<Config> getConfigSet(RuleIdentifier ruleIdentifier) {
        return configGroup.getServiceConfig(ruleIdentifier,
                new Function<RuleIdentifier, ConfigSet<Config>>() {
                    @Override
                    public ConfigSet<Config> apply(RuleIdentifier ruleIdentifier) {
                        RuleDestinationResult ruleDestResultErrRate = circuitBreakerConfig.isEnableRemotePull() ?
                                CircuitBreakUtils.getRuleDestinationSet(ruleIdentifier, extensions, flowControlParam) :
                                RuleDestinationResult.defaultValue();
                        DestinationSet ruleDestinationSetErrRate = ruleDestResultErrRate.getDestinationSet();
                        if (null == ruleDestinationSetErrRate) {
                            return new ConfigSet<>(StatusDimension.Level.SERVICE, true, null, null);
                        }
                        CbPolicy policy = ruleDestinationSetErrRate.getPolicy();
                        HalfOpenConfig halfOpenConfigErrRate = configGroup.getLocalConfig().getHalfOpenConfig();
                        RecoverConfig recoverConfigErrRate = ruleDestinationSetErrRate.getRecover();
                        if (null != recoverConfigErrRate) {
                            halfOpenConfigErrRate = new HalfOpenConfig(halfOpenConfigErrRate, recoverConfigErrRate);
                        }
                        Config targetPlugConfig = configGroup.getLocalConfig().getPlugConfig();
                        if (null != policy) {
                            ErrRateConfig errorRateConfig = policy.getErrorRate();
                            if (null != errorRateConfig && errorRateConfig.hasEnable() && errorRateConfig.getEnable()
                                    .getValue()) {
                                targetPlugConfig = new Config(targetPlugConfig, errorRateConfig);
                            }
                        }
                        return new ConfigSet<>(ruleDestResultErrRate.getMatchLevel(), false, halfOpenConfigErrRate,
                                targetPlugConfig);
                    }
                });
    }
}
