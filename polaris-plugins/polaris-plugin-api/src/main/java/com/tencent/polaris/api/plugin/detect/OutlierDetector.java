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

package com.tencent.polaris.api.plugin.detect;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;

/**
 * 【扩展点接口】主动健康探测策略
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface OutlierDetector extends Plugin {

    /**
     * 对单个实例进行探测，返回探测结果
     * 每个探测方法自己去判断当前周期是否需要探测，如果无需探测，则返回nil
     *
     * @param instance 单个服务实例
     * @return 实例探测结果
     * @throws PolarisException 异常信息
     */
    DetectResult detectInstance(Instance instance) throws PolarisException;
}
