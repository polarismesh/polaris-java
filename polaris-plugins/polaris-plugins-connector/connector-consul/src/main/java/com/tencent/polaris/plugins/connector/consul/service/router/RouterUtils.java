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

package com.tencent.polaris.plugins.connector.consul.service.router;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConstant;
import com.tencent.polaris.plugins.connector.consul.service.router.entity.RouteTag;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMatchStringType;
import static com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil.parseMetadataKey;

/**
 * @author Haotian Zhang
 */
public class RouterUtils {

    public static List<RoutingProto.Source> parseTagListToSourceList(List<RouteTag> tagList) {
        List<RoutingProto.Source> sources = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tagList)) {
            List<RoutingProto.Source.Builder> sourceBuilders = new ArrayList<>();
            List<RoutingProto.Source.Builder> metadataSourceBuilders = new ArrayList<>();
            for (RouteTag routeTag : tagList) {
                if (StringUtils.equals(routeTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_SERVICE_NAME)) {
                    String tagValue = routeTag.getTagValue();
                    if (StringUtils.isNotEmpty(tagValue)) {
                        RoutingProto.Source.Builder sourceBuilder = RoutingProto.Source.newBuilder();
                        sourceBuilder.setNamespace(StringValue.of("*"));
                        sourceBuilder.setService(StringValue.of(tagValue));

                        ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                        matchStringBuilder.setType(TagConditionUtil.parseMatchStringType(routeTag.getTagOperator()));
                        sourceBuilder.putMetadata(TsfMetadataConstants.TSF_SERVICE_TAG_OPERATOR, matchStringBuilder.build());

                        sourceBuilders.add(sourceBuilder);
                    }
                } else if (StringUtils.equals(routeTag.getTagField(), TagConstant.SYSTEM_FIELD.SOURCE_NAMESPACE_SERVICE_NAME)) {
                    String tagValue = routeTag.getTagValue();
                    if (StringUtils.isNotEmpty(tagValue)) {
                        String[] split = tagValue.split("/");
                        RoutingProto.Source.Builder sourceBuilder = RoutingProto.Source.newBuilder();
                        sourceBuilder.setNamespace(StringValue.of("*"));
                        String serviceName = tagValue;
                        if (split.length == 2) {
                            // namespace/service format
                            sourceBuilder.setNamespace(StringValue.of(split[0]));
                            serviceName = split[1];
                        }
                        sourceBuilder.setService(StringValue.of(serviceName));

                        ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                        matchStringBuilder.setType(TagConditionUtil.parseMatchStringType(routeTag.getTagOperator()));
                        sourceBuilder.putMetadata(TsfMetadataConstants.TSF_SERVICE_TAG_OPERATOR, matchStringBuilder.build());

                        sourceBuilders.add(sourceBuilder);
                    }
                } else {
                    RoutingProto.Source.Builder metadataSourceBuilder = RoutingProto.Source.newBuilder();
                    metadataSourceBuilder.setNamespace(StringValue.of("*"));
                    metadataSourceBuilder.setService(StringValue.of("*"));
                    ModelProto.MatchString.Builder matchStringBuilder = ModelProto.MatchString.newBuilder();
                    matchStringBuilder.setType(parseMatchStringType(routeTag));
                    matchStringBuilder.setValue(StringValue.of(routeTag.getTagValue()));
                    matchStringBuilder.setValueType(ModelProto.MatchString.ValueType.TEXT);
                    String metadataKey = routeTag.getTagField();
                    metadataSourceBuilder.putMetadata(parseMetadataKey(metadataKey), matchStringBuilder.build());
                    metadataSourceBuilders.add(metadataSourceBuilder);
                }
            }
            if (CollectionUtils.isNotEmpty(sourceBuilders)) {
                for (RoutingProto.Source.Builder sourceBuilder : sourceBuilders) {
                    for (RoutingProto.Source.Builder metadataSourceBuilder : metadataSourceBuilders) {
                        sourceBuilder.putAllMetadata(metadataSourceBuilder.getMetadataMap());
                    }
                    sources.add(sourceBuilder.build());
                }
            } else {
                RoutingProto.Source.Builder sourceBuilder = RoutingProto.Source.newBuilder();
                sourceBuilder.setNamespace(StringValue.of("*"));
                sourceBuilder.setService(StringValue.of("*"));
                for (RoutingProto.Source.Builder metadataSourceBuilder : metadataSourceBuilders) {
                    sourceBuilder.putAllMetadata(metadataSourceBuilder.getMetadataMap());
                }
                sources.add(sourceBuilder.build());
            }
        }
        return sources;
    }
}
