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

package com.tencent.polaris.api.config.verify;

/**
 * 检验SDK配置
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface Verifier {

    /**
     * 执行校验操作，参数校验失败会抛出IllegalArgumentException
     */
    void verify();

    /**
     * 设置默认值信息
     *
     * @param defaultObject 默认值对象
     */
    void setDefault(Object defaultObject);

}
