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

package com.tencent.polaris.plugins.router.nearby;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;

/**
 * 就近路由配置结构
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public class NearbyRouterConfig implements Verifier {

    @JsonProperty
    private Boolean enableReportLocalAddress;

    @JsonProperty
    private RoutingProto.NearbyRoutingConfig.LocationLevel matchLevel;

    @JsonProperty
    private RoutingProto.NearbyRoutingConfig.LocationLevel maxMatchLevel;

    @JsonProperty
    private Boolean strictNearby;

    @JsonProperty
    private Boolean enableDegradeByUnhealthyPercent;

    @JsonProperty
    private Integer unhealthyPercentToDegrade;

    public RoutingProto.NearbyRoutingConfig.LocationLevel getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(RoutingProto.NearbyRoutingConfig.LocationLevel matchLevel) {
        this.matchLevel = matchLevel;
    }

    public RoutingProto.NearbyRoutingConfig.LocationLevel getMaxMatchLevel() {
        return maxMatchLevel;
    }

    public void setMaxMatchLevel(RoutingProto.NearbyRoutingConfig.LocationLevel maxMatchLevel) {
        this.maxMatchLevel = maxMatchLevel;
    }

    public Boolean isStrictNearby() {
        return strictNearby;
    }

    public void setStrictNearby(Boolean strictNearby) {
        this.strictNearby = strictNearby;
    }

    public Boolean isEnableDegradeByUnhealthyPercent() {
        return enableDegradeByUnhealthyPercent;
    }

    public void setEnableDegradeByUnhealthyPercent(Boolean enableDegradeByUnhealthyPercent) {
        this.enableDegradeByUnhealthyPercent = enableDegradeByUnhealthyPercent;
    }

    public Integer getUnhealthyPercentToDegrade() {
        return unhealthyPercentToDegrade;
    }

    public void setUnhealthyPercentToDegrade(Integer unhealthyPercentToDegrade) {
        this.unhealthyPercentToDegrade = unhealthyPercentToDegrade;
    }

    public Boolean isEnableReportLocalAddress() {
        return enableReportLocalAddress;
    }

    public void setEnableReportLocalAddress(Boolean enableReportLocalAddress) {
        this.enableReportLocalAddress = enableReportLocalAddress;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enableReportLocalAddress, "enableReportLocalAddress");
        ConfigUtils.validateNull(matchLevel, "matchLevel");
        ConfigUtils.validateNull(maxMatchLevel, "matchLevel");
        if (matchLevel.ordinal() > maxMatchLevel.ordinal()) {
            throw new IllegalArgumentException("matchLevel should smaller than maxMatchLevel");
        }
        ConfigUtils.validateNull(strictNearby, "strictNearby");
        ConfigUtils.validateNull(enableDegradeByUnhealthyPercent, "enableDegradeByUnhealthyPercent");
        ConfigUtils.validatePositive(unhealthyPercentToDegrade, "unhealthyPercentToDegrade");
        if (unhealthyPercentToDegrade > 100) {
            throw new IllegalArgumentException("unhealthyPercentToDegrade should less than or equals 100");
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            NearbyRouterConfig nearbyRouterConfig = (NearbyRouterConfig) defaultObject;
            if (null == enableReportLocalAddress) {
                setEnableReportLocalAddress(nearbyRouterConfig.isEnableReportLocalAddress());
            }
            if (null == matchLevel) {
                setMatchLevel(nearbyRouterConfig.getMatchLevel());
            }
            if (null == maxMatchLevel) {
                setMaxMatchLevel(nearbyRouterConfig.getMaxMatchLevel());
            }
            if (null == strictNearby) {
                setStrictNearby(nearbyRouterConfig.isStrictNearby());
            }
            if (null == enableDegradeByUnhealthyPercent) {
                setEnableDegradeByUnhealthyPercent(nearbyRouterConfig.isEnableDegradeByUnhealthyPercent());
            }
            if (null == unhealthyPercentToDegrade) {
                setUnhealthyPercentToDegrade(nearbyRouterConfig.getUnhealthyPercentToDegrade());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "NearbyRouterConfig{" +
                "enableReportLocalAddress=" + enableReportLocalAddress +
                ", matchLevel='" + matchLevel + '\'' +
                ", maxMatchLevel='" + maxMatchLevel + '\'' +
                ", strictNearby=" + strictNearby +
                ", enableDegradeByUnhealthyPercent=" + enableDegradeByUnhealthyPercent +
                ", unhealthyPercentToDegrade=" + unhealthyPercentToDegrade +
                '}';
    }
}
