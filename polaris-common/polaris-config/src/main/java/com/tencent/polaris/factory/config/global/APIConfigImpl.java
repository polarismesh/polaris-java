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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

/**
 * api相关的配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class APIConfigImpl implements APIConfig {
    @JsonProperty
    private Integer maxRetryTimes;

    @JsonProperty
    private String bindIf;

    @JsonProperty
    private String bindIP;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long timeout;

    @JsonProperty
    private Boolean reportEnable;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long reportInterval;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long retryInterval;


    @Override
    public int getMaxRetryTimes() {
        if (null == maxRetryTimes) {
            return 0;
        }
        return maxRetryTimes;
    }

    public void setMaxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    @Override
    public String getBindIf() {
        return bindIf;
    }

    @Override
    public String getBindIP() {
        return bindIP;
    }

    public void setBindIP(String bindIP) {
        this.bindIP = bindIP;
    }

    public void setBindIf(String bindIf) {
        this.bindIf = bindIf;
    }

    @Override
    public long getTimeout() {
        if (null == timeout) {
            return 0;
        }
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isReportEnable() {
        if (null == reportEnable) {
            return true;
        }
        return reportEnable;
    }

    public void setReportEnable(boolean reportEnable) {
        this.reportEnable = reportEnable;
    }

    @Override
    public long getReportInterval() {
        if (null == reportInterval) {
            return 0;
        }
        return reportInterval;
    }

    public void setReportInterval(long reportInterval) {
        this.reportInterval = reportInterval;
    }

    @Override
    public long getRetryInterval() {
        if (null == retryInterval) {
            return 0;
        }
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    @Override
    public void verify() {
        ConfigUtils.validateTimes(maxRetryTimes, "api.maxRetryTimes");
        ConfigUtils.validateInterval(timeout, "api.timeout");
        ConfigUtils.validateInterval(reportInterval, "api.reportInterval");
        ConfigUtils.validateInterval(retryInterval, "api.retryInterval");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            APIConfig apiConfig = (APIConfig) defaultObject;
            if (null == maxRetryTimes) {
                setMaxRetryTimes(apiConfig.getMaxRetryTimes());
            }
            if (null == bindIf) {
                setBindIf(apiConfig.getBindIf());
            }
            if (null == bindIP) {
                setBindIP(apiConfig.getBindIP());
            }
            if (null == timeout) {
                setTimeout(apiConfig.getTimeout());
            }
            if (null == reportInterval) {
                setReportInterval(apiConfig.getReportInterval());
            }
            if (null == reportEnable) {
                setReportEnable(apiConfig.isReportEnable());
            }
            if (null == retryInterval) {
                setRetryInterval(apiConfig.getRetryInterval());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "APIConfigImpl{" +
                "maxRetryTimes=" + maxRetryTimes +
                ", bindIf='" + bindIf + '\'' +
                ", bindIP='" + bindIP + '\'' +
                ", timeout=" + timeout +
                ", reportEnable=" + reportEnable +
                ", reportInterval=" + reportInterval +
                ", retryInterval=" + retryInterval +
                '}';
    }
}
