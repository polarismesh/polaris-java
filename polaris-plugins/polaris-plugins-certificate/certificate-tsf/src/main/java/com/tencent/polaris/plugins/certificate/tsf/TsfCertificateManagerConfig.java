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

package com.tencent.polaris.plugins.certificate.tsf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author Haotian Zhang
 */
public class TsfCertificateManagerConfig implements Verifier {

    @JsonProperty
    private String address;

    @JsonProperty
    private String certPath;

    @JsonProperty
    private String token;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(address, "global.certificate.plugin.tsfCertificateManager.address");
        ConfigUtils.validateString(token, "global.certificate.plugin.tsfCertificateManager.token");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof TsfCertificateManagerConfig) {
            TsfCertificateManagerConfig tsfCertificateManagerConfig = (TsfCertificateManagerConfig) defaultObject;
            if (StringUtils.isBlank(address)) {
                setAddress(tsfCertificateManagerConfig.getAddress());
            }
            if (StringUtils.isBlank(certPath)) {
                setCertPath(tsfCertificateManagerConfig.getCertPath());
            }
            if (StringUtils.isBlank(token)) {
                setToken(tsfCertificateManagerConfig.getToken());
            }
        }
    }
}
