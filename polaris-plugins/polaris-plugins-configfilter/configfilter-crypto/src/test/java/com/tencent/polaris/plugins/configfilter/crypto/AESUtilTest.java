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

package com.tencent.polaris.plugins.configfilter.crypto;

import com.tencent.polaris.plugins.configfilter.crypto.util.AESUtil;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author fabian4
 * @date 2023/6/14
 */
public class AESUtilTest {

    @Test
    public void testAes() {
        byte[] aesKey = AESUtil.generateAesKey();
        byte[] content = "test content".getBytes();
        byte[] encrypted = AESUtil.encrypt(content, aesKey);
        byte[] decrypted = AESUtil.decrypt(encrypted, aesKey);
        assertArrayEquals(content, decrypted);
    }
}
