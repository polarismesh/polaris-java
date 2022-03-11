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

package com.tencent.polaris.factory.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Test for {@link ConfigUtils}
 *
 * @author Haotian Zhang
 */
public class ConfigUtilsTest {

    /**
     * Test for {@link ConfigUtils#validateAllNull(Map)}
     */
    @Test
    public void test1() {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("name1", null);
        try {
            ConfigUtils.validateAllNull(valueMap);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("name1 must not be all null.");
        }

        valueMap.put("name2", new Object());
        try {
            ConfigUtils.validateAllNull(valueMap);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not be thrown.");
        }
    }
}