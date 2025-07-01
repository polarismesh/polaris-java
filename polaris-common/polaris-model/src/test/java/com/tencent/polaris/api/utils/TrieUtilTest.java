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

package com.tencent.polaris.api.utils;

import com.tencent.polaris.api.pojo.TrieNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link TrieUtil}.
 */
public class TrieUtilTest {

    @Test
    public void testCheckSimple() {
        TrieNode<String> rootWithMethod = TrieUtil.buildSimpleApiTrieNode("/echo/{param}-GET");
        TrieNode<String> rootWithoutMethod = TrieUtil.buildSimpleApiTrieNode("/echo/{param}-GET");
        assertThat(TrieUtil.checkSimpleApi(rootWithMethod, "/echo/test")).isTrue();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echo/test")).isTrue();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echoo/test")).isFalse();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echo/")).isFalse();
        assertThat(TrieUtil.checkSimpleApi(rootWithMethod, "/echo/test-GET")).isTrue();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echo/test-GET")).isTrue();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echo/test-POST")).isTrue();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echo/test-POST")).isTrue();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echoo/test-GET")).isFalse();
        assertThat(TrieUtil.checkSimpleApi(rootWithoutMethod, "/echo/-GET")).isFalse();
    }

    @Test
    public void testCheckConfig() {
        TrieNode<String> root1 = TrieUtil.buildConfigTrieNode("provider.config");
        assertThat(TrieUtil.checkConfig(root1, "provider.config.test")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, "provider.config.test.aaa")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, "provider.config2.test")).isFalse();
        assertThat(TrieUtil.checkConfig(root1, "provider")).isFalse();
        assertThat(TrieUtil.checkConfig(root1, "provider.config")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, "provider.config.nameList[1]")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, null)).isFalse();

        TrieNode<String> root2 = TrieUtil.buildConfigTrieNode("provider.conf");
        assertThat(TrieUtil.checkConfig(root2, "provider.conf.test")).isTrue();
        assertThat(TrieUtil.checkConfig(root2, "provider.conf.test.aaa")).isTrue();
        assertThat(TrieUtil.checkConfig(root2, "provider.config2.test")).isFalse();
        assertThat(TrieUtil.checkConfig(root2, "provider")).isFalse();
        assertThat(TrieUtil.checkConfig(root2, "provider.config")).isFalse();
        assertThat(TrieUtil.checkConfig(root2, "provider.conf")).isTrue();
        assertThat(TrieUtil.checkConfig(root2, "provider.conf.nameList[1]")).isTrue();

        TrieNode<String> root3 = new TrieNode<>(TrieNode.ROOT_PATH);
        assertThat(TrieUtil.checkConfig(root3, "provider.config.test")).isFalse();
    }

    @Test
    public void testCheckConfig2() {
        TrieNode<String> root1 = TrieUtil.buildConfigTrieNode("provider.config.list");
        assertThat(TrieUtil.checkConfig(root1, "provider.config.list[1]")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, "provider.config.list[1].name")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, "provider.config.name")).isFalse();

        TrieNode<String> root2 = TrieUtil.buildConfigTrieNode("provider.config.map");
        assertThat(TrieUtil.checkConfig(root2, "provider.config.map.key1")).isTrue();
        assertThat(TrieUtil.checkConfig(root2, "provider.config.map.key2")).isTrue();
        assertThat(TrieUtil.checkConfig(root1, "provider.config.name")).isFalse();

        TrieNode<String> root3 = new TrieNode<>(TrieNode.ROOT_PATH);
        assertThat(TrieUtil.checkConfig(root3, "provider.config.test")).isFalse();
    }

    @Test
    public void testCheckConfigMergeRoot() {
        TrieNode<String> root = new TrieNode<>(TrieNode.ROOT_PATH);

        TrieUtil.buildConfigTrieNode("provider.config", root);
        TrieUtil.buildConfigTrieNode("provider.conf", root);

        assertThat(TrieUtil.checkConfig(root, "provider.config.test")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.config.test.aaa")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.config2.test")).isFalse();
        assertThat(TrieUtil.checkConfig(root, "provider")).isFalse();
        assertThat(TrieUtil.checkConfig(root, "provider.config")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.config.nameList[1]")).isTrue();


        assertThat(TrieUtil.checkConfig(root, "provider.conf.test")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.conf.test.aaa")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.config2.test")).isFalse();
        assertThat(TrieUtil.checkConfig(root, "provider")).isFalse();
        assertThat(TrieUtil.checkConfig(root, "provider.config")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.conf")).isTrue();
        assertThat(TrieUtil.checkConfig(root, "provider.conf.nameList[1]")).isTrue();
    }
}
