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

package com.tencent.polaris.ratelimit.api.rpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link QuotaResponse}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class QuotaResponseTest {

    /**
     * 测试单参数构造函数及 getCode 映射
     * 测试目的：验证 QuotaResult.Code 的序号能正确映射到 QuotaResultCode
     * 测试场景：用 QuotaResultOk 构造 QuotaResponse
     * 验证内容：getCode 返回 QuotaResultOk，waitMs/info 与入参一致
     */
    @Test
    public void testConstructorSingleArgAndCodeMappingOk() {
        // Arrange
        QuotaResult quotaResult = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok");

        // Act
        QuotaResponse response = new QuotaResponse(quotaResult);

        // Assert
        assertThat(response.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
        assertThat(response.getWaitMs()).isEqualTo(0L);
        assertThat(response.getInfo()).isEqualTo("ok");
    }

    /**
     * 测试 Limited 状态码映射
     * 测试目的：验证 QuotaResultLimited 能正确映射
     * 测试场景：用 QuotaResultLimited 构造 QuotaResponse
     * 验证内容：getCode 返回 QuotaResultLimited
     */
    @Test
    public void testCodeMappingLimited() {
        // Arrange
        QuotaResult quotaResult = new QuotaResult(QuotaResult.Code.QuotaResultLimited, 100L, "limited");

        // Act
        QuotaResponse response = new QuotaResponse(quotaResult);

        // Assert
        assertThat(response.getCode()).isEqualTo(QuotaResultCode.QuotaResultLimited);
        assertThat(response.getWaitMs()).isEqualTo(100L);
    }

    /**
     * 测试单参数构造函数对 release 的兜底
     * 测试目的：验证 releaseList 为 null 时，从 quotaResult.release 初始化 releaseList
     * 测试场景：构造带 release Runnable 的 QuotaResult，不传 releaseList
     * 验证内容：releaseList 包含该 Runnable
     */
    @Test
    public void testSingleArgConstructorInitReleaseFromResult() {
        // Arrange
        Runnable release = () -> {
        };
        QuotaResult quotaResult = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok", release);

        // Act
        QuotaResponse response = new QuotaResponse(quotaResult);

        // Assert
        assertThat(response.getReleaseList()).containsExactly(release);
    }

    /**
     * 测试单参数构造函数在无 release 时的空列表
     * 测试目的：验证 quotaResult.release 为 null 时 releaseList 为空
     * 测试场景：构造不带 release 的 QuotaResult
     * 验证内容：releaseList 为空列表
     */
    @Test
    public void testSingleArgConstructorEmptyReleaseList() {
        // Arrange
        QuotaResult quotaResult = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok");

        // Act
        QuotaResponse response = new QuotaResponse(quotaResult);

        // Assert
        assertThat(response.getReleaseList()).isEmpty();
    }

    /**
     * 测试双参数构造函数使用外部 releaseList
     * 测试目的：验证传入非 null releaseList 时直接使用，不追加 quotaResult.release
     * 测试场景：传入包含一个 Runnable 的 releaseList，quotaResult 也有 release
     * 验证内容：releaseList 仅包含外部传入的 Runnable
     */
    @Test
    public void testTwoArgConstructorUsesExternalReleaseList() {
        // Arrange
        Runnable resultRelease = () -> {
        };
        QuotaResult quotaResult = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok", resultRelease);
        Runnable external = () -> {
        };
        List<Runnable> externalList = new ArrayList<>();
        externalList.add(external);

        // Act
        QuotaResponse response = new QuotaResponse(quotaResult, externalList);

        // Assert
        assertThat(response.getReleaseList()).containsExactly(external);
    }

    /**
     * 测试 addRelease 追加 Runnable
     * 测试目的：验证 addRelease 能向 releaseList 追加元素
     * 测试场景：构造后调用 addRelease 追加一个 Runnable
     * 验证内容：releaseList size 增加且包含新元素
     */
    @Test
    public void testAddRelease() {
        // Arrange
        QuotaResponse response = new QuotaResponse(
                new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok"));
        Runnable extra = () -> {
        };

        // Act
        response.addRelease(extra);

        // Assert
        assertThat(response.getReleaseList()).containsExactly(extra);
    }

    /**
     * 测试 setReleaseList 对 null 的兜底
     * 测试目的：验证 setReleaseList 传入 null 时初始化为空列表
     * 测试场景：先添加元素，再 setReleaseList(null)
     * 验证内容：releaseList 变为空列表
     */
    @Test
    public void testSetReleaseListWithNull() {
        // Arrange
        QuotaResponse response = new QuotaResponse(
                new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok"));
        response.addRelease(() -> {
        });

        // Act
        response.setReleaseList(null);

        // Assert
        assertThat(response.getReleaseList()).isEmpty();
    }

    /**
     * 测试 setReleaseList 替换列表
     * 测试目的：验证 setReleaseList 传入非 null 时替换原列表
     * 测试场景：构造后 setReleaseList 为新列表
     * 验证内容：releaseList 与新列表一致
     */
    @Test
    public void testSetReleaseListReplaces() {
        // Arrange
        QuotaResponse response = new QuotaResponse(
                new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok"));
        Runnable r1 = () -> {
        };
        Runnable r2 = () -> {
        };

        // Act
        response.setReleaseList(new ArrayList<>(Arrays.asList(r1, r2)));

        // Assert
        assertThat(response.getReleaseList()).containsExactly(r1, r2);
    }

    /**
     * 测试 activeRule 的 setter/getter
     * 测试目的：验证 setActiveRule/getActiveRule 的基本读写
     * 测试场景：setActiveRule(null) 后 getActiveRule 返回 null
     * 验证内容：getActiveRule 为 null
     */
    @Test
    public void testActiveRuleSetterGetter() {
        // Arrange
        QuotaResponse response = new QuotaResponse(
                new QuotaResult(QuotaResult.Code.QuotaResultOk, 0L, "ok"));

        // Act
        response.setActiveRule(null);

        // Assert
        assertThat(response.getActiveRule()).isNull();
    }
}
