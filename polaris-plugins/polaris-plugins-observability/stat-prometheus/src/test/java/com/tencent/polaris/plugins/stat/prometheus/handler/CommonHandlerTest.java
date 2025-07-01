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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CommonHandler}.
 *
 * @author Haotian Zhang
 */
public class CommonHandlerTest {

    @Test
    public void testConvertMethod_WhenMethodMatchesFirstPattern() {
        // 准备
        String originalMethod = "/api/v1/users";
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("/api/v1/.*"),
                Pattern.compile("/api/v2/.*")
        );

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("/api/v1/.*");
    }

    @Test
    public void testConvertMethod_WhenMethodMatchesSecondPattern() {
        // 准备
        String originalMethod = "/api/v2/orders";
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("/api/v1/.*"),
                Pattern.compile("/api/v2/.*")
        );

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("/api/v2/.*");
    }

    @Test
    public void testConvertMethod_WhenMethodDoesNotMatchAnyPattern() {
        // 准备
        String originalMethod = "/api/v3/products";
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("/api/v1/.*"),
                Pattern.compile("/api/v2/.*")
        );

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("/api/v3/products");
    }

    @Test
    public void testConvertMethod_WithEmptyPatternList() {
        // 准备
        String originalMethod = "/api/v1/users";
        List<Pattern> patterns = Collections.emptyList();

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("/api/v1/users");
    }

    @Test
    public void testConvertMethod_WithNullPatternList() {
        // 准备
        String originalMethod = "/api/v1/users";
        List<Pattern> patterns = null;

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("/api/v1/users");
    }

    @Test
    public void testConvertMethod_WithNullOriginalMethod() {
        // 准备
        String originalMethod = null;
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("/api/v1/.*"),
                Pattern.compile("/api/v2/.*")
        );

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isNull();
    }

    @Test
    public void testConvertMethod_WithComplexPatterns() {
        // 准备
        String originalMethod = "/user/12345/profile";
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("/user/\\d+/profile"),
                Pattern.compile("/product/\\d+/detail")
        );

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("/user/\\d+/profile");
    }

    @Test
    public void testConvertMethod_WithExactMatchPattern() {
        // 准备
        String originalMethod = "GET";
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile("GET"),
                Pattern.compile("POST"),
                Pattern.compile("PUT")
        );

        // 执行
        String result = CommonHandler.convertMethod(originalMethod, patterns);

        // 验证
        assertThat(result).isEqualTo("GET");
    }
}
