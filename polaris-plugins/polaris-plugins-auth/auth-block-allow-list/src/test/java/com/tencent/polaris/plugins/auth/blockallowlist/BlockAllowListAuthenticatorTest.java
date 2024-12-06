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

package com.tencent.polaris.plugins.auth.blockallowlist;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.auth.AuthInfo;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.security.BlockAllowListProto;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link BlockAllowListAuthenticator}.
 *
 * @author Haotian Zhang
 */
public class BlockAllowListAuthenticatorTest {

    private final String testNamespace = "testNamespace";
    private final String testService = "testService";
    private final String testPath = "/path";
    private final String testProtocol = "HTTP";
    private final String testMethod = "GET";
    private final MetadataContext testMetadataContext = new MetadataContext();

    /**
     * 测试空的黑白名单规则列表，预期返回true
     */
    @Test
    public void testCheckAllow_WithEmptyBlockAllowListRuleList_ShouldReturnTrue() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, testPath, testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Collections.emptyList();

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isTrue();
    }

    /**
     * 测试非空的黑白名单规则列表，且禁用了允许列表，预期返回true
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndDisableAllowList_ShouldReturnTrue() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, testPath, testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Collections.singletonList(
                createBlockAllowListRule(false, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isTrue();
    }

    /**
     * 测试非空的黑白名单规则列表，且只有允许列表，预期返回true
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndOnlyAllowList_ShouldReturnTrue() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, testPath, testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Collections.singletonList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isTrue();
    }

    /**
     * 测试非空的黑白名单规则列表，且只有阻止列表，预期返回false
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndOnlyBlockList_ShouldReturnFalse() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, testPath, testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Collections.singletonList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.BLOCK_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isFalse();
    }

    /**
     * 测试非空的黑白名单规则列表，且只有允许列表，但请求路径不匹配，预期返回false
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndOnlyAllowListButNotMatch_ShouldReturnFalse() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, "/no-test", testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Collections.singletonList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isFalse();
    }

    /**
     * 测试非空的黑白名单规则列表，且只有阻止列表，但请求路径不匹配，预期返回true
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndOnlyBlockListButNotMatch_ShouldReturnTrue() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, "/no-test", testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Collections.singletonList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.BLOCK_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isTrue();
    }

    /**
     * 测试非空的黑白名单规则列表，允许列表匹配且阻止列表不匹配，预期返回true
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndAllowListMatchAndBlockListNotMatch_ShouldReturnTrue() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, testPath, testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Arrays.asList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST),
                createBlockAllowListRule(true, "/no-test", BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.BLOCK_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isTrue();
    }

    /**
     * 测试非空的黑白名单规则列表，允许列表不匹配且阻止列表匹配，预期返回false
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndAllowListNotMatchAndBlockListMatch_ShouldReturnFalse() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, "/no-test", testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Arrays.asList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST),
                createBlockAllowListRule(true, "/no-test", BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.BLOCK_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isFalse();
    }

    /**
     * 测试非空的黑白名单规则列表，允许列表和阻止列表都不匹配，预期返回false
     */
    @Test
    public void testCheckAllow_WithNonEmptyBlockAllowListRuleListAndAllowListNotMatchAndBlockListNotMatch_ShouldReturnFalse() {
        BlockAllowListAuthenticator authenticator = new BlockAllowListAuthenticator();
        AuthInfo authInfo = new AuthInfo(testNamespace, testService, "/no-no-test", testProtocol, testMethod, testMetadataContext);
        List<BlockAllowListProto.BlockAllowListRule> blockAllowListRuleList = Arrays.asList(
                createBlockAllowListRule(true, testPath, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.ALLOW_LIST),
                createBlockAllowListRule(true, "/no-test", BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy.BLOCK_LIST)
        );

        boolean result = authenticator.checkAllow(authInfo, blockAllowListRuleList);

        assertThat(result).isFalse();
    }

    /**
     * 创建一个BlockAllowListProto.BlockAllowListRule实例
     *
     * @param enable 是否启用规则
     * @param path   请求路径
     * @param policy 黑白名单策略
     * @return BlockAllowListProto.BlockAllowListRule实例
     */
    private BlockAllowListProto.BlockAllowListRule createBlockAllowListRule(boolean enable, String path, BlockAllowListProto.BlockAllowConfig.BlockAllowPolicy policy) {
        return BlockAllowListProto.BlockAllowListRule.newBuilder()
                .setEnable(enable)
                .addBlockAllowConfig(BlockAllowListProto.BlockAllowConfig.newBuilder()
                        .setApi(ModelProto.API.newBuilder()
                                .setPath(ModelProto.MatchString.newBuilder().setValue(StringValue.of(path)).build())
                                .setProtocol(testProtocol)
                                .setMethod(testMethod)
                                .build())
                        .setBlockAllowPolicy(policy)
                        .build())
                .build();
    }
}
