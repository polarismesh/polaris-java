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

package com.tencent.polaris.metadata.core.impl;

import java.util.Map;

import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.TransitiveType;

public class MessageMetadataContainerImpl extends MetadataContainerImpl implements MessageMetadataContainer {

    public MessageMetadataContainerImpl(String transitivePrefix, boolean keyCaseSensitive) {
        super(transitivePrefix, keyCaseSensitive);
    }

    @Override
    public String getMethod() {
        return getRawMetadataStringValue(LABEL_KEY_METHOD);
    }

    @Override
    public void setMethod(String method) {
        putMetadataStringValue(LABEL_KEY_METHOD, method, TransitiveType.NONE);
    }

    @Override
    public String getHeader(String headerName) {
        return getRawMetadataMapValue(LABEL_MAP_KEY_HEADER, headerName);
    }

    @Override
    public void setHeader(String headerName, String headerValue, TransitiveType transitiveType) {
        putMetadataMapValue(LABEL_MAP_KEY_HEADER, headerName, headerValue, transitiveType);
    }

    @Override
    public String getQuery(String queryName) {
        return getRawMetadataMapValue(LABEL_MAP_KEY_QUERY, queryName);
    }

    @Override
    public void setQuery(String queryName, String queryValue, TransitiveType transitiveType) {
        putMetadataMapValue(LABEL_MAP_KEY_QUERY, queryName, queryValue, transitiveType);
    }

    @Override
    public String getCookie(String cookieName) {
        return getRawMetadataMapValue(LABEL_MAP_KEY_COOKIE, cookieName);
    }

    @Override
    public void setCookie(String cookieName, String cookieValue, TransitiveType transitiveType) {
        putMetadataMapValue(LABEL_MAP_KEY_COOKIE, cookieName, cookieValue, transitiveType);
    }

    @Override
    public String getPath() {
        return getRawMetadataStringValue(LABEL_KEY_PATH);
    }

    @Override
    public void setPath(String path) {
        putMetadataStringValue(LABEL_KEY_PATH, path, TransitiveType.NONE);
    }

    @Override
    public String getCallerIP() {
        return getRawMetadataStringValue(LABEL_KEY_CALLER_IP);
    }

    @Override
    public void setCallerIP(String callerIP) {
        putMetadataStringValue(LABEL_KEY_CALLER_IP, callerIP, TransitiveType.NONE);
    }

    @Override
    public Map<String, String> getTransitiveHeaders() {
        return getMapTransitiveStringValues(LABEL_MAP_KEY_HEADER);
    }

    @Override
    public Map<String, String> getTransitiveQueries() {
        return getMapTransitiveStringValues(LABEL_MAP_KEY_QUERY);
    }

    @Override
    public Map<String, String> getTransitiveCookies() {
        return getMapTransitiveStringValues(LABEL_MAP_KEY_COOKIE);
    }
}
