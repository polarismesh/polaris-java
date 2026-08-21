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

package com.tencent.polaris.configuration.client.internal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConfigFileSnapshot}.
 *
 * @author evelynwei
 */
public class ConfigFileSnapshotTest {

    /**
     * 测试目的：四元组快照各字段可读且不可变。
     * 测试场景：构造含 version/md5/content/effectiveTime 的快照。
     * 验证内容：四个 getter 返回构造值。
     */
    @Test
    public void testSnapshotFields() {
        ConfigFileSnapshot snapshot = new ConfigFileSnapshot(12, "md5abc", "k=v", 1785900000000L);

        assertThat(snapshot.getVersion()).isEqualTo(12);
        assertThat(snapshot.getMd5()).isEqualTo("md5abc");
        assertThat(snapshot.getContent()).isEqualTo("k=v");
        assertThat(snapshot.getEffectiveTime()).isEqualTo(1785900000000L);
    }

    /**
     * 测试目的：content 为 null 与 effectiveTime 为 0 的初始快照。
     * 测试场景：从未拉取成功的初始快照。
     * 验证内容：content 为 null、effectiveTime 为 0。
     */
    @Test
    public void testInitialSnapshot() {
        ConfigFileSnapshot snapshot = new ConfigFileSnapshot(0, "", null, 0);

        assertThat(snapshot.getVersion()).isZero();
        assertThat(snapshot.getMd5()).isEmpty();
        assertThat(snapshot.getContent()).isNull();
        assertThat(snapshot.getEffectiveTime()).isZero();
    }
}
