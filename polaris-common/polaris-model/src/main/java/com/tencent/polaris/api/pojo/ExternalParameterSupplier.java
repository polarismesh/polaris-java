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

package com.tencent.polaris.api.pojo;

import java.util.Optional;

/**
 * 北极星内部治理规则执行时，会识别规则中的参数来源类别，如果发现规则中的参数来源指定为外部数据源时，会调用本接口进行获取
 *
 * 可以实现该接口，实现规则中的参数来源于配置中心、数据库、环境变量等等
 */
public interface ExternalParameterSupplier {

    /**
     * 规则中外部参数的 key 标识，插件提供者需要根据 key ，在自己的参数数据源中返回该 key 所对应的真实规则参数信息
     *
     * @param key
     * @return {@link Optional<String>} 如果参数提供者没有该 key 对应的真实参数信息，返回一个空的 Optional
     */
    Optional<String> search(String key);

}
