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

package com.tencent.polaris.plugin.lossless.deregister;

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
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.logging.LoggingConsts;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeregisterLosslessPolicy implements LosslessPolicy, HttpServerAware {

    private static final Logger LOG = LoggerFactory.getLogger(DeregisterLosslessPolicy.class);

    private static final Logger EVENT_LOG = LoggerFactory.getLogger(LoggingConsts.LOGGING_LOSSLESS_EVENT);

    private LosslessConfig losslessConfig;

    private ValueContext valueContext;

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
        handlers.put(OFFLINE_PATH, new DeregisterHandler());
        return handlers;
    }

    @Override
    public boolean allowPortDrift() {
        // 优雅上下线端口会配置在K8S的脚本中，不允许漂移
        return false;
    }

    private class DeregisterHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InetSocketAddress remoteAddress = exchange.getRemoteAddress();
            LOG.info("[LosslessDeregister] received lossless deregister request from {}", remoteAddress);
            if (!remoteAddress.getAddress().isLoopbackAddress()) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
            if (CollectionUtils.isEmpty(actionProviders)) {
                LOG.warn("[LosslessDeRegister] LosslessActionProvider not found, no lossless action will be taken");
                HttpServerUtils.writeTextToHttpServer(exchange, REPS_TEXT_NO_ACTION, 200);
                return;
            }
            String text;
            int code;
            Map<BaseInstance, RegisterStatus> registerStatusMap = valueContext.getValue(CTX_KEY_REGISTER_STATUS);
            try {
                for (Map.Entry<BaseInstance, LosslessActionProvider> entry : actionProviders.entrySet()) {
                    BaseInstance instance = entry.getKey();
                    LosslessActionProvider actionProvider = entry.getValue();
                    actionProvider.doDeregister();
                    registerStatusMap.put(instance, RegisterStatus.UNREGISTERED);
                    // record event log
                    String clientId = valueContext.getClientId();
                    Event event = new Event();
                    event.setClientId(clientId);
                    event.setBaseInstance(instance);
                    event.setEventName(EVENT_LOSSLESS_DEREGISTER);
                    EVENT_LOG.info(event.toString());
                }
                text = REPS_TEXT_OK;
                code = 200;
            } catch (Throwable e) {
                LOG.error("[LosslessDeRegister] fail to execute deregister", e);
                text = REPS_TEXT_FAILED + ": " + e.getMessage();
                code = 500;
            }
            HttpServerUtils.writeTextToHttpServer(exchange, text, code);

        }
    }

    @Override
    public String getName() {
        return "deregister-lossless";
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

    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void buildInstanceProperties(InstanceProperties instanceProperties) {

    }

    @Override
    public void losslessRegister(InstanceProperties instanceProperties) {

    }
}
