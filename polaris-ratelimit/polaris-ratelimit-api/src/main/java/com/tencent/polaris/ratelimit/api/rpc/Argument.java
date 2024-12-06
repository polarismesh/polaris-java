/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.ratelimit.api.rpc;

import com.tencent.polaris.api.utils.StringUtils;
import java.util.Map;
import java.util.Objects;

public class Argument {

    public enum ArgumentType {
        CUSTOM, METHOD, HEADER, QUERY, CALLER_SERVICE, CALLER_IP
    }

    private final ArgumentType type;

    private final String key;

    private final String value;

    private Argument(ArgumentType type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public ArgumentType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static Argument buildCustom(String key, String value) {
        return new Argument(ArgumentType.CUSTOM, StringUtils.defaultString(key), StringUtils.defaultString(value));
    }

    public static Argument buildMethod(String method) {
        return new Argument(ArgumentType.METHOD, "", StringUtils.defaultString(method));
    }

    public static Argument buildHeader(String headerKey, String headerValue) {
        return new Argument(ArgumentType.HEADER, StringUtils.defaultString(headerKey),
                StringUtils.defaultString(headerValue));
    }

    public static Argument buildQuery(String queryKey, String queryValue) {
        return new Argument(ArgumentType.QUERY, StringUtils.defaultString(queryKey),
                StringUtils.defaultString(queryValue));
    }

    public static Argument buildCallerService(String namespace, String service) {
        return new Argument(ArgumentType.CALLER_SERVICE, StringUtils.defaultString(namespace),
                StringUtils.defaultString(service));
    }

    public static Argument buildCallerIP(String callerIP) {
        return new Argument(ArgumentType.CALLER_IP, "", StringUtils.defaultString(callerIP));
    }

    public static Argument fromLabel(String labelKey, String labelValue) {
        labelKey = StringUtils.defaultString(labelKey);
        if (StringUtils.equals(labelKey, RateLimitConsts.LABEL_KEY_METHOD)) {
            return buildMethod(labelValue);
        }
        if (StringUtils.equals(labelKey, RateLimitConsts.LABEL_KEY_CALLER_IP)) {
            return buildCallerIP(labelValue);
        }
        if (labelKey.startsWith(RateLimitConsts.LABEL_KEY_HEADER)) {
            return buildHeader(labelKey.substring(RateLimitConsts.LABEL_KEY_HEADER.length()), labelValue);
        }
        if (labelKey.startsWith(RateLimitConsts.LABEL_KEY_QUERY)) {
            return buildQuery(labelKey.substring(RateLimitConsts.LABEL_KEY_QUERY.length()), labelValue);
        }
        if (labelKey.startsWith(RateLimitConsts.LABEL_KEY_CALLER_SERVICE)) {
            return buildCallerService(labelKey.substring(RateLimitConsts.LABEL_KEY_CALLER_SERVICE.length()),
                    labelValue);
        }
        return buildCustom(labelKey, labelValue);
    }

    public void toLabel(Map<String, String> labels) {
        switch (type) {
            case METHOD: {
                labels.put(RateLimitConsts.LABEL_KEY_METHOD, value);
                break;
            }
            case CALLER_IP: {
                labels.put(RateLimitConsts.LABEL_KEY_CALLER_IP, value);
                break;
            }
            case HEADER: {
                labels.put(RateLimitConsts.LABEL_KEY_HEADER + key, value);
                break;
            }
            case QUERY: {
                labels.put(RateLimitConsts.LABEL_KEY_QUERY + key, value);
                break;
            }
            case CALLER_SERVICE: {
                labels.put(RateLimitConsts.LABEL_KEY_CALLER_SERVICE + key, value);
                break;
            }
            case CUSTOM: {
                labels.put(key, value);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Argument)) {
            return false;
        }
        Argument that = (Argument) o;
        return type == that.type &&
                Objects.equals(key, that.key) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, value);
    }

    @Override
    public String toString() {
        return "MatchArgument{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }


}
