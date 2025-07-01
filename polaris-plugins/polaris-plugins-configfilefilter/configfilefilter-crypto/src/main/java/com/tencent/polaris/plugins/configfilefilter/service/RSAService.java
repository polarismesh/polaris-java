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

package com.tencent.polaris.plugins.configfilefilter.service;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.encrypt.util.RSAUtil;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

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
        return RSAUtil.decrypt(Base64.getDecoder().decode(context), this.privateKey);
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public String getPKCS1PublicKey() {
        SubjectPublicKeyInfo spkInfo = SubjectPublicKeyInfo.getInstance(this.publicKey.getEncoded());
        ASN1Primitive primitive;
        try {
            primitive = spkInfo.parsePublicKey();
            byte[] publicKeyPKCS1 = primitive.getEncoded();
            return Base64.getEncoder().encodeToString(publicKeyPKCS1);
        } catch (IOException e) {
            throw new PolarisException(ErrorCode.RSA_KEY_GENERATE_ERROR, e.getMessage());
        }
    }

}
