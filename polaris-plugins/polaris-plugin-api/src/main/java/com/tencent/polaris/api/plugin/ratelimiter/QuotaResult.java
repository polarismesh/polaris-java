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

package com.tencent.polaris.api.plugin.ratelimiter;

public class QuotaResult {

    public enum Code {
        /**
         * OK，代表请求可以通过
         */
        QuotaResultOk,
        /**
         * Limited，代表本次请求被限流
         */
        QuotaResultLimited
    }

    private final Code code;

    private final long waitMs;

    private final String info;

    private Runnable release;

    public QuotaResult(Code code, long waitMs, String info) {
        this(code, waitMs, info, null);
    }

    public QuotaResult(Code code, long waitMs, String info, Runnable release) {
        this.code = code;
        this.waitMs = waitMs;
        this.info = info;
        this.release = release;
    }

    public Code getCode() {
        return code;
    }

    public long getWaitMs() {
        return waitMs;
    }

    public String getInfo() {
        return info;
    }

    public Runnable getRelease() {
        return release;
    }

    public void setRelease(Runnable release) {
        this.release = release;
    }
}
