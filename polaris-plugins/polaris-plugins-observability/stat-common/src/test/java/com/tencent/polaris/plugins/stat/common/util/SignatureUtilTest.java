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

package com.tencent.polaris.plugins.stat.common.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Test for {@link SignatureUtil}.
 *
 * @author fuyuwei01
 */
public class SignatureUtilTest {

    private static final long EXPECTED_SIGNATURE = 4782059858504325644L;

    private static final long EMPTY_SIGNATURE = -3750763034362895579L;

    private static final long UTF8_SIGNATURE = -7676803588853562296L;

    /**
     * 验证相同 labels 重复计算时签名一致.
     */
    @Test
    public void testStableSignature() {
        Map<String, String> labels = buildLabels();
        long signature = SignatureUtil.labelsToSignature(labels);

        Assertions.assertThat(SignatureUtil.labelsToSignature(labels)).isEqualTo(signature);
        Assertions.assertThat(labels).containsExactlyEntriesOf(buildLabels());
    }

    /**
     * 验证相同标签以不同顺序插入时签名一致.
     */
    @Test
    public void testInsertionOrderDoesNotChangeSignature() {
        Map<String, String> labels = buildLabels();
        Map<String, String> reversedLabels = new LinkedHashMap<>();
        reversedLabels.put("namespace", "production");
        reversedLabels.put("service", "checkout");

        Assertions.assertThat(SignatureUtil.labelsToSignature(reversedLabels))
                .isEqualTo(SignatureUtil.labelsToSignature(labels));
    }

    /**
     * 验证固定标签样本的签名，防止计算方式意外改变.
     */
    @Test
    public void testKnownSignature() {
        Assertions.assertThat(SignatureUtil.labelsToSignature(buildLabels())).isEqualTo(EXPECTED_SIGNATURE);
    }

    /**
     * 验证空标签使用固定的空签名.
     */
    @Test
    public void testEmptyLabels() {
        Assertions.assertThat(SignatureUtil.labelsToSignature(Collections.emptyMap()))
                .isEqualTo(EMPTY_SIGNATURE);
    }

    /**
     * 验证 null 标签与空标签行为一致.
     */
    @Test
    public void testNullLabels() {
        Assertions.assertThat(SignatureUtil.labelsToSignature(null))
                .isEqualTo(SignatureUtil.labelsToSignature(Collections.emptyMap()));
    }

    /**
     * 验证固定样本修改标签值后签名不同.
     */
    @Test
    public void testChangedLabelValue() {
        Map<String, String> changedLabels = buildLabels();
        changedLabels.put("service", "payment");

        Assertions.assertThat(SignatureUtil.labelsToSignature(changedLabels))
                .isNotEqualTo(SignatureUtil.labelsToSignature(buildLabels()));
    }

    /**
     * 验证固定样本修改标签名称后签名不同.
     */
    @Test
    public void testChangedLabelName() {
        Map<String, String> changedLabels = new LinkedHashMap<>();
        changedLabels.put("application", "checkout");
        changedLabels.put("namespace", "production");

        Assertions.assertThat(SignatureUtil.labelsToSignature(changedLabels))
                .isNotEqualTo(SignatureUtil.labelsToSignature(buildLabels()));
    }

    /**
     * 验证键值分隔符区分拼接字符串相同的不同标签.
     */
    @Test
    public void testKeyValueBoundary() {
        Assertions.assertThat(SignatureUtil.labelsToSignature(Collections.singletonMap("ab", "c")))
                .isNotEqualTo(SignatureUtil.labelsToSignature(Collections.singletonMap("a", "bc")));
    }

    /**
     * 验证 UTF-8 标签的固定签名.
     */
    @Test
    public void testUtf8Labels() {
        Assertions.assertThat(SignatureUtil.labelsToSignature(
                Collections.singletonMap("service", "\u7ed3\u7b97\u670d\u52a1"))).isEqualTo(UTF8_SIGNATURE);
    }

    private Map<String, String> buildLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("service", "checkout");
        labels.put("namespace", "production");
        return labels;
    }
}
