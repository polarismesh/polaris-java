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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.ResultToErrorCode;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DefaultFunctionalDecorator implements FunctionalDecorator {

    private final FunctionalDecoratorRequest makeDecoratorRequest;

    private final CircuitBreakAPI circuitBreakAPI;

    public DefaultFunctionalDecorator(FunctionalDecoratorRequest makeDecoratorRequest,
            CircuitBreakAPI circuitBreakAPI) {
        this.makeDecoratorRequest = makeDecoratorRequest;
        this.circuitBreakAPI = circuitBreakAPI;
    }

    private CheckResult commonCheckFunction() {
        // check service
        Resource svcResource = new ServiceResource(makeDecoratorRequest.getService());
        CheckResult check = circuitBreakAPI.check(svcResource);
        if (!check.isPass()) {
            return check;
        }
        // check method
        if (StringUtils.isNotBlank(makeDecoratorRequest.getMethod())) {
            Resource methodResource = new MethodResource(makeDecoratorRequest.getService(),
                    makeDecoratorRequest.getMethod());
            check = circuitBreakAPI.check(methodResource);
            if (!check.isPass()) {
                return check;
            }
        }
        return null;
    }

    private void commonReport(int code, long delay, RetStatus retStatus) {
        // report service
        Resource svcResource = new ServiceResource(makeDecoratorRequest.getService());
        ResourceStat resourceStat = new ResourceStat(svcResource, code, delay, retStatus);
        circuitBreakAPI.report(resourceStat);
        // report method
        if (StringUtils.isNotBlank(makeDecoratorRequest.getMethod())) {
            Resource methodResource = new MethodResource(makeDecoratorRequest.getService(),
                    makeDecoratorRequest.getMethod());
            resourceStat = new ResourceStat(methodResource, code, delay, retStatus);
            circuitBreakAPI.report(resourceStat);
        }
    }

    @Override
    public <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        return new Supplier<T>() {
            @Override
            public T get() {
                CheckResult check = commonCheckFunction();
                if (null != check) {
                    throw new CallAbortedException(check.getRuleName(), check.getFallbackInfo());
                }
                ResultToErrorCode resultToErrorCode = makeDecoratorRequest.getResultToErrorCode();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    T result = supplier.get();
                    long endTimeMilli = System.currentTimeMillis();
                    int code = 0;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onSuccess(result);
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, RetStatus.RetSuccess);
                    return result;
                } catch (Throwable e) {
                    long endTimeMilli = System.currentTimeMillis();
                    int code = -1;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onError(e);
                    }
                    RetStatus retStatus = RetStatus.RetFail;
                    if (e instanceof CallAbortedException) {
                        retStatus = RetStatus.RetReject;
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, retStatus);
                    throw e;
                }
            }
        };
    }

    @Override
    public <T> Consumer<T> decorateConsumer(Consumer<T> consumer) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                CheckResult check = commonCheckFunction();
                if (null != check) {
                    throw new CallAbortedException(check.getRuleName(), check.getFallbackInfo());
                }
                ResultToErrorCode resultToErrorCode = makeDecoratorRequest.getResultToErrorCode();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    consumer.accept(t);
                    long endTimeMilli = System.currentTimeMillis();
                    int code = 0;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onSuccess(null);
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, RetStatus.RetSuccess);
                } catch (Throwable e) {
                    long endTimeMilli = System.currentTimeMillis();
                    int code = -1;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onError(e);
                    }
                    RetStatus retStatus = RetStatus.RetFail;
                    if (e instanceof CallAbortedException) {
                        retStatus = RetStatus.RetReject;
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, retStatus);
                    throw e;
                }
            }
        };
    }

    @Override
    public <T, R> Function<T, R> decorateFunction(Function<T, R> function) {
        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                CheckResult check = commonCheckFunction();
                if (null != check) {
                    throw new CallAbortedException(check.getRuleName(), check.getFallbackInfo());
                }
                ResultToErrorCode resultToErrorCode = makeDecoratorRequest.getResultToErrorCode();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    R result = function.apply(t);
                    long endTimeMilli = System.currentTimeMillis();
                    int code = 0;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onSuccess(result);
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, RetStatus.RetSuccess);
                    return result;
                } catch (Throwable e) {
                    long endTimeMilli = System.currentTimeMillis();
                    int code = -1;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onError(e);
                    }
                    RetStatus retStatus = RetStatus.RetFail;
                    if (e instanceof CallAbortedException) {
                        retStatus = RetStatus.RetReject;
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, retStatus);
                    throw e;
                }
            }
        };
    }

    @Override
    public <T> Predicate<T> decoratePredicate(Predicate<T> predicate) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                CheckResult check = commonCheckFunction();
                if (null != check) {
                    throw new CallAbortedException(check.getRuleName(), check.getFallbackInfo());
                }
                ResultToErrorCode resultToErrorCode = makeDecoratorRequest.getResultToErrorCode();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    boolean result = predicate.test(t);
                    long endTimeMilli = System.currentTimeMillis();
                    int code = 0;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onSuccess(result);
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, RetStatus.RetSuccess);
                    return result;
                } catch (Throwable e) {
                    long endTimeMilli = System.currentTimeMillis();
                    int code = -1;
                    if (null != resultToErrorCode) {
                        code = resultToErrorCode.onError(e);
                    }
                    RetStatus retStatus = RetStatus.RetFail;
                    if (e instanceof CallAbortedException) {
                        retStatus = RetStatus.RetReject;
                    }
                    long delay = endTimeMilli - startTimeMilli;
                    commonReport(code, delay, retStatus);
                    throw e;
                }
            }
        };
    }

}
