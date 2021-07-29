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

package com.tencent.polaris.api.plugin.route;

import com.tencent.polaris.api.pojo.Instance;
import java.util.List;

/**
 * 路由结果
 *
 * @author andrewshan
 * @date 2019/9/23
 */
public class RouteResult {

    private final List<Instance> instances;

    private final NextRouterInfo nextRouterInfo;

    private final int hashCode;

    public RouteResult(List<Instance> instances, State state) {
        this.instances = instances;
        nextRouterInfo = new NextRouterInfo(state);
        this.hashCode = 0;
    }

    public RouteResult(List<Instance> instances,
            NextRouterInfo nextRouterInfo, int hashCode) {
        this.instances = instances;
        this.nextRouterInfo = nextRouterInfo;
        this.hashCode = hashCode;
    }

    public int getHashCode() {
        return hashCode;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public NextRouterInfo getNextRouterInfo() {
        return nextRouterInfo;
    }

    /**
     * 路由链的下一步状态
     */
    public enum State {
        /**
         * 走下一个插件
         */
        Next,
        /**
         * 重试
         */
        Retry
    }

    /**
     * 下一步链路的信息
     */
    public static class NextRouterInfo {

        private final State state;

        private LocationLevel locationLevel;

        /**
         * 最小的存在实例的级别
         */
        private LocationLevel minAvailableLevel;

        public NextRouterInfo(State state) {
            this.state = state;
        }

        public State getState() {
            return state;
        }

        public LocationLevel getLocationLevel() {
            return locationLevel;
        }

        public void setLocationLevel(LocationLevel locationLevel) {
            this.locationLevel = locationLevel;
        }

        public LocationLevel getMinAvailableLevel() {
            return minAvailableLevel;
        }

        public void setMinAvailableLevel(LocationLevel minAvailableLevel) {
            this.minAvailableLevel = minAvailableLevel;
        }
    }
}