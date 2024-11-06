/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
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

import com.tencent.polaris.api.pojo.TrieNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ApiTrieUtil}.
 */
public class ApiTrieUtilTest {

    @Test
    public void testCheckSimple() {
        TrieNode<String> rootWithMethod = ApiTrieUtil.buildSimpleTrieNode("/echo/{param}-GET");
        TrieNode<String> rootWithoutMethod = ApiTrieUtil.buildSimpleTrieNode("/echo/{param}-GET");
        assertThat(ApiTrieUtil.checkSimple(rootWithMethod, "/echo/test")).isTrue();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echo/test")).isTrue();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echoo/test")).isFalse();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echo/")).isFalse();
        assertThat(ApiTrieUtil.checkSimple(rootWithMethod, "/echo/test-GET")).isTrue();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echo/test-GET")).isTrue();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echo/test-POST")).isTrue();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echo/test-POST")).isTrue();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echoo/test-GET")).isFalse();
        assertThat(ApiTrieUtil.checkSimple(rootWithoutMethod, "/echo/-GET")).isFalse();
    }

    @Test
    public void testCheckConfig() {
        TrieNode<String> root1 = ApiTrieUtil.buildConfigTrieNode("provider.config");
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.test")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.test.aaa")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config2.test")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.nameList[1]")).isTrue();

        TrieNode<String> root2 = ApiTrieUtil.buildConfigTrieNode("provider.conf");
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.conf.test")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.conf.test.aaa")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.config2.test")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.config")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.conf")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.conf.nameList[1]")).isTrue();

        TrieNode<String> root3 = new TrieNode<>(TrieNode.ROOT_PATH);
        assertThat(ApiTrieUtil.checkConfig(root3, "provider.config.test")).isFalse();
    }

    @Test
    public void testCheckConfig2() {
        TrieNode<String> root1 = ApiTrieUtil.buildConfigTrieNode("provider.config.list");
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.list[1]")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.list[1].name")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.name")).isFalse();

        TrieNode<String> root2 = ApiTrieUtil.buildConfigTrieNode("provider.config.map");
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.config.map.key1")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root2, "provider.config.map.key2")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root1, "provider.config.name")).isFalse();

        TrieNode<String> root3 = new TrieNode<>(TrieNode.ROOT_PATH);
        assertThat(ApiTrieUtil.checkConfig(root3, "provider.config.test")).isFalse();
    }

    @Test
    public void testCheckConfigMergeRoot() {
        TrieNode<String> root = new TrieNode<>(TrieNode.ROOT_PATH);

        ApiTrieUtil.buildConfigTrieNode("provider.config", root);
        ApiTrieUtil.buildConfigTrieNode("provider.conf", root);

        assertThat(ApiTrieUtil.checkConfig(root, "provider.config.test")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.config.test.aaa")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.config2.test")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root, "provider")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.config")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.config.nameList[1]")).isTrue();


        assertThat(ApiTrieUtil.checkConfig(root, "provider.conf.test")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.conf.test.aaa")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.config2.test")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root, "provider")).isFalse();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.config")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.conf")).isTrue();
        assertThat(ApiTrieUtil.checkConfig(root, "provider.conf.nameList[1]")).isTrue();
    }
}
