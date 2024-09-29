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

package com.tencent.polaris.plugin.lossless.register;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.api.config.provider.LosslessConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.HttpServerAware;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.plugin.lossless.LosslessPolicy;
import com.tencent.polaris.api.plugin.lossless.RegisterStatus;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.Event;
import com.tencent.polaris.client.util.HttpServerUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.logging.LoggingConsts;
import com.tencent.polaris.plugin.lossless.common.LosslessUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HealthCheckRegisterLosslessPolicy implements LosslessPolicy, HttpServerAware {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckRegisterLosslessPolicy.class);

    private static final Logger EVENT_LOG = LoggerFactory.getLogger(LoggingConsts.LOGGING_LOSSLESS_EVENT);

    private LosslessConfig losslessConfig;

    private ValueContext valueContext;

    private Extensions extensions;

    private final ScheduledExecutorService healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(
            new NamedThreadFactory("lossless-register-check"));

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Override
    public String getName() {
        return "health-check-register-lossless";
    }

    @Override
    public PluginType getType() {
        return PluginTypes.LOSSLESS_POLICY.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        losslessConfig = ctx.getConfig().getProvider().getLossless();
        valueContext = ctx.getValueContext();
        if (!valueContext.containsValue(CTX_KEY_REGISTER_STATUS)) {
            Map<BaseInstance, RegisterStatus> registerStatuses = new ConcurrentHashMap<>();
            valueContext.setValue(CTX_KEY_REGISTER_STATUS, registerStatuses);
        }
        if (!valueContext.containsValue(CTX_KEY_REGISTER_TIMESTAMP)) {
            Map<BaseInstance, Long> registerTimestamps = new ConcurrentHashMap<>();
            valueContext.setValue(CTX_KEY_REGISTER_TIMESTAMP, registerTimestamps);
        }
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        extensions = ctx;
    }

    @Override
    public void destroy() {
        stopped.set(true);
        healthCheckExecutor.shutdownNow();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void buildInstanceProperties(InstanceProperties instanceProperties) {

    }

    @Override
    public void losslessRegister(BaseInstance instance, InstanceProperties instanceProperties) {
        LOG.info("[HealthCheckRegisterLosslessPolicy] start to do lossless register by plugin {}", getName());

        Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
        if (CollectionUtils.isEmpty(actionProviders)) {
            LOG.warn("[HealthCheckRegisterLosslessPolicy] LosslessActionProvider not found, no lossless action will be taken");
            return;
        }
        if (stopped.get()) {
            LOG.info("[HealthCheckRegisterLosslessPolicy] plugin {} stopped, not lossless register action will be taken", getName());
            return;
        }
        LosslessActionProvider losslessActionProvider = actionProviders.get(instance);
        if (null == losslessActionProvider) {
            LOG.warn("[HealthCheckRegisterLosslessPolicy] LosslessActionProvider for instance {} not found, " +
                    "no lossless action will be taken", instance);
            return;
        }
        doLosslessRegister(instance, losslessActionProvider, instanceProperties);
    }

    @Override
    public void losslessDeregister(BaseInstance instance) {

    }

    private class HealthChecker implements Runnable {

        final BaseInstance instance;

        final LosslessActionProvider losslessActionProvider;

        final InstanceProperties instanceProperties;

        public HealthChecker(BaseInstance instance,
                             LosslessActionProvider losslessActionProvider, InstanceProperties instanceProperties) {
            this.instance = instance;
            this.losslessActionProvider = losslessActionProvider;
            this.instanceProperties = instanceProperties;
        }

        @Override
        public void run() {
            boolean result = losslessActionProvider.doHealthCheck();
            LOG.info("[HealthCheckRegisterLosslessPolicy] do health-check for lossless register, result {}", result);
            if (!result) {
                healthCheckExecutor.schedule(this, getHealthCheckInterval(instance), TimeUnit.MILLISECONDS);
                return;
            }
            LOG.info("[HealthCheckRegisterLosslessPolicy] health-check success, start to do register");
            try {
                doRegister(instance, losslessActionProvider, instanceProperties, true);
            } catch (Throwable throwable) {
                LOG.error("[HealthCheckRegisterLosslessPolicy] fail to do lossless register in plugin {}", getName(), throwable);
            }
        }
    }

    private void doLosslessRegister(
            BaseInstance instance, LosslessActionProvider losslessActionProvider, InstanceProperties instanceProperties) {

        if (!isDelayRegisterEnable(instance)) {
            LOG.info("[HealthCheckRegisterLosslessPolicy] console lossless register disabled, start to do register now");
            doRegister(instance, losslessActionProvider, instanceProperties, false);
            return;
        }

        LOG.info("[HealthCheckRegisterLosslessPolicy] do losslessRegister for instance {}", instance);
        if (!losslessActionProvider.isEnableHealthCheck()) {
            long delayRegisterInterval = getDelayRegisterInterval(instance);
            LOG.info("[HealthCheckRegisterLosslessPolicy] health check disabled, start lossless register after {}ms, plugin {}",
                    delayRegisterInterval, getName());
            Event delayRegisterStartEvent = new Event(valueContext.getClientId(), instance, EVENT_LOSSLESS_DELAY_REGISTER_START);
            EVENT_LOG.info(delayRegisterStartEvent.toString());
            healthCheckExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    LOG.info("[HealthCheckRegisterLosslessPolicy] health-check disabled, start to do register now");
                    doRegister(instance, losslessActionProvider, instanceProperties, true);
                }
            }, delayRegisterInterval, TimeUnit.MILLISECONDS);
        } else {
            long healthCheckInterval = getHealthCheckInterval(instance);
            LOG.info("[HealthCheckRegisterLosslessPolicy] health check enabled, start lossless register after check, interval {}ms, plugin {}",
                    healthCheckInterval, getName());
            HealthChecker healthChecker = new HealthChecker(instance, losslessActionProvider, instanceProperties);
            healthCheckExecutor.schedule(
                    healthChecker, healthCheckInterval, TimeUnit.MILLISECONDS);
        }
    }

    private void doRegister(BaseInstance instance, LosslessActionProvider losslessActionProvider,
            InstanceProperties instanceProperties, boolean isDelayRegister) {
        losslessActionProvider.doRegister(instanceProperties);
        Map<BaseInstance, RegisterStatus> registerStatusMap = valueContext.getValue(CTX_KEY_REGISTER_STATUS);
        registerStatusMap.put(instance, RegisterStatus.REGISTERED);
        // record event log
        String clientId = valueContext.getClientId();
        if (isDelayRegister) {
            EVENT_LOG.info(new Event(clientId, instance, EVENT_LOSSLESS_REGISTER).toString());
        } else {
            EVENT_LOG.info(new Event(clientId, instance, EVENT_DIRECT_REGISTER).toString());
        }



        int warmupInterval = getWarmupInterval(instance);
        if (warmupInterval > 0) {
            LOG.info("[HealthCheckRegisterLosslessPolicy] warmup for instance {}, warmupInterval:{}ms", instance, warmupInterval);
            // need warmup
            Map<BaseInstance, Long> registerTimestamp = valueContext.getValue(CTX_KEY_REGISTER_TIMESTAMP);
            registerTimestamp.put(instance, System.currentTimeMillis());

            Event warmupStartEvent = new Event(clientId, instance, EVENT_LOSSLESS_WARMUP_START);
            EVENT_LOG.info(warmupStartEvent.toString());

            healthCheckExecutor.schedule(() -> {
                LOG.info("[HealthCheckRegisterLosslessPolicy] warmup for instance {} finished", instance);
                Event warmupEndEvent = new Event(clientId, instance, EVENT_LOSSLESS_WARMUP_END);
                EVENT_LOG.info(warmupEndEvent.toString());
            }, warmupInterval, TimeUnit.MILLISECONDS);
        } else {
            LOG.info("[HealthCheckRegisterLosslessPolicy] no warmup for instance {}", instance);
        }
    }

    @Override
    public String getHost() {
        return losslessConfig.getHost();
    }

    @Override
    public int getPort() {
        return losslessConfig.getPort();
    }

    static RegisterStatus checkRegisterStatus(
            Collection<BaseInstance> instances, Map<BaseInstance, RegisterStatus> registerStatuses) {
        RegisterStatus finalStatus = RegisterStatus.REGISTERED;
        if (CollectionUtils.isNotEmpty(instances)) {
            if (CollectionUtils.isNotEmpty(registerStatuses)) {
                for (BaseInstance baseInstance : instances) {
                    RegisterStatus registerStatus = registerStatuses.get(baseInstance);
                    if (registerStatus != RegisterStatus.REGISTERED) {
                        finalStatus = RegisterStatus.UNREGISTERED;
                        LOG.info("[HealthCheckRegisterLosslessPolicy] instance {} not register, register status is unregistered", baseInstance);
                        break;
                    }
                }
            } else {
                LOG.info("[HealthCheckRegisterLosslessPolicy] no instances registered, register status is unregistered");
                // 如果没有实例发起过上线，那么这个还是未注册状态
                finalStatus = RegisterStatus.UNREGISTERED;
            }
        } else {
            LOG.info("[HealthCheckRegisterLosslessPolicy] instances is empty, register status is unregistered");
            // 没有actionProvider，无法发现优雅上线，那么这个还是未上线状态
            finalStatus = RegisterStatus.UNREGISTERED;
        }
        return finalStatus;
    }

    private RegisterStatus checkWarmupStatus(Collection<BaseInstance> instances) {
        int needWarmupInstances = 0;
        int doneWarmupInstances = 0;
        long current = System.currentTimeMillis();
        Map<BaseInstance, Long> warmupTimestamps = valueContext.getValue(CTX_KEY_REGISTER_TIMESTAMP);
        for (BaseInstance baseInstance : instances) {
            int warmupInterval = getWarmupInterval(baseInstance);
            if (warmupInterval > 0) {
                needWarmupInstances++;
                long registerTimestamp = warmupTimestamps.get(baseInstance);
                if (registerTimestamp > 0 && current - registerTimestamp > warmupInterval) {
                    doneWarmupInstances++;
                }
            }
        }
        if (needWarmupInstances == 0) {
            return RegisterStatus.REGISTERED;
        }
        if (doneWarmupInstances == needWarmupInstances) {
            return RegisterStatus.WARMUP_END;
        } else {
            return RegisterStatus.WARMUP_START;
        }
    }

    @Override
    public Map<String, HttpHandler> getHandlers() {
        if (!losslessConfig.isEnable()) {
            return Collections.emptyMap();
        }
        Map<String, HttpHandler> handlers = new HashMap<>();
        handlers.put(READINESS_PATH, new ReadinessHttpHandler());
        handlers.put(DEPRECATED_READINESS_PATH, new ReadinessHttpHandler());
        return handlers;
    }

    @Override
    public boolean allowPortDrift() {
        // 优雅上下线端口会配置在K8S的脚本中，不允许漂移
        return false;
    }

    private Set<BaseInstance> getNeedReadinessInstances(Set<BaseInstance> instances) {
        Set<BaseInstance> needReadinessInstances = new HashSet<>();
        for (BaseInstance instance : instances) {
            if (isReadinessEnable(instance)) {
                needReadinessInstances.add(instance);
            }
        }
        return needReadinessInstances;
    }

    private boolean isReadinessEnable(BaseInstance instance) {
        LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
                instance.getNamespace(), instance.getService());
        // high priority for console configuration
        return  Optional.ofNullable(losslessRule).
                map(LosslessProto.LosslessRule::getLosslessOnline).
                map(LosslessProto.LosslessOnline::getReadiness).
                map(LosslessProto.Readiness::getEnable).
                orElse(true);
    }

    private boolean isDelayRegisterEnable(BaseInstance instance) {
        LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
                instance.getNamespace(), instance.getService());
        // high priority for console configuration
        return  Optional.ofNullable(losslessRule).
                map(LosslessProto.LosslessRule::getLosslessOnline).
                map(LosslessProto.LosslessOnline::getDelayRegister).
                map(LosslessProto.DelayRegister::getEnable).
                orElse(true);
    }

    private int getWarmupInterval(BaseInstance instance) {
        LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
                instance.getNamespace(), instance.getService());
        return Optional.ofNullable(losslessRule).
                map(LosslessProto.LosslessRule::getLosslessOnline).
                map(LosslessProto.LosslessOnline::getWarmup).
                filter(LosslessProto.Warmup::getEnable).
                map(LosslessProto.Warmup::getIntervalSecond).map(interval -> interval * 1000).
                orElse(0);
    }

    private long getDelayRegisterInterval(BaseInstance instance) {
        LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
                instance.getNamespace(), instance.getService());
        return Optional.ofNullable(losslessRule).
                map(LosslessProto.LosslessRule::getLosslessOnline).
                map(LosslessProto.LosslessOnline::getDelayRegister).
                map(LosslessProto.DelayRegister::getIntervalSecond).
                map(Long::valueOf).map(interval -> interval * 1000).
                orElse(losslessConfig.getDelayRegisterInterval());
    }

    private long getHealthCheckInterval(BaseInstance instance) {
        LosslessProto.LosslessRule losslessRule = LosslessUtils.getFirstLosslessRule(extensions,
                instance.getNamespace(), instance.getService());
        return Optional.ofNullable(losslessRule).
                map(LosslessProto.LosslessRule::getLosslessOnline).
                map(LosslessProto.LosslessOnline::getDelayRegister).
                map(LosslessProto.DelayRegister::getHealthCheckIntervalSecond).
                map(Long::valueOf).map(interval -> interval * 1000).
                orElse(losslessConfig.getHealthCheckInterval());
    }

    class ReadinessHttpHandler implements HttpHandler {

        private RegisterStatus lastFinalStatus;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
            Map<BaseInstance, RegisterStatus> registerStatuses = valueContext.getValue(CTX_KEY_REGISTER_STATUS);
            Set<BaseInstance> instances = actionProviders.keySet();

            Set<BaseInstance> needReadinessInstances = getNeedReadinessInstances(instances);
            if (CollectionUtils.isEmpty(needReadinessInstances)) {
                LOG.debug("[HealthCheckRegisterLosslessPolicy] no instance need readiness check");
                HttpServerUtils.writeTextToHttpServer(exchange, REPS_TEXT_NO_INSTANCE_NEED_READINESS_CHECK, 404);
                exchange.close();
                return;
            }

            RegisterStatus finalStatus = checkRegisterStatus(needReadinessInstances, registerStatuses);
            if (finalStatus == RegisterStatus.REGISTERED) {
                // all registered, need check warmup status
                finalStatus = checkWarmupStatus(needReadinessInstances);
            }
            if (finalStatus != lastFinalStatus) {
                LOG.info("[HealthCheckRegisterLosslessPolicy] receive /online request for instances {}, " +
                        "finalStatus from {} to {}", needReadinessInstances, lastFinalStatus, finalStatus);
                // 最终一致性，无需做多线程保护
                lastFinalStatus = finalStatus;
            } else {
                LOG.debug("[HealthCheckRegisterLosslessPolicy] receive /online request for instances {}, " +
                        "finalStatus from {} to {}", needReadinessInstances, lastFinalStatus, finalStatus);
            }
            int responseCode;
            switch (finalStatus) {
            case REGISTERED:
            case WARMUP_END:
                responseCode = 200;
                break;
            default:
                responseCode = 503;
                break;
            }
            HttpServerUtils.writeTextToHttpServer(
                    exchange, finalStatus.toString(), responseCode);
        }
    }
}
