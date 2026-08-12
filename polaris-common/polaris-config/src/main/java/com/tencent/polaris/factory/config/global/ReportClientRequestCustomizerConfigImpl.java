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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.factory.config.global;

import com.tencent.polaris.api.config.global.ReportClientRequestCustomizerConfig;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;

/**
 * Default implementation of {@link ReportClientRequestCustomizerConfig}.
 *
 * @author fishtailfu
 */
public class ReportClientRequestCustomizerConfigImpl extends PluginConfigImpl
        implements ReportClientRequestCustomizerConfig {

    @Override
    public void verify() {
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject != null) {
            setDefaultPluginConfig((ReportClientRequestCustomizerConfig) defaultObject);
        }
    }
}
