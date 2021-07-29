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

package com.tencent.polaris.plugins.stat.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SignatureUtil {
    public static final byte SEPARATOR_BYTE = -1; //255

    static long emptyLabelSignature = FnvHash.hashNew();

    public static long labelsToSignature(Map<String, String> labels) {
        if (labels == null || labels.size() == 0) {
            return emptyLabelSignature;
        }

        List<String> labelNames = new ArrayList<>(labels.keySet());
        Collections.sort(labelNames);

        long sum = FnvHash.hashNew();
        for (String labelName : labelNames) {
            sum = FnvHash.hashAdd(sum, labelName);
            sum = FnvHash.hashAddByte(sum, SEPARATOR_BYTE);
            sum = FnvHash.hashAdd(sum, labels.get(labelName));
            sum = FnvHash.hashAddByte(sum, SEPARATOR_BYTE);
        }
        return sum;
    }
}
