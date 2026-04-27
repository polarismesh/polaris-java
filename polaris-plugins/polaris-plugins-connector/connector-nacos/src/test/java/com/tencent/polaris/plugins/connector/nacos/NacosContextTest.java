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

package com.tencent.polaris.plugins.connector.nacos;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * NacosContext 服务名映射表单元测试。
 */
public class NacosContextTest {

    @Test
    public void putServiceNameMapping_storesEntry() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        Assert.assertEquals("X", ctx.getServiceNameMappings().get("A"));
    }

    @Test
    public void putServiceNameMapping_nullPolarisName_ignored() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping(null, "X");
        Assert.assertTrue(ctx.getServiceNameMappings().isEmpty());
    }

    @Test
    public void putServiceNameMapping_emptyPolarisName_ignored() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("", "X");
        Assert.assertTrue(ctx.getServiceNameMappings().isEmpty());
    }

    @Test
    public void putServiceNameMapping_nullNacosName_removesEntry() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        ctx.putServiceNameMapping("A", null);
        Assert.assertFalse(ctx.getServiceNameMappings().containsKey("A"));
    }

    @Test
    public void putServiceNameMapping_emptyNacosName_removesEntry() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        ctx.putServiceNameMapping("A", "");
        Assert.assertFalse(ctx.getServiceNameMappings().containsKey("A"));
    }

    @Test
    public void removeServiceNameMapping_removesExistingEntry() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        ctx.removeServiceNameMapping("A");
        Assert.assertFalse(ctx.getServiceNameMappings().containsKey("A"));
    }

    @Test
    public void removeServiceNameMapping_nullKey_noException() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        ctx.removeServiceNameMapping(null);
        Assert.assertTrue(ctx.getServiceNameMappings().containsKey("A"));
    }

    @Test
    public void setServiceNameMappings_replacesAllEntries() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        Map<String, String> next = new HashMap<>();
        next.put("B", "Y");
        ctx.setServiceNameMappings(next);
        Assert.assertFalse(ctx.getServiceNameMappings().containsKey("A"));
        Assert.assertEquals("Y", ctx.getServiceNameMappings().get("B"));
    }

    @Test
    public void setServiceNameMappings_filtersEmptyEntries() {
        NacosContext ctx = new NacosContext();
        Map<String, String> input = new HashMap<>();
        input.put("A", "X");
        input.put("B", "");
        input.put("C", null);
        input.put("", "Z");
        // HashMap 允许一个 null key;这里故意选 HashMap 以覆盖 null-key 过滤分支
        input.put(null, "W");
        ctx.setServiceNameMappings(input);
        Assert.assertEquals(1, ctx.getServiceNameMappings().size());
        Assert.assertEquals("X", ctx.getServiceNameMappings().get("A"));
    }

    @Test
    public void setServiceNameMappings_null_clearsTable() {
        NacosContext ctx = new NacosContext();
        ctx.putServiceNameMapping("A", "X");
        ctx.setServiceNameMappings(null);
        Assert.assertTrue(ctx.getServiceNameMappings().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getServiceNameMappings_returnsUnmodifiableView() {
        NacosContext ctx = new NacosContext();
        ctx.getServiceNameMappings().put("A", "X");
    }

    @Test
    public void concurrentPut_noLostWrites() throws Exception {
        final NacosContext ctx = new NacosContext();
        final int threads = 8;
        final int perThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                final int offset = t * perThread;
                pool.submit(() -> {
                    for (int i = 0; i < perThread; i++) {
                        ctx.putServiceNameMapping("svc" + (offset + i), "nacos" + (offset + i));
                    }
                });
            }
            pool.shutdown();
            Assert.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            if (!pool.isTerminated()) {
                pool.shutdownNow();
            }
        }
        Assert.assertEquals(threads * perThread, ctx.getServiceNameMappings().size());
    }
}
