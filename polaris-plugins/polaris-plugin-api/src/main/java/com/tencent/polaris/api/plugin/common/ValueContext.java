/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.polaris.api.plugin.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于主流程传递kv数据的上下文对象，线程安全
 *
 * @author andrewshan, Haotian Zhang
 */
public class ValueContext {

    private static final Logger LOG = LoggerFactory.getLogger(ValueContext.class);

    private static final String KEY_HOST = "key_host";

    private static final String KEY_CLIENT_ID = "key_clientId";

    private static final String KEY_ENGINE = "key_engine";

    private static final String KEY_SERVER_CONNECTOR_PROTOCOL = "key_serverConnectorProtocol";

    private final Object lock = new Object();
    private final Map<Object, Object> coreMap;
    private volatile boolean locationReady;

    public ValueContext() {
        coreMap = new ConcurrentHashMap<>();
    }

    public <T> void setValue(String key, T value) {
        coreMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        return (T) coreMap.get(key);
    }

    /**
     * 等待地域信息就绪
     *
     * @param timeoutMs 超时时间
     * @throws InterruptedException 中断异常
     */
    public void waitForLocationReady(long timeoutMs) throws InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("waitForLocationReady: timeoutMs:{}, is ready:{},object:{}", timeoutMs,
                    locationReady, this);
        }
        if (locationReady) {
            return;
        }

        synchronized (lock) {
            lock.wait(timeoutMs);
        }
    }

    public String getHost() {
        return getValue(KEY_HOST);
    }

    public void setHost(String host) {
        setValue(KEY_HOST, host);
    }

    public String getClientId() {
        return getValue(KEY_CLIENT_ID);
    }

    public void setClientId(String clientId) {
        setValue(KEY_CLIENT_ID, clientId);
    }

    public String getServerConnectorProtocol() {
        return getValue(KEY_SERVER_CONNECTOR_PROTOCOL);
    }

    public void setServerConnectorProtocol(String protocol) {
        setValue(KEY_SERVER_CONNECTOR_PROTOCOL, protocol);
    }

    public void notifyAllForLocationReady() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("notifyAllForLocationReady:object:{}", this);
        }
        locationReady = true;

        synchronized (lock) {
            lock.notifyAll();
        }
    }

}
