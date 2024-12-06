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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.StringUtils;
import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class RouteArgument {

    public static final String LABEL_KEY_METHOD = "$method";

    public static final String LABEL_KEY_HEADER = "$header.";

    public static final String LABEL_KEY_QUERY = "$query.";

    public static final String LABEL_KEY_COOKIE = "$cookie.";

    public static final String LABEL_KEY_PATH = "$path";

    public static final String LABEL_KEY_CALLER_IP = "$caller_ip";

    public enum ArgumentType {
        CUSTOM{
            @Override
            public String key(String v) {
                return v;
            }
        },

        METHOD {
            @Override
            public String key(String val) {
                return LABEL_KEY_METHOD;
            }
        },

        HEADER {
            @Override
            public String key(String val) {
                return LABEL_KEY_HEADER + val;
            }
        },

        QUERY {
            @Override
            public String key(String val) {
                return LABEL_KEY_QUERY + val;
            }
        },

        COOKIE {
            @Override
            public String key(String val) {
                return LABEL_KEY_COOKIE + val;
            }
        },

        PATH {
            @Override
            public String key(String val) {
                return LABEL_KEY_PATH;
            }
        },

        CALLER_IP {
            @Override
            public String key(String val) {
                return LABEL_KEY_CALLER_IP;
            }
        };

        public abstract String key(String val);
    }

    private final ArgumentType type;

    private final String key;

    private final String value;

    private RouteArgument(ArgumentType type, String key, String value) {
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

    public static RouteArgument buildCustom(String key, String value) {
        return new RouteArgument(ArgumentType.CUSTOM, StringUtils.defaultString(key), StringUtils.defaultString(value));
    }

    public static RouteArgument buildMethod(String method) {
        return new RouteArgument(ArgumentType.METHOD, "", StringUtils.defaultString(method));
    }

    public static RouteArgument buildHeader(String headerKey, String headerValue) {
        return new RouteArgument(ArgumentType.HEADER, StringUtils.defaultString(headerKey),
                StringUtils.defaultString(headerValue));
    }

    public static RouteArgument buildQuery(String queryKey, String queryValue) {
        return new RouteArgument(ArgumentType.QUERY, StringUtils.defaultString(queryKey),
                StringUtils.defaultString(queryValue));
    }

    public static RouteArgument buildCookie(String cookieKey, String cookieValue) {
        return new RouteArgument(ArgumentType.COOKIE, StringUtils.defaultString(cookieKey),
                StringUtils.defaultString(cookieValue));
    }

    public static RouteArgument buildPath(String path) {
        return new RouteArgument(ArgumentType.PATH, "", StringUtils.defaultString(path));
    }

    public static RouteArgument buildCallerIP(String callerIP) {
        return new RouteArgument(ArgumentType.CALLER_IP, "", StringUtils.defaultString(callerIP));
    }

    public static RouteArgument fromLabel(String labelKey, String labelValue) {
        labelKey = StringUtils.defaultString(labelKey);
        if (StringUtils.equals(labelKey, LABEL_KEY_METHOD)) {
            return buildMethod(labelValue);
        }
        if (StringUtils.equals(labelKey, LABEL_KEY_CALLER_IP)) {
            return buildCallerIP(labelValue);
        }
        if (labelKey.startsWith(LABEL_KEY_HEADER)) {
            return buildHeader(labelKey.substring(LABEL_KEY_HEADER.length()), labelValue);
        }
        if (labelKey.startsWith(LABEL_KEY_QUERY)) {
            return buildQuery(labelKey.substring(LABEL_KEY_QUERY.length()), labelValue);
        }
        if (labelKey.startsWith(LABEL_KEY_COOKIE)) {
            return buildCookie(labelKey.substring(LABEL_KEY_COOKIE.length()),
                    labelValue);
        }
        if (StringUtils.equals(labelKey, LABEL_KEY_PATH)) {
            return buildPath(labelValue);
        }
        return buildCustom(labelKey, labelValue);
    }

    public void toLabel(Map<String, String> labels) {
        switch (type) {
            case METHOD: {
                labels.put(LABEL_KEY_METHOD, value);
                break;
            }
            case CALLER_IP: {
                labels.put(LABEL_KEY_CALLER_IP, value);
                break;
            }
            case HEADER: {
                labels.put(LABEL_KEY_HEADER + key, value);
                break;
            }
            case QUERY: {
                labels.put(LABEL_KEY_QUERY + key, value);
                break;
            }
            case COOKIE: {
                labels.put(LABEL_KEY_COOKIE + key, value);
                break;
            }
            case PATH: {
                labels.put(LABEL_KEY_PATH, value);
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
        if (!(o instanceof RouteArgument)) {
            return false;
        }
        RouteArgument that = (RouteArgument) o;
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
        return "RouteArgument{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
