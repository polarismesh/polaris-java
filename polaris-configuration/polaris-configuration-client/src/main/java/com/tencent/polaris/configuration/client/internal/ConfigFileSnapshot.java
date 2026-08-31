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

/**
 * ConfigFile 版本、版本名、MD5、内容与生效时间的不可变快照，保证字段来自同一份配置对象。
 *
 * @author fishtailfu
 */
public class ConfigFileSnapshot {

    private final long version;

    private final String versionName;

    private final String md5;

    private final String content;

    private final long effectiveTime;

    private final boolean encrypted;

    private final String encryptAlgo;

    private final String dataKey;

    public ConfigFileSnapshot(long version, String md5, String content, long effectiveTime) {
        this(version, md5, content, effectiveTime, false, null, null);
    }

    public ConfigFileSnapshot(long version, String md5, String content, long effectiveTime, String versionName) {
        this.version = version;
        this.versionName = versionName;
        this.md5 = md5;
        this.content = content;
        this.effectiveTime = effectiveTime;
        this.encrypted = false;
        this.encryptAlgo = null;
        this.dataKey = null;
    }

    public ConfigFileSnapshot(long version, String md5, String content, long effectiveTime, boolean encrypted,
            String encryptAlgo, String dataKey) {
        this.version = version;
        this.versionName = null;
        this.md5 = md5;
        this.content = content;
        this.effectiveTime = effectiveTime;
        this.encrypted = encrypted;
        this.encryptAlgo = encryptAlgo;
        this.dataKey = dataKey;
    }

    /**
     * 从同一份本地 ConfigFile 构造快照，version/versionName/md5 与加密字段同源。
     *
     * @param configFile 本地配置
     * @param content ACK/上报使用的源内容
     * @param effectiveTime 生效时间戳
     */
    public ConfigFileSnapshot(ConfigFile configFile, String content, long effectiveTime) {
        this.version = configFile.getVersion();
        this.versionName = configFile.getName();
        this.md5 = configFile.getMd5();
        this.content = content;
        this.effectiveTime = effectiveTime;
        this.encrypted = configFile.isEncrypted();
        this.encryptAlgo = configFile.getEncryptAlgo();
        this.dataKey = configFile.getDataKey();
    }

    public long getVersion() {
        return version;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getMd5() {
        return md5;
    }

    public String getContent() {
        return content;
    }

    public long getEffectiveTime() {
        return effectiveTime;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getEncryptAlgo() {
        return encryptAlgo;
    }

    public String getDataKey() {
        return dataKey;
    }
}
