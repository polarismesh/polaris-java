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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.consumer.LocalCacheConfig;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

/**
 * 本地缓存相关配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class LocalCacheConfigImpl extends PluginConfigImpl implements LocalCacheConfig {

    @JsonProperty
    private Boolean serviceExpireEnable;

    @JsonProperty
    private Boolean servicePushEmptyProtectEnable;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long serviceExpireTime;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long serviceRefreshInterval;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long serviceListRefreshInterval;

    @JsonProperty
    private Boolean persistEnable;

    @JsonProperty
    private String persistDir;

    @JsonProperty
    private String type;

    @JsonProperty
    private Integer persistMaxWriteRetry;

    @JsonProperty
    private Integer persistMaxReadRetry;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long persistRetryInterval;

    @Override
    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean isPersistEnable() {
        if (null == persistEnable) {
            return false;
        }
        return persistEnable;
    }

    public void setPersistEnable(Boolean persistEnable) {
        this.persistEnable = persistEnable;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int getPersistMaxWriteRetry() {
        if (null == persistMaxWriteRetry) {
            return 0;
        }
        return persistMaxWriteRetry;
    }

    public void setPersistMaxWriteRetry(int persistMaxWriteRetry) {
        this.persistMaxWriteRetry = persistMaxWriteRetry;
    }

    @Override
    public int getPersistMaxReadRetry() {
        if (null == persistMaxReadRetry) {
            return 0;
        }
        return persistMaxReadRetry;
    }

    public void setPersistMaxReadRetry(int persistMaxReadRetry) {
        this.persistMaxReadRetry = persistMaxReadRetry;
    }

    @Override
    public boolean isServiceExpireEnable() {
        if (null == serviceExpireEnable) {
            return false;
        }
        return serviceExpireEnable;
    }

    public void setServiceExpireEnable(boolean serviceExpireEnable) {
        this.serviceExpireEnable = serviceExpireEnable;
    }

    @Override
    public boolean isServicePushEmptyProtectEnable() {
        if(null == servicePushEmptyProtectEnable) {
            return false;
        }
        return servicePushEmptyProtectEnable;
    }

    public void setServicePushEmptyProtectEnable(Boolean servicePushEmptyProtectEnable) {
        this.servicePushEmptyProtectEnable = servicePushEmptyProtectEnable;
    }

    @Override
    public long getServiceExpireTime() {
        if (null == serviceExpireTime) {
            return 0;
        }
        return serviceExpireTime;
    }

    public void setServiceExpireTime(long serviceExpireTime) {
        this.serviceExpireTime = serviceExpireTime;
    }

    @Override
    public long getServiceRefreshInterval() {
        if (null == serviceRefreshInterval) {
            return 0;
        }
        return serviceRefreshInterval;
    }

    public void setServiceRefreshInterval(long serviceRefreshInterval) {
        this.serviceRefreshInterval = serviceRefreshInterval;
    }

    @Override
    public long getServiceListRefreshInterval() {
        return this.serviceListRefreshInterval;
    }

    public void setServiceListRefreshInterval(Long serviceListRefreshInterval) {
        this.serviceListRefreshInterval = serviceListRefreshInterval;
    }

    @Override
    public long getPersistRetryInterval() {
        if (null == persistRetryInterval) {
            return 0;
        }
        return persistRetryInterval;
    }

    public void setPersistRetryInterval(long persistRetryInterval) {
        this.persistRetryInterval = persistRetryInterval;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(serviceExpireEnable, "localCache.serviceExpireEnable");
        ConfigUtils.validateNull(servicePushEmptyProtectEnable, "localCache.servicePushEmptyProtectEnable");
        ConfigUtils.validateIntervalWithMin(serviceExpireTime, DefaultValues.MIN_SERVICE_EXPIRE_TIME_MS,
                "localCache.serviceExpireTime");
        ConfigUtils.validateIntervalWithMin(serviceRefreshInterval, DefaultValues.MIN_SERVICE_REFRESH_INTERVAL_MS,
                "localCache.serviceRefreshInterval");
        ConfigUtils.validateNull(persistEnable, "localCache.persistEnable");
        if (persistEnable) {
            ConfigUtils.validateString(persistDir, "localCache.persistDir");
        }
        ConfigUtils.validateString(type, "localCache.type");
        ConfigUtils.validateTimes(persistMaxWriteRetry, "localCache.persistMaxWriteRetry");
        ConfigUtils.validateTimes(persistMaxReadRetry, "localCache.persistMaxReadRetry");
        ConfigUtils.validateInterval(persistRetryInterval, "localCache.persistRetryInterval");
        verifyPluginConfig();
    }


    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            LocalCacheConfig localCacheConfig = (LocalCacheConfig) defaultObject;
            if (null == serviceExpireEnable) {
                setServiceExpireEnable(localCacheConfig.isServiceExpireEnable());
            }
            if (null == serviceExpireTime) {
                setServiceExpireTime(localCacheConfig.getServiceExpireTime());
            }
            if (null == serviceRefreshInterval) {
                setServiceRefreshInterval(localCacheConfig.getServiceRefreshInterval());
            }
            if (null == serviceListRefreshInterval) {
                setServiceListRefreshInterval(localCacheConfig.getServiceListRefreshInterval());
            }
            if (null == persistEnable) {
                setPersistEnable(localCacheConfig.isPersistEnable());
            }
            if (null == persistDir) {
                setPersistDir(localCacheConfig.getPersistDir());
            }
            if (null == type) {
                setType(localCacheConfig.getType());
            }
            if (null == persistMaxWriteRetry) {
                setPersistMaxWriteRetry(localCacheConfig.getPersistMaxWriteRetry());
            }
            if (null == persistMaxReadRetry) {
                setPersistMaxReadRetry(localCacheConfig.getPersistMaxReadRetry());
            }
            if (null == persistRetryInterval) {
                setPersistRetryInterval(localCacheConfig.getPersistRetryInterval());
            }
            if (null == servicePushEmptyProtectEnable) {
                setServicePushEmptyProtectEnable(localCacheConfig.isServicePushEmptyProtectEnable());
            }
            setDefaultPluginConfig(localCacheConfig);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "LocalCacheConfigImpl{" +
                "serviceExpireEnable=" + serviceExpireEnable +
                ", serviceExpireTime=" + serviceExpireTime +
                ", serviceRefreshInterval=" + serviceRefreshInterval +
                ", persistEnable=" + persistEnable +
                ", persistDir='" + persistDir + '\'' +
                ", type='" + type + '\'' +
                ", persistMaxWriteRetry=" + persistMaxWriteRetry +
                ", persistMaxReadRetry=" + persistMaxReadRetry +
                ", persistRetryInterval=" + persistRetryInterval +
                ", servicePushEmptyProtectEnable=" + servicePushEmptyProtectEnable +
                "} " + super.toString();
    }
}
