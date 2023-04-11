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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;

public class HealthCheckUtils {

    /**
     * instance expire interval = multiple * check expire period
     */
    public static int CHECK_PERIOD_MULTIPLE = 3;

    /**
     * default check expire period
     */
    public static int DEFAULT_CHECK_INTERVAL = 60 * 1000;

    /**
     * parse protocol string to enum
     *
     * @param protocol protocol string
     * @return enum
     */
    public static Protocol parseProtocol(String protocol) {
        protocol = StringUtils.defaultString(protocol).toLowerCase();
        if (protocol.equals("http") || protocol.startsWith("http/") || protocol.endsWith("/http")) {
            return Protocol.HTTP;
        }
        if (protocol.equals("udp") || protocol.startsWith("udp/") || protocol.endsWith("/udp")) {
            return Protocol.UDP;
        }
        return Protocol.UNKNOWN;
    }

}
