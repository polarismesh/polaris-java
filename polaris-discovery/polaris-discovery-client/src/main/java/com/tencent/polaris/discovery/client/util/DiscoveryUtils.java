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

package com.tencent.polaris.discovery.client.util;

import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utils for discovery.
 *
 * @author Haotian Zhang
 */
public class DiscoveryUtils {

    public static ServiceInstances generateIpv6ServiceInstances(ServiceInstances serviceInstances) {
        ServiceKey serviceKey = serviceInstances.getServiceKey();
        Map<String, String> serviceMetadata = serviceInstances.getMetadata();

        List<Instance> instanceList = serviceInstances.getInstances();
        List<Instance> finalInstanceList = new ArrayList<>();
        for (Instance instance : instanceList) {
            if (checkIpv6Instance(instance)) {
                finalInstanceList.add(new InstanceWrap(instance, true));
            }
        }
        return new DefaultServiceInstances(serviceKey, finalInstanceList, serviceMetadata);
    }

    static boolean checkIpv6Instance(Instance instance) {
        if (MapUtils.isNotEmpty(instance.getMetadata())
                && instance.getMetadata().containsKey(MetadataConstants.ADDRESS_IPV6)
                && StringUtils.isNotBlank(instance.getMetadata().get(MetadataConstants.ADDRESS_IPV6))) {
            return true;
        }
        return IPAddressUtils.checkIpv6Host(instance.getHost());
    }
}
