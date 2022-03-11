package com.tencent.polaris.configuration.client.internal;

/**
 * @author lepdou 2022-03-04
 */
public interface RetryPolicy {

    void success();

    void fail();

    long getCurrentDelayTime();

    void executeDelay();

}
