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

package com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity;


import com.tencent.polaris.api.utils.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author zhixinzxliu
 */
public class CircuitBreakerRule implements Serializable {

    /**
     * 空构造函数
     */
    public CircuitBreakerRule() {
    }

    /**
     * 熔断规则主键，全局唯一
     */
    private String ruleId;

    /**
     * 熔断规则微服务ID
     */
    private String microserviceId;

    /**
     * microserviceId 微服务服务名称
     */
    private String microserviceName;

    /**
     * TAG 熔断规则详情
     */
    private List<CircuitBreakerStrategy> strategyList;

    /**
     * microserviceId 微服务所属命名空间id
     */
    private String namespaceId;

    private String updateTime;

    private String targetServiceName;

    private String targetNamespaceId;

    private TsfCircuitBreakerIsolationLevelEnum isolationLevel;
    // 生效实际，判断这个熔断规则，是否关闭后又打开
    private long effectTime;

    public TsfCircuitBreakerIsolationLevelEnum getIsolationLevel() {
        return isolationLevel;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public void setIsolationLevel(TsfCircuitBreakerIsolationLevelEnum isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public String getTargetServiceName() {
        return targetServiceName;
    }

    public void setTargetServiceName(String targetServiceName) {
        this.targetServiceName = targetServiceName;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getMicroserviceId() {
        return microserviceId;
    }

    public void setMicroserviceId(String microserviceId) {
        this.microserviceId = microserviceId;
    }

    public List<CircuitBreakerStrategy> getStrategyList() {
        return strategyList;
    }

    public void setStrategyList(List<CircuitBreakerStrategy> strategyList) {
        this.strategyList = strategyList;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getMicroserviceName() {
        return microserviceName;
    }

    public void setMicroserviceName(String microserviceName) {
        this.microserviceName = microserviceName;
    }

    public String getTargetNamespaceId() {
        return targetNamespaceId;
    }

    public void setTargetNamespaceId(String targetNamespaceId) {
        this.targetNamespaceId = targetNamespaceId;
    }

    public long getEffectTime() {
        return effectTime;
    }

    public void setEffectTime(long effectTime) {
        this.effectTime = effectTime;
    }

    public boolean validate() {
        if (isolationLevel == null) {
            return false;
        }

        if (StringUtils.isBlank(targetNamespaceId) || StringUtils.isBlank(targetServiceName)) {
            return false;
        }

        if (TsfCircuitBreakerIsolationLevelEnum.INSTANCE != isolationLevel &&
                TsfCircuitBreakerIsolationLevelEnum.API != isolationLevel &&
                TsfCircuitBreakerIsolationLevelEnum.SERVICE != isolationLevel) {
            return false;
        }

        if (getStrategyList() == null || getStrategyList().size() < 1) {
            if (isolationLevel != TsfCircuitBreakerIsolationLevelEnum.API) {
                // 如果是Instance和Service级别，放入默认的strategy
                this.strategyList = new ArrayList<>();
                this.strategyList.add(new CircuitBreakerStrategy());
            } else {
                return false;
            }
        }

        // 在实例级别和服务级别，策略有且只能有一个
        if (isolationLevel == TsfCircuitBreakerIsolationLevelEnum.INSTANCE || isolationLevel == TsfCircuitBreakerIsolationLevelEnum.SERVICE) {
            if (getStrategyList().size() != 1 || !getStrategyList().get(0).validate()) {
                return false;
            }
        }

        if (isolationLevel == TsfCircuitBreakerIsolationLevelEnum.API) {
            for (CircuitBreakerStrategy strategy : strategyList) {
                if (strategy.getApiList() == null || strategy.getApiList().isEmpty()) {
                    return false;
                }
            }
        }

        for (CircuitBreakerStrategy strategy : getStrategyList()) {
            if (!strategy.validate()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        // 理论上 strategyList 也要比较，但这样需要比较很多东西，暂定比较 nid、service name、level、updateTime 以及 effectTime
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircuitBreakerRule that = (CircuitBreakerRule) o;
        return Objects.equals(microserviceName, that.microserviceName) && Objects.equals(namespaceId, that.namespaceId)
                && Objects.equals(updateTime, that.updateTime) && Objects.equals(targetServiceName, that.targetServiceName)
                && Objects.equals(targetNamespaceId, that.targetNamespaceId) && isolationLevel == that.isolationLevel
                && effectTime == that.effectTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(microserviceName, namespaceId, updateTime, targetServiceName, targetNamespaceId, isolationLevel, effectTime);
    }

    /**
     * 包括 strategyList
     *
     * @return
     */
    public String toFullString() {
        return "CircuitBreakerRule{" +
                "ruleId='" + ruleId + '\'' +
                ", microserviceId='" + microserviceId + '\'' +
                ", strategyList=" + strategyList +
                ", namespaceId='" + namespaceId + '\'' +
                ", microserviceName='" + microserviceName + '\'' +
                ", targetServiceName='" + targetServiceName + '\'' +
                ", targetNamespaceId='" + targetNamespaceId + '\'' +
                ", isolationLevel=" + isolationLevel +
                ", updateTime=" + updateTime +
                ", effectTime=" + effectTime +
                '}';
    }

    @Override
    public String toString() {
        return "CircuitBreakerRule{" +
                "ruleId='" + ruleId + '\'' +
                ", microserviceId='" + microserviceId + '\'' +
                ", namespaceId='" + namespaceId + '\'' +
                ", microserviceName='" + microserviceName + '\'' +
                ", targetServiceName='" + targetServiceName + '\'' +
                ", targetNamespaceId='" + targetNamespaceId + '\'' +
                ", isolationLevel=" + isolationLevel +
                ", updateTime=" + updateTime +
                ", effectTime=" + effectTime +
                '}';
    }
}
