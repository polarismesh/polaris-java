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

package com.tencent.polaris.encrypt;

import com.tencent.polaris.api.utils.ClassUtils;

/**
 * Shared encrypt constants that must stay loadable without BouncyCastle.
 *
 * <p>Do not put these on {@code AESUtil}: its static initializer instantiates
 * {@code BouncyCastleProvider}, so a presence check against AESUtil would throw
 * {@code NoClassDefFoundError} before the check runs.
 *
 * @author evelynwei
 */
public class EncryptConstants {

    /**
     * Symmetric algorithm name written into encryptAlgo and used by JCE AES APIs.
     */
    public static final String ALGO_AES = "AES";

    /**
     * BouncyCastle Provider class name for presence checks before loading AESUtil or RSAUtil.
     */
    public static final String BOUNCY_CASTLE_PROVIDER = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    /**
     * Whether the optional BouncyCastle provider is on the classpath.
     *
     * @return true if the provider class can be loaded
     */
    public static boolean isBouncyCastlePresent() {
        return ClassUtils.isClassPresent(BOUNCY_CASTLE_PROVIDER);
    }
}
