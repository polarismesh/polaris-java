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

package com.tencent.polaris.ai.client.utils;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link SkillValidator}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillValidatorTest {

    /**
     * 测试目的：合法 GetSkill 请求通过校验
     * 测试场景：name 与 namespace 均非空
     * 验证内容：不抛异常
     */
    @Test
    public void testValidateGetRequestSuccess() {
        // Arrange
        SkillGetRequest request = new SkillGetRequest();
        request.setName("sql-analysis");
        request.setNamespace("default");

        // Act & Assert
        assertThatCode(() -> SkillValidator.validateGetRequest(request)).doesNotThrowAnyException();
    }

    /**
     * 测试目的：空 GetSkill 请求被拒绝
     * 测试场景：request 为 null
     * 验证内容：API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateGetRequestNull() {
        // Act & Assert
        assertThatThrownBy(() -> SkillValidator.validateGetRequest(null))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.API_INVALID_ARGUMENT);
    }

    /**
     * 测试目的：缺 name 的 GetSkill 请求被拒绝
     * 测试场景：name 为空
     * 验证内容：API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateGetRequestBlankName() {
        // Arrange
        SkillGetRequest request = new SkillGetRequest();
        request.setName("");
        request.setNamespace("default");

        // Act & Assert
        assertThatThrownBy(() -> SkillValidator.validateGetRequest(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.API_INVALID_ARGUMENT);
    }

    /**
     * 测试目的：缺 namespace 的 GetSkill 请求被拒绝
     * 测试场景：namespace 为空
     * 验证内容：API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateGetRequestBlankNamespace() {
        // Arrange
        SkillGetRequest request = new SkillGetRequest();
        request.setName("sql-analysis");
        request.setNamespace(" ");

        // Act & Assert
        assertThatThrownBy(() -> SkillValidator.validateGetRequest(request))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.API_INVALID_ARGUMENT);
    }

    /**
     * 测试目的：List 允许空过滤条件
     * 测试场景：非空 request
     * 验证内容：不抛异常；null 抛 API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateListRequest() {
        // Arrange
        SkillListRequest request = new SkillListRequest();

        // Act & Assert
        assertThatCode(() -> SkillValidator.validateListRequest(request)).doesNotThrowAnyException();
        assertThatThrownBy(() -> SkillValidator.validateListRequest(null))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.API_INVALID_ARGUMENT);
    }

    /**
     * 测试目的：Download 请求校验与 Get 一致
     * 测试场景：合法、null、缺字段
     * 验证内容：合法通过，非法抛 API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateDownloadRequest() {
        // Arrange
        SkillDownloadRequest request = new SkillDownloadRequest();
        request.setName("sql-analysis");
        request.setNamespace("default");

        // Act & Assert
        assertThatCode(() -> SkillValidator.validateDownloadRequest(request)).doesNotThrowAnyException();
        assertThatThrownBy(() -> SkillValidator.validateDownloadRequest(null))
                .isInstanceOf(PolarisException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.API_INVALID_ARGUMENT);
    }
}
