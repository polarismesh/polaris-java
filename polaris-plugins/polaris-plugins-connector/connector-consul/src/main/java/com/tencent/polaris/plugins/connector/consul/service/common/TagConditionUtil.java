/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.connector.consul.service.common;

import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.specification.api.v1.model.ModelProto;

public class TagConditionUtil {

    public static ModelProto.MatchString.MatchStringType parseMatchStringType(TagCondition tagCondition) {
        return parseMatchStringType(tagCondition.getTagOperator());
    }

    public static ModelProto.MatchString.MatchStringType parseMatchStringType(String tagOperator) {
        if (StringUtils.equals(tagOperator, TagConstant.OPERATOR.EQUAL)) {
            // 匹配关系  等于
            return ModelProto.MatchString.MatchStringType.EXACT;
        } else if (StringUtils.equals(tagOperator, TagConstant.OPERATOR.NOT_EQUAL)) {
            // 匹配关系  不等于
            return ModelProto.MatchString.MatchStringType.NOT_EQUALS;
        } else if (StringUtils.equals(tagOperator, TagConstant.OPERATOR.IN)) {
            return ModelProto.MatchString.MatchStringType.IN;
        } else if (StringUtils.equals(tagOperator, TagConstant.OPERATOR.NOT_IN)) {
            return ModelProto.MatchString.MatchStringType.NOT_IN;
        } else if (StringUtils.equals(tagOperator, TagConstant.OPERATOR.REGEX)) {
            return ModelProto.MatchString.MatchStringType.REGEX;
        } else {
            return ModelProto.MatchString.MatchStringType.REGEX;
        }
    }

    public static String parseMetadataKey(String originalKey) {
        if (StringUtils.equals(originalKey, TagConstant.SYSTEM_FIELD.SOURCE_APPLICATION_ID)) {
            // 系统标签  应用ID
            return RuleUtils.CALLEE_APPLICATION_METADATA_PREFIX + TsfMetadataConstants.TSF_APPLICATION_ID;
        } else if (StringUtils.equals(originalKey, TagConstant.SYSTEM_FIELD.SOURCE_GROUP_ID)) {
            // 系统标签  部署组ID
            return RuleUtils.CALLEE_APPLICATION_METADATA_PREFIX + TsfMetadataConstants.TSF_GROUP_ID;
        } else if (StringUtils.equals(originalKey, TagConstant.SYSTEM_FIELD.SOURCE_CONNECTION_IP)) {
            // 系统标签  发起方本地IP
            return MessageMetadataContainer.LABEL_KEY_CALLER_IP;
        } else if (StringUtils.equals(originalKey, TagConstant.SYSTEM_FIELD.SOURCE_APPLICATION_VERSION)) {
            // 系统标签  包版本
            return RuleUtils.CALLEE_APPLICATION_METADATA_PREFIX + TsfMetadataConstants.TSF_PROG_VERSION;
        } else if (StringUtils.equals(originalKey, TagConstant.SYSTEM_FIELD.DESTINATION_INTERFACE)) {
            // 系统标签  被调用方API
            return MessageMetadataContainer.LABEL_KEY_PATH;
        } else if (StringUtils.equals(originalKey, TagConstant.SYSTEM_FIELD.REQUEST_HTTP_METHOD)) {
            // 系统标签 HTTP METHOD
            return MessageMetadataContainer.LABEL_KEY_METHOD;
        }
        return originalKey;
    }
}
