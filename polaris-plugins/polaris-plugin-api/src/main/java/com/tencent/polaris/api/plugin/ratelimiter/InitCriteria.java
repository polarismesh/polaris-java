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


import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;

public class InitCriteria {

    private String windowKey;

    private Rule rule;

    //规则中含有正则表达式扩散
    private boolean regexSpread;

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Rule getRule() {
        return rule;
    }

    public String getWindowKey() {
        return windowKey;
    }

    public void setWindowKey(String windowKey) {
        this.windowKey = windowKey;
    }

    public boolean isRegexSpread() {
        return regexSpread;
    }

    public void setRegexSpread(boolean regexSpread) {
        this.regexSpread = regexSpread;
    }
}
