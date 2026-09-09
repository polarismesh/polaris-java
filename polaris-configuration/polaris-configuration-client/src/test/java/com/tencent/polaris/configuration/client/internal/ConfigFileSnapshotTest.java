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

import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConfigFileSnapshot}.
 *
 * @author evelynwei
 */
public class ConfigFileSnapshotTest {

    /**
     * 测试目的：快照各字段可读且不可变。
     * 测试场景：构造含 version/versionName/md5/content/effectiveTime 的快照。
     * 验证内容：getter 返回构造值。
     */
    @Test
    public void testSnapshotFields() {
        ConfigFileSnapshot snapshot = new ConfigFileSnapshot(12, "md5abc", "k=v", 1785900000000L, "v1.0.0");

        assertThat(snapshot.getVersion()).isEqualTo(12);
        assertThat(snapshot.getVersionName()).isEqualTo("v1.0.0");
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
        assertThat(snapshot.getVersionName()).isNull();
        assertThat(snapshot.getMd5()).isEmpty();
        assertThat(snapshot.getContent()).isNull();
        assertThat(snapshot.getEffectiveTime()).isZero();
    }

    /**
     * 测试目的：从 ConfigFile 构造时 version/versionName/md5/加密字段同源。
     * 测试场景：ConfigFile 同时设置发布名与加密字段。
     * 验证内容：快照 getter 与 ConfigFile 一致。
     */
    @Test
    public void testSnapshotFromConfigFile() {
        ConfigFile configFile = new ConfigFile("ns", "g", "f");
        configFile.setVersion(9);
        configFile.setName("release-9");
        configFile.setMd5("md5fromfile");
        configFile.setEncrypted(true);
        configFile.setEncryptAlgo("AES");
        configFile.setDataKey("key");

        ConfigFileSnapshot snapshot = new ConfigFileSnapshot(configFile, "cipher", 123L);

        assertThat(snapshot.getVersion()).isEqualTo(9);
        assertThat(snapshot.getVersionName()).isEqualTo("release-9");
        assertThat(snapshot.getMd5()).isEqualTo("md5fromfile");
        assertThat(snapshot.getContent()).isEqualTo("cipher");
        assertThat(snapshot.getEffectiveTime()).isEqualTo(123L);
        assertThat(snapshot.isEncrypted()).isTrue();
        assertThat(snapshot.getEncryptAlgo()).isEqualTo("AES");
        assertThat(snapshot.getDataKey()).isEqualTo("key");
    }

    /**
     * 测试目的：全量构造器可同时携带版本名与加密字段。
     * 测试场景：直接使用全量构造器。
     * 验证内容：八个字段逐一回读一致，版本名不会被加密字段挤掉。
     */
    @Test
    public void testFullSnapshotConstructor() {
        ConfigFileSnapshot snapshot = new ConfigFileSnapshot(7, "v2.0.0", "md5full", "cipher", 456L,
                true, "AES", "key");

        assertThat(snapshot.getVersion()).isEqualTo(7);
        assertThat(snapshot.getVersionName()).isEqualTo("v2.0.0");
        assertThat(snapshot.getMd5()).isEqualTo("md5full");
        assertThat(snapshot.getContent()).isEqualTo("cipher");
        assertThat(snapshot.getEffectiveTime()).isEqualTo(456L);
        assertThat(snapshot.isEncrypted()).isTrue();
        assertThat(snapshot.getEncryptAlgo()).isEqualTo("AES");
        assertThat(snapshot.getDataKey()).isEqualTo("key");
    }

    /**
     * 测试目的：加密重载不携带版本名，普通重载不携带加密字段，各自默认值明确。
     * 测试场景：分别使用加密七参重载与普通五参重载。
     * 验证内容：加密快照版本名为 null，普通快照加密字段为默认值。
     */
    @Test
    public void testOverloadDefaults() {
        ConfigFileSnapshot encryptedSnapshot = new ConfigFileSnapshot(3, "md5abc", "cipher", 1L,
                true, "AES", "key");
        ConfigFileSnapshot plainSnapshot = new ConfigFileSnapshot(3, "md5abc", "k=v", 1L, "v1.0.0");

        assertThat(encryptedSnapshot.getVersionName()).isNull();
        assertThat(encryptedSnapshot.isEncrypted()).isTrue();
        assertThat(plainSnapshot.getVersionName()).isEqualTo("v1.0.0");
        assertThat(plainSnapshot.isEncrypted()).isFalse();
        assertThat(plainSnapshot.getEncryptAlgo()).isNull();
        assertThat(plainSnapshot.getDataKey()).isNull();
    }
}
