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

package com.tencent.polaris.plugins.connector.consul.service.common;

/**
 * TSF 标签常量
 *
 * @author hongweizhu
 */
public class TagConstant {

    /**
     * 标签类型
     *
     * @author hongweizhu
     */
    public static class TYPE {
        /**
         * 系统标签
         */
        public static final String SYSTEM = "S";
        /**
         * 用户自定义标签
         */
        public static final String CUSTOM = "U";
    }

    /**
     * 规则之间运算表达式的逻辑关系
     *
     * @author juanyinyang
     */
    public static class TagRuleRelation {
        /**
         * 与
         */
        public static final String AND = "AND";
        /**
         * 或
         */
        public static final String OR = "OR";
    }

    /**
     * 操作符
     *
     * @author hongweizhu
     */
    public static class OPERATOR {
        /**
         * 包含
         */
        public static final String IN = "IN";
        /**
         * 不包含
         */
        public static final String NOT_IN = "NOT_IN";
        /**
         * 等于
         */
        public static final String EQUAL = "EQUAL";
        /**
         * 不等于
         */
        public static final String NOT_EQUAL = "NOT_EQUAL";
        /**
         * 正则
         */
        public static final String REGEX = "REGEX";
    }

    /**
     * 系统标签名
     *
     * @author vanqfjiang
     */
    public static class SYSTEM_FIELD {
        /**
         * 请求发起方的应用 ID
         */
        public static final String SOURCE_APPLICATION_ID = "source.application.id";
        /**
         * 请求发起方的应用版本号
         */
        public static final String SOURCE_APPLICATION_VERSION = "source.application.version";
        /**
         * 请求发起方的实例 ID
         */
        public static final String SOURCE_INSTANCE_ID = "source.instance.id";
        /**
         * 请求发起方的部署组 ID
         */
        public static final String SOURCE_GROUP_ID = "source.group.id";
        /**
         * 请求发起方 IP
         */
        public static final String SOURCE_CONNECTION_IP = "source.connection.ip";
        /**
         * 请求发起方的服务名
         */
        public static final String SOURCE_SERVICE_NAME = "source.service.name";

        /**
         * 请求发起方的 Namespace/serviceName
         */
        public static final String SOURCE_NAMESPACE_SERVICE_NAME = "source.namespace.service.name";

        /**
         * 请求发起方的服务 token，鉴权模块使用
         */
        public static final String SOURCE_SERVICE_TOKEN = "source.service.token";
        /**
         * 请求发起方被它的上游调用的接口（如果有）
         */
        public static final String SOURCE_INTERFACE = "source.interface";
        /**
         * 请求接收方的服务名
         */
        public static final String DESTINATION_SERVICE_NAME = "destination.service.name";
        /**
         * 请求接收方被调用的接口
         */
        public static final String DESTINATION_INTERFACE = "destination.interface";

        /**
         * 请求接收方的应用
         */
        public static final String DESTINATION_APPLICATION_ID = "destination.application.id";
        /**
         * 请求接收方被调用的接口
         */
        public static final String DESTINATION_APPLICATION_VERSION = "destination.application.version";

        /**
         * 请求接收方的部署组 ID
         */
        public static final String DESTINATION_GROUP_ID = "destination.group.id";

        /**
         * 请求所使用的 HTTP 方法
         */
        public static final String REQUEST_HTTP_METHOD = "request.http.method";

    }
}
