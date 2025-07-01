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

package com.tencent.polaris.plugins.outlier.detector.http;

import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.HttpProtocolConfig;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.HttpProtocolConfig.MessageHeader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * HttpOutlierDetector.java
 *
 * @author andrewshan
 * @date 2019/9/19
 */
public class HttpHealthChecker implements HealthChecker, PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHealthChecker.class);

    private static final String DEFAULT_METHOD = "GET";

    private static final String DEFAULT_PATH = "/";

    private int timeoutMs = 0;

    private HttpProtocolConfig config;

    @Override
    public DetectResult detectInstance(Instance instance, FaultDetectRule faultDetectRule) throws PolarisException {
        int curTimeout = timeoutMs;
        HttpProtocolConfig curConfig = config;
        int curPort = instance.getPort();
        if (null != faultDetectRule && faultDetectRule.getProtocol() == Protocol.HTTP) {
            if (faultDetectRule.getTimeout() > 0) {
                curTimeout = faultDetectRule.getTimeout();
            }
            if (faultDetectRule.hasHttpConfig()) {
                curConfig = faultDetectRule.getHttpConfig();
            }
            if (faultDetectRule.getPort() > 0) {
                curPort = faultDetectRule.getPort();
            }
        }
        String method = curConfig.getMethod();
        String pathStr = curConfig.getUrl();
        List<MessageHeader> headersList = curConfig.getHeadersList();
        String body = curConfig.getBody();
        java.net.HttpURLConnection conn = null;
        OutputStream outputStream = null;
        long startTimeMillis = System.currentTimeMillis();
        String path = String.format("http://%s:%d%s", instance.getHost(), curPort, pathStr);
        try {
            java.net.URL url = new java.net.URL(path);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            if (headersList.size() > 0) {
                for (MessageHeader messageHeader : headersList) {
                    conn.addRequestProperty(messageHeader.getKey(), messageHeader.getValue());
                }
            }
            conn.setConnectTimeout(curTimeout);// 连接超时
            conn.setReadTimeout(curTimeout);// 读取超时
            if (StringUtils.isNotBlank(body)) {
                conn.setDoOutput(true);
                outputStream = conn.getOutputStream();
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }
            int responseCode = conn.getResponseCode();
            long delayMillis = System.currentTimeMillis() - startTimeMillis;

            RetStatus retStatus = responseCode >= 200 && responseCode < 500 ? RetStatus.RetSuccess : RetStatus.RetFail;
            return new DetectResult(responseCode, delayMillis, retStatus);
        } catch (Exception e) {
            LOG.warn("http detect exception, host:{}, port:{}, error {}", instance.getHost(), instance.getPort(),
                    e.getMessage());
            long delayMillis = System.currentTimeMillis() - startTimeMillis;
            return new DetectResult(-1, delayMillis, RetStatus.RetFail);
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException ioException) {
                    LOG.error("fail to close output stream for url {}", path, ioException);
                }
            }
            if (null != conn) {
                conn.disconnect();
            }
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
        HttpProtocolConfig.Builder builder = HttpProtocolConfig.newBuilder();
        if (null == cfg || StringUtils.isBlank(cfg.getPath())) {
            builder.setUrl(DEFAULT_PATH);
        }
        if (null != cfg && null != cfg.getTimeout() && cfg.getTimeout() > 0) {
            timeoutMs = (int) cfg.getTimeout().longValue();
        } else {
            timeoutMs = DEFAULT_TIMEOUT_MILLI;
        }
        builder.setMethod(DEFAULT_METHOD);
        this.config = builder.build();
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
    }

    @Override
    public void destroy() {

    }
}