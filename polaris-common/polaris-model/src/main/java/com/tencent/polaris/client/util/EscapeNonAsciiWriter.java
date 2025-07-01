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

package com.tencent.polaris.client.util;

import java.io.IOException;
import java.io.Writer;

/**
 * 测试过这个类，对中文有效，对处于 Plane 1 的字符（如 𐀀）也有效
 *
 * @author onlyicelin
 */
public class EscapeNonAsciiWriter extends Writer {
    private final Writer out;

    public EscapeNonAsciiWriter(Writer out) {
        this.out = out;
    }

    @Override
    public void write(char[] buffer, int offset, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            char c = buffer[i + offset];
            if (c <= 0x7f) {
                out.write(c);
            } else {
                out.write(String.format("\\u%04x", (int) c));
            }
        }
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
