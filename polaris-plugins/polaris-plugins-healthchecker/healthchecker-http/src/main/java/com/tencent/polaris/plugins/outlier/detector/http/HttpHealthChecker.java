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

package com.tencent.polaris.plugins.outlier.detector.http;

import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;


/**
 * HttpOutlierDetector.java
 *
 * @author andrewshan
 * @date 2019/9/19
 */
public class HttpHealthChecker implements HealthChecker, PluginConfigProvider  {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHealthChecker.class);

    private static final int EXPECT_CODE = 200;

    private Config config;

    @Override
    public DetectResult detectInstance(Instance instance) throws PolarisException {
        try {
            String pattern = config.getPath();

            String path = String.format("http://%s:%d%s", instance.getHost(), instance.getPort(), pattern);
            java.net.URL url = new java.net.URL(path);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(config.getTimeout().intValue());// 连接超时
            conn.setReadTimeout(config.getTimeout().intValue());// 读取超时

            if (conn.getResponseCode() == EXPECT_CODE) {
                return new DetectResult(RetStatus.RetSuccess);
            }
            return new DetectResult(RetStatus.RetFail);
        } catch (Exception e) {
            LOG.debug("http detect exception, service:{}, host:{}, port:{}, e:{}", instance.getService(),
                    instance.getHost(), instance.getPort(), e);
            return new DetectResult(RetStatus.RetFail);
        }
    }
    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return Config.class;
    }

    @Override
    public String getName() {
        return DefaultValues.DEFAULT_HEALTH_CHECKER_HTTP;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.HEALTH_CHECKER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        OutlierDetectionConfig outlierDetection = ctx.getConfig().getConsumer().getOutlierDetection();
        Config cfg = outlierDetection.getPluginConfig(getName(), Config.class);
        if (cfg == null) {
            throw new PolarisException(ErrorCode.INVALID_CONFIG,
                    String.format("plugin %s config is missing", getName()));
        }
        this.config = cfg;
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
    }

    @Override
    public void destroy() {

    }
}