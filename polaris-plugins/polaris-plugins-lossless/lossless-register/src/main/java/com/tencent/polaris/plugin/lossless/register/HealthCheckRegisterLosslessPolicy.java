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
import com.tencent.polaris.client.util.HttpServerUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HealthCheckRegisterLosslessPolicy implements LosslessPolicy, HttpServerAware {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckRegisterLosslessPolicy.class);

    private LosslessConfig losslessConfig;

    private ValueContext valueContext;

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
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

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
            LOG.info("[LosslessRegister] do health-check for lossless register, result {}", result);
            if (!result) {
                healthCheckExecutor.schedule(this, losslessConfig.getHealthCheckInterval(), TimeUnit.MILLISECONDS);
                return;
            }
            LOG.info("[LosslessRegister] health-check success, start to do register");
            try {
                doRegister(instance, losslessActionProvider, instanceProperties);
            } catch (Throwable throwable) {
                LOG.error("[LosslessRegister] fail to do lossless register in plugin {}", getName(), throwable);
            }
        }
    }

    private void doLosslessRegister(
            BaseInstance instance, LosslessActionProvider losslessActionProvider, InstanceProperties instanceProperties) {
        LOG.info("[LosslessRegister] do losslessRegister for instance {}", instance);
        if (!losslessActionProvider.isEnableHealthCheck()) {
            LOG.info("[LosslessRegister] health check disabled, start lossless register after {}ms, plugin {}",
                    losslessConfig.getDelayRegisterInterval(), getName());
            healthCheckExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    LOG.info("[LosslessRegister] health-check disabled, start to do register now");
                    doRegister(instance, losslessActionProvider, instanceProperties);
                }
            }, losslessConfig.getDelayRegisterInterval(), TimeUnit.MILLISECONDS);
        } else {
            LOG.info("[LosslessRegister] health check enabled, start lossless register after check, interval {}ms, plugin {}",
                    losslessConfig.getHealthCheckInterval(), getName());
            HealthChecker healthChecker = new HealthChecker(instance, losslessActionProvider, instanceProperties);
            healthCheckExecutor.schedule(
                    healthChecker, losslessConfig.getHealthCheckInterval(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void losslessRegister(InstanceProperties instanceProperties) {
        LOG.info("[LosslessRegister] start to do lossless register by plugin {}", getName());

        Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
        if (CollectionUtils.isEmpty(actionProviders)) {
            LOG.warn("[LosslessRegister] LosslessActionProvider not found, no lossless action will be taken");
            return;
        }
        if (stopped.get()) {
            LOG.info("[LosslessRegister] plugin {} stopped, not lossless register action will be taken", getName());
            return;
        }
        for (Map.Entry<BaseInstance, LosslessActionProvider> entry : actionProviders.entrySet()) {
            doLosslessRegister(entry.getKey(), entry.getValue(), instanceProperties);
        }
    }

    private void doRegister(BaseInstance instance,
                            LosslessActionProvider losslessActionProvider, InstanceProperties instanceProperties) {
        losslessActionProvider.doRegister(instanceProperties);
        Map<BaseInstance, RegisterStatus> registerStatusMap = valueContext.getValue(CTX_KEY_REGISTER_STATUS);
        registerStatusMap.put(instance, RegisterStatus.REGISTERED);
    }

    @Override
    public String getHost() {
        return losslessConfig.getHost();
    }

    @Override
    public int getPort() {
        return losslessConfig.getPort();
    }

    @Override
    public Map<String, HttpHandler> getHandlers() {
        if (!losslessConfig.isEnable()) {
            return Collections.emptyMap();
        }
        Map<String, HttpHandler> handlers = new HashMap<>();
        handlers.put(ONLINE_PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                RegisterStatus finalStatus = RegisterStatus.REGISTERED;
                Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
                // 全部实例都已经注册成功，才返回注册成功
                if (CollectionUtils.isNotEmpty(actionProviders)) {
                    Map<BaseInstance, RegisterStatus> registerStatuses = valueContext.getValue(CTX_KEY_REGISTER_STATUS);
                    if (CollectionUtils.isNotEmpty(registerStatuses)) {
                        for (BaseInstance baseInstance : actionProviders.keySet()) {
                            RegisterStatus registerStatus = registerStatuses.get(baseInstance);
                            if (registerStatus != RegisterStatus.REGISTERED) {
                                finalStatus = RegisterStatus.UNREGISTERED;
                            }
                        }
                    }
                } else {
                    finalStatus = RegisterStatus.UNREGISTERED;
                }
                HttpServerUtils.writeTextToHttpServer(
                        exchange, finalStatus.toString(), finalStatus == RegisterStatus.REGISTERED ? 200 : 404);
            }
        });
        return handlers;
    }

    @Override
    public boolean allowPortDrift() {
        // 优雅上下线端口会配置在K8S的脚本中，不允许漂移
        return false;
    }
}
