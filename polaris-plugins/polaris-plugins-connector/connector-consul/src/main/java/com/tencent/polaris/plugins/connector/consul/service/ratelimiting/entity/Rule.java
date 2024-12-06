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

package com.tencent.polaris.plugins.connector.consul.service.ratelimiting.entity;


import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.plugins.connector.consul.service.common.TagRule;

import java.util.Objects;

public class Rule {

    private String id;

    /**
     * 周期长度，以秒为单位
     */
    private Integer duration;

    /**
     * 服务总的配额
     */
    private Integer totalQuota;

    /**
     * 分配给当前实例的配额，null 时表示仍未拿到配额
     */
    private Integer instanceQuota;
    private Mode mode;
    private String limitedResponse;
    private Condition condition;
    private Integer concurrentThreads;
    private Type type;
    private TagRule tagRule;

    public Rule(String id, Integer duration, Integer totalQuota, Integer instanceQuota, Mode mode, String limitedResponse, Condition condition, Integer concurrentThreads, Type type, TagRule tagRule) {
        this.id = id;
        this.duration = duration;
        this.totalQuota = totalQuota;
        this.instanceQuota = instanceQuota;
        this.mode = mode;
        this.limitedResponse = limitedResponse;
        this.condition = condition;
        this.concurrentThreads = concurrentThreads;
        this.type = type;
        this.tagRule = tagRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(getId(), rule.getId())
                && Objects.equals(getDuration(), rule.getDuration())
                && Objects.equals(getTotalQuota(), rule.getTotalQuota())
                && Objects.equals(getInstanceQuota(), rule.getInstanceQuota())
                && getMode() == rule.getMode()
                && Objects.equals(getLimitedResponse(), rule.getLimitedResponse())
                && getCondition() == rule.getCondition()
                && Objects.equals(getConcurrentThreads(), rule.getConcurrentThreads())
                && getType() == rule.getType()
                && Objects.equals(getTagRule(), rule.getTagRule());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, duration, totalQuota, instanceQuota, mode, limitedResponse, condition, concurrentThreads, type, tagRule);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public TagRule getTagRule() {
        return tagRule;
    }

    public void setTagRule(TagRule tagRule) {
        this.tagRule = tagRule;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(Integer totalQuota) {
        this.totalQuota = totalQuota;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getInstanceQuota() {
        return instanceQuota;
    }

    public void setInstanceQuota(Integer instanceQuota) {
        this.instanceQuota = instanceQuota;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getLimitedResponse() {
        return limitedResponse;
    }

    public void setLimitedResponse(String limitedResponse) {
        this.limitedResponse = limitedResponse;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Integer getConcurrentThreads() {
        return concurrentThreads;
    }

    public void setConcurrentThreads(Integer concurrentThreads) {
        this.concurrentThreads = concurrentThreads;
    }

    public boolean isSameDuration(Rule other) {
        return Objects.equals(this.duration, other.duration);
    }

    public boolean isSameTagRule(Rule other) {
        if (this.tagRule != other.tagRule) {
            if (this.tagRule == null || other.tagRule == null) {
                return false;
            }
            if (this.tagRule.getConditions().size() != other.tagRule.getConditions().size()) {
                return false;
            }
            for (int i = 0; i < this.tagRule.getConditions().size(); i++) {
                if (!this.tagRule.getConditions().get(i).equals2(other.tagRule.getConditions().get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "id='" + id + '\'' +
                ", duration=" + duration +
                ", totalQuota=" + totalQuota +
                ", instanceQuota=" + instanceQuota +
                ", mode=" + mode +
                ", limitedResponse='" + limitedResponse + '\'' +
                ", condition=" + condition +
                ", concurrentThreads=" + concurrentThreads +
                ", type=" + type +
                ", tagRule=" + tagRule +
                '}';
    }

    public enum Type {
        GLOBAL, SOURCE_SERVICE, TAG_CONDITION
    }

    public enum Condition {
        QPS, THREAD, CPU;

        public static Condition getConditionByStr(String var) {
            if (StringUtils.isEmpty(var)) return QPS;
            for (Condition type : Condition.values()) {
                if (type.name().equalsIgnoreCase(var)) {
                    return type;
                }
            }
            return QPS;
        }
    }

    /**
     * 单机、集群
     */
    public enum Mode {
        CLUSTER, STANDALONE;

        public static Mode getModeByStr(String var) {
            if (StringUtils.isEmpty(var)) return CLUSTER;
            for (Mode type : Mode.values()) {
                if (type.name().equalsIgnoreCase(var)) {
                    return type;
                }
            }
            return CLUSTER;
        }
    }
}
