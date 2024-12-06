/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.encrypt;

/**
 * TSF 配置加密提供器接口
 *
 * @author hongweizhu
 */
public abstract class ConfigEncryptProvider {

    /**
     * 加密
     *
     * @param content  明文
     * @param password 密码
     * @return 密文
     */
    public abstract String encrypt(String content, String password);

    /**
     * 解密
     *
     * @param encryptedContent 密文
     * @param password         密码
     * @return 明文
     */
    public abstract String decrypt(String encryptedContent, String password);
}
