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

package com.tencent.polaris.certificate.client.core;

import com.tencent.polaris.api.plugin.certificate.CertFile;
import com.tencent.polaris.api.plugin.certificate.CertFileKey;
import com.tencent.polaris.certificate.api.core.CertificateAPI;
import com.tencent.polaris.certificate.api.flow.CertificateFlow;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;

import java.util.Map;

public class DefaultCertificateAPI extends BaseEngine implements CertificateAPI {

    private CertificateFlow certificateFlow;

    public DefaultCertificateAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        certificateFlow = sdkContext.getOrInitFlow(CertificateFlow.class);
    }

    @Override
    public Map<CertFileKey, CertFile> getPemFileMap() {
        checkAvailable("CertificateFlow");
        return certificateFlow.getPemFileMap();
    }

    @Override
    public void close() {
        certificateFlow.destroy();
    }
}
