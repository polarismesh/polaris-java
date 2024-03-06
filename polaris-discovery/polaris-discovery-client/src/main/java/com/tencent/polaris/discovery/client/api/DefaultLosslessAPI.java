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

package com.tencent.polaris.discovery.client.api;

import com.tencent.polaris.api.core.LosslessAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.plugin.lossless.RegisterStatusProvider;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;

public class DefaultLosslessAPI extends BaseEngine implements LosslessAPI {


    public DefaultLosslessAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() throws PolarisException {

    }

    @Override
    public void setLosslessActionProvider(LosslessActionProvider losslessActionProvider) {

    }

    @Override
    public void losslessRegister() {

    }

    @Override
    public void setRegisterStatusProvider(RegisterStatusProvider registerStatusProvider) {

    }
}
