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

package com.tencent.polaris.plugin.lossless.warmup;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.weight.WeightAdjuster;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.InstanceWeight;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugin.lossless.common.LosslessRuleDictionary;
import com.tencent.polaris.plugin.lossless.common.LosslessRuleListener;
import com.tencent.polaris.plugin.lossless.common.LosslessUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;
import org.slf4j.Logger;

public class WarmupWeightAdjuster implements WeightAdjuster {
    private static final Logger LOG = LoggerFactory.getLogger(WarmupWeightAdjuster.class);

    public static final String WARMUP_WEIGHT_ADJUSTER_NAME = "warmup";

    private Extensions extensions;

    private LosslessRuleDictionary losslessRuleDictionary;

    private final ConcurrentHashMap<ServiceKey, Lock> lockMap = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return WARMUP_WEIGHT_ADJUSTER_NAME;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.WEIGHT_ADJUSTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        this.extensions = extensions;
        losslessRuleDictionary = new LosslessRuleDictionary();

        extensions.getLocalRegistry().registerResourceListener(new LosslessRuleListener(losslessRuleDictionary));
    }

    @Override
    public void destroy() {

    }

    @Override
    public Map<String, InstanceWeight> timingAdjustDynamicWeight(Map<String, InstanceWeight> dynamicWeight,
            ServiceInstances instances) {

        if (CollectionUtils.isEmpty(instances.getInstances())) {
            return Collections.emptyMap();
        }
        Map<String, InstanceWeight> result = losslessRuleDictionary.getInstanceWeight(instances.getServiceKey());

        if (result != null && result.size() == instances.getInstances().size()) {
            return result;
        }
        // lock for every service key
        Lock lock = lockMap.computeIfAbsent(instances.getServiceKey(), k -> new ReentrantLock());
        lock.lock();
        try {
            // double check
            result = losslessRuleDictionary.getInstanceWeight(instances.getServiceKey());
            if (result != null && result.size() == instances.getInstances().size()) {
                return result;
            }
            result = doTimingAdjustDynamicWeight(dynamicWeight, instances);
            losslessRuleDictionary.putInstanceWeight(instances.getServiceKey(), result);
        } finally {
            lock.unlock();
        }
        return result;
    }

    private Map<String, InstanceWeight> doTimingAdjustDynamicWeight(Map<String, InstanceWeight> dynamicWeight,
            ServiceInstances instances) {

        List<LosslessProto.LosslessRule> losslessRuleList = LosslessUtils.getLosslessRules(extensions,
                instances.getNamespace(), instances.getService());

        if (CollectionUtils.isEmpty(losslessRuleList)) {
            return Collections.emptyMap();
        }
        // metadata key, {metadata value -> lossless rule}}
        Map<String, Map<String, LosslessProto.LosslessRule>> metadataWarmupRules = losslessRuleDictionary.getMetadataLosslessRules(
                new ServiceKey(instances.getNamespace(), instances.getService()));

        if (metadataWarmupRules.isEmpty()) {
            // no match metadata, select first warmup rule
            return getInstanceWeightFromLosslessRule(dynamicWeight, instances, losslessRuleList.get(0));
        } else {
            return getInstanceWeightFromMetadataRule(dynamicWeight, metadataWarmupRules, instances);
        }
    }

    private Map<String, InstanceWeight> getInstanceWeightFromMetadataRule(Map<String, InstanceWeight> originDynamicWeight,
            Map<String, Map<String, LosslessProto.LosslessRule>> metadataLosslessRules,
            ServiceInstances instances) {

        Map<String, InstanceWeight> result = new HashMap<>(originDynamicWeight);
        long currentTime = System.currentTimeMillis();
        // enableOverloadProtection of all instance warmup rules is true, then needOverloadProtection is true
        boolean needOverloadProtection = true;
        int overloadProtectionThreshold = 0;
        int warmupInstanceCount = 0;
        for (Instance instance : instances.getInstances()) {
            LosslessProto.LosslessRule losslessRule = LosslessUtils.getMatchMetadataLosslessRule(instance, metadataLosslessRules);
            Optional<LosslessProto.Warmup> warmupOptional = Optional.ofNullable(losslessRule).
                    map(LosslessProto.LosslessRule::getLosslessOnline).
                    map(LosslessProto.LosslessOnline::getWarmup);

            if (warmupOptional.isPresent()) {
                LosslessProto.Warmup warmup = warmupOptional.get();
                if (!warmup.getEnable()) {
                    continue;
                }
                if (!warmup.getEnableOverloadProtection()) {
                    needOverloadProtection = false;
                }
                if (warmup.getOverloadProtectionThreshold() > overloadProtectionThreshold) {
                    overloadProtectionThreshold = warmup.getOverloadProtectionThreshold();
                }
                InstanceWeight weight = getInstanceWeight(originDynamicWeight, instance, warmup, currentTime);
                if (weight.isDynamicWeightValid()) {
                    warmupInstanceCount++;
                }
                result.put(instance.getId(), weight);
            }
        }
        if (needOverloadProtection) {
            if (warmupInstanceCount / instances.getInstances().size() * 100 > overloadProtectionThreshold) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[getInstanceWeightFromMetadataRule] warmup instance count:{}, instance size:{}, threshold:{}",
                            warmupInstanceCount, instances.getInstances().size(), overloadProtectionThreshold);
                }
                return originDynamicWeight;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[getInstanceWeightFromMetadataRule] warmup instance size:{}, result:{}",
                    warmupInstanceCount, result);
        }
        // if no instance need warmup, return originDynamicWeight
        if (warmupInstanceCount == 0) {
            return originDynamicWeight;
        } else {
            return result;
        }
    }

    private Map<String, InstanceWeight> getInstanceWeightFromLosslessRule(Map<String, InstanceWeight> originDynamicWeight,
            ServiceInstances instances, LosslessProto.LosslessRule losslessRule) {

        long currentTime = System.currentTimeMillis();
        Map<String, InstanceWeight> result = new HashMap<>(originDynamicWeight);
		LosslessProto.Warmup warmup = losslessRule.getLosslessOnline().getWarmup();

        if (!warmup.getEnable()) {
            return originDynamicWeight;
        }
        if (warmup.getEnableOverloadProtection()) {
            int needWarmupCount = countNeedWarmupInstances(instances.getInstances(), warmup, currentTime);
            if (needWarmupCount / instances.getInstances().size() * 100 >= warmup.getOverloadProtectionThreshold()) {
                LOG.debug("[getInstanceWeightFromLosslessRule] need warmup instance size:{}, instance size:{}, threshold:{}",
                        needWarmupCount, instances.getInstances().size(), warmup.getOverloadProtectionThreshold());
                return originDynamicWeight;
            }
        }
        int warmupInstanceCount = 0;
        for (Instance instance : instances.getInstances()) {
            InstanceWeight weight = getInstanceWeight(originDynamicWeight, instance, warmup, currentTime);
            if (weight.isDynamicWeightValid()) {
                warmupInstanceCount++;
            }
            result.put(instance.getId(), weight);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("[getInstanceWeightFromLosslessRule] warmup instance size:{}, result:{}",
                    warmupInstanceCount, result);
        }
        // if no instance need warmup, return originDynamicWeight
        if (warmupInstanceCount == 0) {
            return originDynamicWeight;
        } else {
            LOG.debug("[getInstanceWeightFromLosslessRule] warmup instance count:{}, result:{}",
                    warmupInstanceCount, result);
            return result;
        }
    }

    private int countNeedWarmupInstances(List<Instance> instanceList, LosslessProto.Warmup warmup, long currentTime) {
        int needWarmupInstanceCount = 0;
        for (Instance instance : instanceList) {
            Long createTime = instance.getCreateTime();
            if (createTime == null || createTime <= 0) {
                continue;
            }
            long uptime = Math.abs(TimeUnit.MILLISECONDS.toSeconds(currentTime - createTime));
            boolean isCompleted = uptime >= warmup.getIntervalSecond();
            if (!isCompleted) {
                needWarmupInstanceCount++;
            }

        }
        return needWarmupInstanceCount;
    }

    @Override
    public boolean realTimeAdjustDynamicWeight(InstanceGauge metric) {
        return false;
    }

    private InstanceWeight getInstanceWeight(Map<String, InstanceWeight> dynamicWeight,
            Instance instance, LosslessProto.Warmup warmup, long currentTime)  {

        InstanceWeight weight = new InstanceWeight();
        weight.setId(instance.getId());

        int baseWeight = instance.getWeight();
        if (dynamicWeight.containsKey(instance.getId())) {
            baseWeight = dynamicWeight.get(instance.getId()).getDynamicWeight();
        }
        weight.setBaseWeight(baseWeight);
        weight.setDynamicWeight(baseWeight);

        Long createTime = instance.getCreateTime();
        if (createTime == null || createTime <= 0) {
            return weight;
        }
        long uptime = Math.abs(TimeUnit.MILLISECONDS.toSeconds(currentTime - createTime));

        if (uptime > warmup.getIntervalSecond()) {
            return weight;
        }

        double ww = ((double) uptime / warmup.getIntervalSecond());
        double result = Math.ceil(Math.abs(Math.pow(ww, warmup.getCurvature()) * baseWeight));
        // if weight higher than base weight, return base weight.
        weight.setDynamicWeight(result > baseWeight ? baseWeight : (int) result);
        return weight;
    }
}
