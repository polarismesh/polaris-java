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

package com.tencent.polaris.plugins.connector.common.constant;

import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;

/**
 * Constant for {@link ServiceUpdateTask}
 *
 * @author skyehtzhang
 */
public class ServiceUpdateTaskConstant {

    public enum Type {
        /**
         * 首次调度
         */
        FIRST,
        /**
         * 长稳调度
         */
        LONG_RUNNING,
        /**
         * 已经销毁
         */
        TERMINATED
    }

    public enum Status {
        /**
         * 调度中
         */
        RUNNING,
        /**
         * 已经就绪
         */
        READY,
    }

}
