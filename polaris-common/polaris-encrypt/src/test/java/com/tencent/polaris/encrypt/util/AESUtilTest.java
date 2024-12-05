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

package com.tencent.polaris.encrypt.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link AESUtil}.
 *
 * @author fabian4, Haotian Zhang
 */
public class AESUtilTest {

    @Test
    public void testAes() {
        byte[] aesKey = AESUtil.generateAesKey();
        String content = "test content";
        String encrypted = AESUtil.encrypt(content, aesKey);
        String decrypted = AESUtil.decrypt(encrypted, aesKey);
        assertEquals(content, decrypted);
    }

    @Test
    public void testAesECB() {
        String content = "test content";
        String password = "test password";
        String encrypted = AESUtil.encrypt(content, password);
        String decrypted = AESUtil.decrypt(encrypted, password);
        assertEquals(content, decrypted);
    }
}
