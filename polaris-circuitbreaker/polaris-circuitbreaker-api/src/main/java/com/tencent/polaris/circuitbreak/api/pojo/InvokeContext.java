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

package com.tencent.polaris.circuitbreak.api.pojo;

import com.tencent.polaris.api.pojo.ServiceKey;

import java.util.concurrent.TimeUnit;

/**
 * request invoke context for {@code InvokeHandler}
 */
public class InvokeContext {

    public static class RequestContext {

        private ServiceKey sourceService;

        private final ServiceKey service;

        private final String protocol;

        private final String method;

        private final String path;

        private ResultToErrorCode resultToErrorCode;

        public RequestContext(ServiceKey service, String protocol, String method, String path) {
            this.service = service;
            this.protocol = protocol;
            this.method = method;
            this.path = path;
        }

        public ServiceKey getSourceService() {
            return sourceService;
        }

        public void setSourceService(ServiceKey sourceService) {
            this.sourceService = sourceService;
        }

        public ServiceKey getService() {
            return service;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public ResultToErrorCode getResultToErrorCode() {
            return resultToErrorCode;
        }

        public void setResultToErrorCode(ResultToErrorCode resultToErrorCode) {
            this.resultToErrorCode = resultToErrorCode;
        }

    }

    public static class ResponseContext {

        private long duration;

        private TimeUnit durationUnit;

        private Object result;

        private Throwable error;

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public TimeUnit getDurationUnit() {
            return durationUnit;
        }

        public void setDurationUnit(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public Throwable getError() {
            return error;
        }

        public void setError(Throwable error) {
            this.error = error;
        }

    }

}
