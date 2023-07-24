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

package com.tencent.polaris.plugins.configfilefilter.crypto;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.filter.Crypto;
import com.tencent.polaris.plugins.configfilefilter.util.AESUtil;

/**
 * AES Crypto 加密
 *
 * @author fabian4
 * @date 2023/7/1
 */
public class AESCrypto implements Crypto {

    @Override
    public void doEncrypt(ConfigFile configFile) {
        throw new PolarisException(ErrorCode.NOT_SUPPORT);
    }

    @Override
    public void doDecrypt(ConfigFile configFile, byte[] password) {
        String result = AESUtil.decrypt(configFile.getContent(), password);
        configFile.setContent(result);
    }


    @Override
    public String getName() {
        return "AES";
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CRYPTO.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

    }

    @Override
    public void destroy() {

    }
}
