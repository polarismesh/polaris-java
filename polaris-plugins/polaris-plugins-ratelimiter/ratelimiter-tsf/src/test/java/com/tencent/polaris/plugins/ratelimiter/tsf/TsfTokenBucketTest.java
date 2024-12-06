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

package com.tencent.polaris.plugins.ratelimiter.tsf;

import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Haotian Zhang
 */
public class TsfTokenBucketTest {
    private static List<Long> buildRefillTimestamps() {
        List<Long> nowTimestamps = new ArrayList<>();
        nowTimestamps.add(49660159513004553L);
        nowTimestamps.add(49660159516426905L);
        nowTimestamps.add(49660159523459932L);
        nowTimestamps.add(49660159527792838L);
        nowTimestamps.add(49660159535601037L);
        nowTimestamps.add(49660159548316431L);
        nowTimestamps.add(49660159555494251L);
        nowTimestamps.add(49660159561041565L);
        nowTimestamps.add(49660159566454978L);
        nowTimestamps.add(49660159577088290L);
        nowTimestamps.add(49660159578864957L);
        nowTimestamps.add(49660159580307839L);
        nowTimestamps.add(49660159586826208L);
        nowTimestamps.add(49660159600019009L);
        nowTimestamps.add(49660159602102471L);
        nowTimestamps.add(49660159609781848L);
        nowTimestamps.add(49660159622526382L);
        nowTimestamps.add(49660159632893860L);
        nowTimestamps.add(49660159649375561L);
        nowTimestamps.add(49660159671635781L);
        nowTimestamps.add(49660159674054467L);
        nowTimestamps.add(49660159679652343L);
        nowTimestamps.add(49660159696563221L);
        nowTimestamps.add(49660159704360279L);
        nowTimestamps.add(49660159711285355L);
        nowTimestamps.add(49660159727295199L);
        nowTimestamps.add(49660160921049622L);
        nowTimestamps.add(49660160922049622L);
        nowTimestamps.add(49660160924777179L);
        return nowTimestamps;
    }

    // case1: 对应中行2023-10-15单节点异常限流问题：验证周期末尾出现CapacityDebuff过大导致，验证debuff不能超过newCapacity
    @Test
    public void testSetNewCapacity1() {
        long oldCapacity = 12;
        long newCapacity = 11;
        long duration = 1;
        TimeUnit unit = TimeUnit.SECONDS;
        String reportId = "rate-jqv3d9a7";
        long currentPeriodStartAt = 64585051612675526L;
        long lastRefillTime = 64585052540342186L;
        double ratioPassed = 0.999749929;
        long unitNanos = unit.toNanos(duration);
        MockTicker ticker = new MockTicker();
        TsfTokenBucket tsfTokenBucket = new TsfTokenBucket(oldCapacity, duration, unit,
                reportId, ticker, currentPeriodStartAt, lastRefillTime);
        tsfTokenBucket.setSize(10);
        long now1 = 64585052612335490L; // 用于做setNewCapacity时候的refill
        long now2 = (long) (ratioPassed * unitNanos) + currentPeriodStartAt;
        ticker.addTimestamp(now1);
        ticker.addTimestamp(now2);
        tsfTokenBucket.onRemoteUpdate(new RemoteQuotaInfo(newCapacity, 0, 0, 0));
        long capacityDebuffForTokenRefill = tsfTokenBucket.getCapacityDebuffForTokenRefill();
        System.out.println(capacityDebuffForTokenRefill);
        Assert.assertEquals(1, capacityDebuffForTokenRefill);
    }

    // case2：测试当下发时间比例从34%到99.99%的变化下的debuff值变化，验证debuff不能超过newCapacity
    @Test
    public void testSetNewCapacity2() {
        double ratioPassed = 0.34;
        while (ratioPassed < 1) {
            long oldCapacity = 12;
            long newCapacity = 11;
            long duration = 1;
            TimeUnit unit = TimeUnit.SECONDS;
            String reportId = "rate-jqv3d9a7";
            long currentPeriodStartAt = 64585051612675526L;
            long lastRefillTime = 64585052540342186L;
            long unitNanos = unit.toNanos(duration);
            MockTicker ticker = new MockTicker();
            TsfTokenBucket tsfTokenBucket = new TsfTokenBucket(oldCapacity, duration, unit,
                    reportId, ticker, currentPeriodStartAt, lastRefillTime);
            tsfTokenBucket.setSize(10);
            long now1 = 64585052612335490L; // 用于做setNewCapacity时候的refill
            long now2 = (long) (ratioPassed * unitNanos) + currentPeriodStartAt;
            ticker.addTimestamp(now1);
            ticker.addTimestamp(now2);
            tsfTokenBucket.onRemoteUpdate(new RemoteQuotaInfo(newCapacity, 0, 0, 0));
            long capacityDebuffForTokenRefill = tsfTokenBucket.getCapacityDebuffForTokenRefill();
            Assert.assertTrue(capacityDebuffForTokenRefill == 0 || capacityDebuffForTokenRefill == 1);
            System.out.println("ratioPassed is " + ratioPassed + ", capacityDebuffForTokenRefill is " + capacityDebuffForTokenRefill);
            if (ratioPassed >= 0.999) {
                ratioPassed += 0.00011;
            } else if (ratioPassed >= 0.99) {
                ratioPassed += 0.0011;
            } else if (ratioPassed >= 0.9) {
                ratioPassed += 0.011;
            } else {
                ratioPassed += 0.15;
            }
        }
    }

    // case3: 对应中行2023-10-17单节点异常限流问题(正常计算情况下也出现了Debuff比NewCapacity大)：测试容量变更较大的情况下，后续流量分配的问题，验证size不出现负值
    @Test
    public void testSetNewCapacity3() {
        long oldCapacity = 20;
        long newCapacity = 8;
        long duration = 1;
        TimeUnit unit = TimeUnit.SECONDS;
        String reportId = "rate-jqv3d9a7";
        long currentPeriodStartAt = 49660159180597172L;
        long lastRefillTime = 49660159483430448L;
        double ratioPassed = 0.324292147;
        long unitNanos = unit.toNanos(duration);
        MockTicker ticker = new MockTicker();
        TsfTokenBucket tsfTokenBucket = new TsfTokenBucket(oldCapacity, duration, unit,
                reportId, ticker, currentPeriodStartAt, lastRefillTime);
        tsfTokenBucket.setSize(0);
        long now1 = 49660159504769937L; // 用于做setNewCapacity时候的refill
        long now2 = (long) (ratioPassed * unitNanos) + currentPeriodStartAt;
        ticker.addTimestamp(now1);
        ticker.addTimestamp(now2);
        tsfTokenBucket.onRemoteUpdate(new RemoteQuotaInfo(newCapacity, 0, 0, 0));
        long capacityDebuffForTokenRefill = tsfTokenBucket.getCapacityDebuffForTokenRefill();
        Assert.assertTrue(capacityDebuffForTokenRefill < newCapacity);
        System.out.println("capacityDebuffForTokenRefill = " + capacityDebuffForTokenRefill);
        List<Long> nowTimestamps = buildRefillTimestamps();
        for (Long value : nowTimestamps) {
            ticker.addTimestamp(value);
        }
        for (Long nowTimestamp : nowTimestamps) {
            tsfTokenBucket.refillAndSyncPeriod();
            System.out.println("now = " + nowTimestamp + ", size = " + tsfTokenBucket.getSize());
            Assert.assertTrue(tsfTokenBucket.getSize() >= 0);
        }
    }

}
