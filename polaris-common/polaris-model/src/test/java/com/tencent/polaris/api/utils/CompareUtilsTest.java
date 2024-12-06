/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.api.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CompareUtils}.
 *
 * @author Haotian Zhang
 */
public class CompareUtilsTest {

    @Test
    public void testIsWildcardMatcherSingle() {
        assertThat(CompareUtils.isWildcardMatcherSingle("*")).isTrue();
        assertThat(CompareUtils.isWildcardMatcherSingle("")).isTrue();
        assertThat(CompareUtils.isWildcardMatcherSingle("abc")).isFalse();
    }

    @Test
    public void testCompareSingleValue() {
        assertThat(CompareUtils.compareSingleValue("*", "abc") > 0).isTrue();
        assertThat(CompareUtils.compareSingleValue("*", "*") == 0).isTrue();
        assertThat(CompareUtils.compareSingleValue("abc", "*") < 0).isTrue();
        assertThat(CompareUtils.compareSingleValue("abc", "def") < 0).isTrue();
    }

    @Test
    public void testCompareService() {
        assertThat(CompareUtils.compareService("*", "*", "*", "*") == 0).isTrue();
        assertThat(CompareUtils.compareService("*", "abc", "*", "*") < 0).isTrue();
        assertThat(CompareUtils.compareService("*", "abc", "abc", "*") > 0).isTrue();
        assertThat(CompareUtils.compareService("abc", "abc", "abc", "*") < 0).isTrue();
        assertThat(CompareUtils.compareService("abc", "abc", "abc", "def") < 0).isTrue();
        assertThat(CompareUtils.compareService("abc", "*", "abc", "def") > 0).isTrue();
        assertThat(CompareUtils.compareService("abc", "*", "*", "def") < 0).isTrue();
    }
}
