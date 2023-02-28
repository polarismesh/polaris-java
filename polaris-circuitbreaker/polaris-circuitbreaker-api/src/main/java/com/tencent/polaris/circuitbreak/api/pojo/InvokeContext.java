package com.tencent.polaris.circuitbreak.api.pojo;

import com.tencent.polaris.api.pojo.ServiceKey;

import java.util.concurrent.TimeUnit;

/**
 * request invoke context for {@code InvokeHandler}
 */
public class InvokeContext {

    public static class RequestContext {

        private ServiceKey sourceService;

        private ServiceKey service;

        private String method;

        private ResultToErrorCode resultToErrorCode;

        public RequestContext(){

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

        public void setService(ServiceKey service) {
            this.service = service;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
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
