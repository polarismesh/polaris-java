package com.tencent.polaris.circuitbreak.api.pojo;

import com.tencent.polaris.api.pojo.ServiceKey;

import java.util.concurrent.TimeUnit;

/**
 * request invoke context for {@code CircuitBreakAPI}
 */
public class InvokeContext {

    private ServiceKey sourceService;

    private ServiceKey service;

    private String method;

    private ResultToErrorCode resultToErrorCode;

    private long duration;

    private TimeUnit durationUnit;

    private Object result;

    private Throwable error;

    public InvokeContext(FunctionalDecoratorRequest functionalDecoratorRequest) {
        this.sourceService = functionalDecoratorRequest.getSourceService();
        this.service = functionalDecoratorRequest.getService();
        this.method = functionalDecoratorRequest.getMethod();
        this.resultToErrorCode = functionalDecoratorRequest.getResultToErrorCode();
    }

    public InvokeContext(FunctionalDecoratorRequest functionalDecoratorRequest, long duration, TimeUnit durationUnit, Object result, Throwable error) {
        this(functionalDecoratorRequest);
        this.duration = duration;
        this.durationUnit = durationUnit;
        this.result = result;
        this.error = error;
    }

    public InvokeContext() {

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
