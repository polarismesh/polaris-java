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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpElement {


    public final class HttpMethod {

        public static final String GET = "GET";
        public static final String HEAD = "HEAD";
        public static final String POST = "POST";
        public static final String PUT = "PUT";
        public static final String PATCH = "PATCH";
        public static final String DELETE = "DELETE";
        public static final String OPTIONS = "OPTIONS";
        public static final String TRACE = "TRACE";
    }

    public static final Set<String> HTTP_METHOD_SET =
            new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT,
                    HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.TRACE));

    public final class MediaType {

        public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded;charset=UTF-8";
        public static final String APPLICATION_XHTML_XML = "application/xhtml+xml";
        public static final String APPLICATION_XML = "application/xml;charset=UTF-8";
        public static final String APPLICATION_JSON = "application/json;charset=UTF-8";
        public static final String MULTIPART_FORM_DATA = "multipart/form-data;charset=UTF-8";
        public static final String TEXT_HTML = "text/html;charset=UTF-8";
        public static final String TEXT_PLAIN = "text/plain;charset=UTF-8";
    }
}
