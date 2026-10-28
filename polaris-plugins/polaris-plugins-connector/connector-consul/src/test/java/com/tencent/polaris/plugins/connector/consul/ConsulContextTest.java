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

package com.tencent.polaris.plugins.connector.consul;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Test for {@link ConsulContext}.
 *
 * @author Ajax-1
 */
public class ConsulContextTest {

    @Test
    public void testDefaultRefreshThreadPoolSize() {
        // Arrange：新建默认上下文
        ConsulContext consulContext = new ConsulContext();

        // Act：读取刷新线程池大小
        int poolSize = consulContext.getRefreshThreadPoolSize();

        // Assert：默认值为 8
        Assertions.assertThat(poolSize).isEqualTo(ConsulContext.DEFAULT_REFRESH_THREAD_POOL_SIZE).isEqualTo(8);
    }

    @Test
    public void testSetRefreshThreadPoolSize() {
        // Arrange：新建默认上下文
        ConsulContext consulContext = new ConsulContext();

        // Act：设置刷新线程池大小
        consulContext.setRefreshThreadPoolSize(32);

        // Assert：setter 生效
        Assertions.assertThat(consulContext.getRefreshThreadPoolSize()).isEqualTo(32);
    }
}
