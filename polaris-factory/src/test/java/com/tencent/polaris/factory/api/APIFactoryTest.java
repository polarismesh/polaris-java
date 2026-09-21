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

package com.tencent.polaris.factory.api;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Test for {@link APIFactory}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class APIFactoryTest {

    /**
     * 测试目的：聚合 factory 不暴露按需加载的 SkillAPI
     * 测试场景：检查 APIFactory 公共方法
     * 验证内容：不存在 createSkillAPI 系列方法
     */
    @Test
    public void testDoesNotExposeSkillAPI() {
        // Act
        Method[] methods = APIFactory.class.getMethods();

        // Assert
        Assertions.assertThat(Arrays.stream(methods)
                        .map(Method::getName)
                        .filter(name -> name.startsWith("createSkillAPI")))
                .isEmpty();
    }
}
