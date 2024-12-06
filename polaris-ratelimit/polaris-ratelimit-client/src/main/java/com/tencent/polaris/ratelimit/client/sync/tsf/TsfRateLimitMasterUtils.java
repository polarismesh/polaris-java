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

package com.tencent.polaris.ratelimit.client.sync.tsf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.polaris.client.util.EscapeNonAsciiWriter;
import com.tencent.polaris.logging.LoggerFactory;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TSF 限流中控上报工具.
 *
 * @author Haotian Zhang
 */
public class TsfRateLimitMasterUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TsfRateLimitMasterUtils.class);

    private static final CloseableHttpClient httpClient;

    private static URI uri = null;

    static {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(500).setConnectionRequestTimeout(2000)
                .setSocketTimeout(1000).build();
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public static void setUri(String rateLimitMasterIp, String rateLimitMasterPort, String serviceName, String instanceId, String token) {
        try {
            int port = Integer.parseInt(rateLimitMasterPort);
            uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(rateLimitMasterIp)
                    .setPort(port)
                    .setPath(String.format("/sync/%s/%s", URLEncoder.encode(serviceName, "UTF-8"), URLEncoder.encode(instanceId, "UTF-8")))
                    .setParameter("token", token)
                    .build();
        } catch (URISyntaxException | UnsupportedEncodingException | NumberFormatException e) {
            LOG.error("build rate limit request url fail.", e);
        }
    }

    public static Map<String, Integer> report(String ruleId, long pass, long block) {
        if (uri == null) {
            LOG.warn("url is not set.");
        }
        List<ReportRequest.RuleStatics> ruleStaticsList = new ArrayList<>();
        ruleStaticsList.add(new ReportRequest.RuleStatics(ruleId, pass, block));
        ReportRequest request = new ReportRequest(ruleStaticsList);
        List<String> staticsIds = new ArrayList<>();
        for (ReportRequest.RuleStatics ruleStatic : ruleStaticsList) {
            staticsIds.add(ruleStatic.ruleId);
        }
        LOG.debug("[TSF Ratelimit] Report request uri {}, body {}", uri, request);
        Map<String, Integer> ruleQuotaMap = new HashMap<>();
        try {
            HttpPost httpPost = new HttpPost(uri);

            StringEntity postBody = new StringEntity(serializeToJson(request));

            httpPost.setEntity(postBody);
            httpPost.setHeader("Content-Type", "application/json");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            Header encodingHeader = entity.getContentEncoding();
            Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8
                    : Charsets.toCharset(encodingHeader.getValue());
            String responseBody = EntityUtils.toString(entity, encoding);

            ReportResponse response = deserializeTagList(responseBody);

            List<ReportResponse.Limit> limits = response.getLimits();
            if (limits == null || limits.isEmpty()) {
                LOG.warn("[TSF Ratelimit] Master returns no limit rules");
                return ruleQuotaMap;
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("[TSF Ratelimit] Report response uri {}, body {}", uri, response);
            }
            for (ReportResponse.Limit limit : limits) {
                if (limit.getQuota() <= 0 || !staticsIds.contains(limit.getRuleId())) {
                    // 多条(超过1条）生效限流规则时，直接删除生效时，sdk 会一直打下面日志。 ratelimit master 可能会存在更新问题，导致返回的 limit 有多的，sdk 忽略
                    // 其他临时规避方法:
                    // 1. 当前已有该日志的应用，可以任意修改当前生效的限流规则，触发规则更新后不再打印 warn 日志
                    // 2. 如果要删除限流规则，可以直接把生效状态关闭，再删除
                    LOG.debug("[TSF Ratelimit] quota for {} is {}, skipped", limit.getRuleId(), limit.getQuota());
                    continue;
                }
                ruleQuotaMap.put(limit.getRuleId(), limit.getQuota());
            }
        } catch (Exception e) {
            LOG.warn("[TSF Ratelimit] Report to master failed.", e);
        }
        return ruleQuotaMap;
    }

    public static <T> String serializeToJson(T object) {
        StringWriter writer = new StringWriter();
        EscapeNonAsciiWriter escapeWriter = new EscapeNonAsciiWriter(writer);

        Gson gson = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation().create();
        gson.toJson(object, escapeWriter);
        return writer.toString();
    }

    public static ReportResponse deserializeTagList(String buffer) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.fromJson(buffer, ReportResponse.class);
    }
}
