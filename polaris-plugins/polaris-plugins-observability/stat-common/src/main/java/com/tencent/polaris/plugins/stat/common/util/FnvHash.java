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

package com.tencent.polaris.plugins.stat.common.util;

import java.nio.charset.StandardCharsets;

public class FnvHash {
    static final long offset64 = 0xcbf29ce484222325L; //14695981039346656037
    static final long prime64 = 1099511628211L;

    static long hashNew() {
        return offset64;
    }

    static long hashAdd(long h, String s) {
        byte[] bArray = s.getBytes(StandardCharsets.UTF_8);
        for (byte b : bArray) {
            h = hashAddByte(h, b);
        }
        return h;
    }

    static long hashAddByte(long h, byte b) {
        h ^= b;
        h *= prime64;
        return h;
    }
}
