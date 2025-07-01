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
import com.tencent.polaris.api.plugin.event.EventConstants;
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
import com.tencent.polaris.plugin.lossless.common.LosslessEventUtils;
import com.tencent.polaris.plugin.lossless.common.LosslessUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeregisterLosslessPolicy implements LosslessPolicy, HttpServerAware {

    private static final Logger LOG = LoggerFactory.getLogger(DeregisterLosslessPolicy.class);

    private static final Logger EVENT_LOG = LoggerFactory.getLogger(LoggingConsts.LOGGING_LOSSLESS_EVENT);

    private LosslessConfig losslessConfig;

    private ValueContext valueContext;

    private Extensions extensions;

    @Override
    public Map<String, HttpHandler> getHandlers() {
        if (!losslessConfig.isEnable()) {
            return Collections.emptyMap();
        }
        Map<String, HttpHandler> handlers = new HashMap<>();
        handlers.put(OFFLINE_PATH, new DeregisterHandler());
        return handlers;
    }

    private class DeregisterHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InetSocketAddress remoteAddress = exchange.getRemoteAddress();
            LOG.info("[LosslessDeregister] received lossless deregister request from {}", remoteAddress);
            if (!remoteAddress.getAddress().isLoopbackAddress()) {
                LOG.warn("[LosslessDeRegister] only loop-back address (like localhost or 127.0.0.1) can call this path");
                HttpServerUtils.writeTextToHttpServer(exchange, REPS_TEXT_ONLY_LOCALHOST, 403);
                return;
            }
            Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
            if (CollectionUtils.isEmpty(actionProviders)) {
                LOG.warn("[LosslessDeRegister] LosslessActionProvider not found, no lossless action will be taken");
                HttpServerUtils.writeTextToHttpServer(exchange, REPS_TEXT_NO_ACTION, 200);
                return;
            }

            List<LosslessPolicy> losslessPolicies = extensions.getLosslessPolicies();
            if (CollectionUtils.isEmpty(losslessPolicies)) {
                LOG.warn("[LosslessDeRegister] lossless is disabled, no losslessDeregister will do");
                HttpServerUtils.writeTextToHttpServer(exchange, REPS_TEXT_NO_POLICY, 500);
                return;
            }

            Set<BaseInstance> needOfflineInstances = getNeedOfflineInstances(actionProviders.keySet());
            if (CollectionUtils.isEmpty(needOfflineInstances)) {
                LOG.warn("[LosslessDeRegister] no instance needs to be offline");
                HttpServerUtils.writeTextToHttpServer(exchange, "", 404);
                return;
            }

            String text;
            int code;
            try {
                for (BaseInstance instance : needOfflineInstances) {
                    for (LosslessPolicy losslessPolicy : losslessPolicies) {
                        losslessPolicy.losslessDeregister(instance);
                    }
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
        extensions = ctx;
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
    public void losslessRegister(BaseInstance instance, InstanceProperties instanceProperties) {

    }

    @Override
    public void losslessDeregister(BaseInstance instance) {
        Map<BaseInstance, LosslessActionProvider> actionProviders = valueContext.getValue(LosslessActionProvider.CTX_KEY);
        if (CollectionUtils.isEmpty(actionProviders)) {
            LOG.warn("[LosslessDeRegister] LosslessActionProvider not found, " +
                    "no lossless action will be taken for instance {}", instance);
            return;
        }
        LosslessActionProvider losslessActionProvider = actionProviders.get(instance);
        if (null == losslessActionProvider) {
            LOG.warn("[LosslessDeRegister] LosslessActionProvider not found for instance {}", instance);
            return;
        }
        doLosslessDeregister(instance, losslessActionProvider);
    }

    private void doLosslessDeregister(BaseInstance instance, LosslessActionProvider actionProvider) {
        actionProvider.doDeregister();
        Map<BaseInstance, RegisterStatus> registerStatusMap = valueContext.getValue(CTX_KEY_REGISTER_STATUS);
        registerStatusMap.put(instance, RegisterStatus.UNREGISTERED);
        // record event log
        String clientId = valueContext.getClientId();
        Event event = new Event();
        event.setClientId(clientId);
        event.setBaseInstance(instance);
        event.setEventName(EVENT_LOSSLESS_DEREGISTER);
        EVENT_LOG.info(event.toString());
        LosslessEventUtils.reportEvent(extensions, instance.getNamespace(), instance.getService(), instance.getHost(),
                instance.getPort(), EventConstants.EventName.LosslessOfflineStart);
    }

    private boolean isLosslessOfflineEnable(BaseInstance instance) {
        // high priority for console configuration
        LosslessProto.LosslessRule losslessRule = LosslessUtils.getMatchLosslessRule(extensions, instance);
        return Optional.ofNullable(losslessRule).
                map(LosslessProto.LosslessRule::getLosslessOffline).
                map(LosslessProto.LosslessOffline::getEnable).
                orElse(true);
    }

    private Set<BaseInstance> getNeedOfflineInstances(Set<BaseInstance> instances) {
        Set<BaseInstance> needOfflineInstances = new HashSet<>();
        for (BaseInstance instance : instances) {
            if (isLosslessOfflineEnable(instance)) {
                needOfflineInstances.add(instance);
            } else {
                LOG.info("[LosslessDeregister] lossless offline is disabled for instance {}", instance);
            }
        }
        return needOfflineInstances;
    }
}
