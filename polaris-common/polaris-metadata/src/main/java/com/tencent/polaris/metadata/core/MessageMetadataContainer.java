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

package com.tencent.polaris.metadata.core;

import java.util.Map;

public interface MessageMetadataContainer extends MetadataContainer {

    String LABEL_KEY_METHOD = "$method";

    String LABEL_MAP_KEY_HEADER = "$header.";

    String LABEL_MAP_KEY_QUERY = "$query.";

    String LABEL_MAP_KEY_COOKIE = "$cookie.";

    String LABEL_KEY_PATH = "$path";

    String LABEL_KEY_CALLER_IP = "$caller_ip";

    /**
     * 获取方法值
     * @return 方法值
     */
    String getMethod();

    /**
     * 设置方法值
     * @param method 方法
     */
    void setMethod(String method);

    /**
     * 根据header名称获取header值
     * @param headerName header名称
     * @return header值
     */
    String getHeader(String headerName);

    /**
     * 设置header的值，根据header名称和header值进行设置
     * @param headerName header名称
     * @param headerValue header值
     * @param transitiveType 透传类型
     */
    void setHeader(String headerName, String headerValue, TransitiveType transitiveType);

    /**
     * 根据query的名称获取值
     * @param queryName query名称
     * @return 值
     */
    String getQuery(String queryName);

    /**
     * 设置query的值，根据query名称和query值进行设置
     * @param queryName header名称
     * @param queryValue header值
     * @param transitiveType 透传类型
     */
    void setQuery(String queryName, String queryValue, TransitiveType transitiveType);

    /**
     * 根据cookie的名称，获取cookie的值
     * @param cookieName cookie名称
     * @return cookie值
     */
    String getCookie(String cookieName);

    /**
     * 设置cookie值
     * @param cookieName cookie名称
     * @param cookieValue cookie值
     * @param transitiveType 透传类型
     */
    void setCookie(String cookieName, String cookieValue, TransitiveType transitiveType);

    /**
     * 获取方法路径
     * @return 路径
     */
    String getPath();

    /**
     * 设置方法路径
     * @param path 方法路径
     */
    void setPath(String path);

    /**
     * 获取调用者IP
     * @return 调用者IP
     */
    String getCallerIP();

    /**
     * 设置调用者IP
     * @param callerIP 调用者IP
     */
    void setCallerIP(String callerIP);

    /**
     * 获取所有需透传的header键值对
     * @return header键值对
     */
    Map<String, String> getTransitiveHeaders();

    /**
     * 获取所有需透传的query键值对
     * @return query键值对
     */
    Map<String, String> getTransitiveQueries();

    /**
     * 获取所有需透传的cookie键值对
     * @return cookie键值对
     */
    Map<String, String> getTransitiveCookies();

}
