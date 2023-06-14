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

package com.tencent.polaris.plugins.configfilter.crypto.service;

import com.tencent.polaris.plugins.configfilter.crypto.util.RSAUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * @author fabian4
 * @date 2023/6/14
 */
public class RSAService {

    private final PublicKey publicKey;

    private final PrivateKey privateKey;

    public RSAService() {
        KeyPair keyPair = RSAUtil.generateRsaKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }

    public byte[] decrypt(String context) {
        return RSAUtil.decrypt(context.getBytes(), this.privateKey);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
