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

package com.tencent.polaris.api.utils;

import com.google.protobuf.StringValue;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.polaris.api.utils.RuleUtils.matchMetadata;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RuleUtils}.
 *
 * @author Haotian Zhang
 */
public class RuleUtilsTest {

    @Test
    public void testMatchMetadata() {
        // empty rule metadata
        Map<String, String> destMeta1 = new HashMap<>();
        destMeta1.put("k1", "v1");
        assertThat(matchMetadata(null, destMeta1)).isTrue();

        Map<String, String> destMeta2 = new HashMap<>();
        destMeta2.put("k2", "v2");
        assertThat(matchMetadata(null, destMeta2)).isTrue();

        Map<String, String> destMeta3 = new HashMap<>();
        destMeta3.put("k1", "v1");
        destMeta3.put("k2", "v2");
        assertThat(matchMetadata(null, destMeta3)).isTrue();

        // rule metadata with * key
        Map<String, ModelProto.MatchString> ruleMeta1 = new HashMap<>();
        ruleMeta1.put("*", ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue("v1").build())
                .setTypeValue(ModelProto.MatchString.ValueType.TEXT_VALUE).build());
        ruleMeta1.put("k2", ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue("v2").build())
                .setTypeValue(ModelProto.MatchString.ValueType.TEXT_VALUE).build());

        assertThat(matchMetadata(ruleMeta1, destMeta1)).isTrue();

        assertThat(matchMetadata(ruleMeta1, destMeta2)).isTrue();

        assertThat(matchMetadata(ruleMeta1, destMeta3)).isTrue();

        // rule metadata without * key
        Map<String, ModelProto.MatchString> ruleMeta2 = new HashMap<>();
        ruleMeta2.put("k1", ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue("v1").build())
                .setTypeValue(ModelProto.MatchString.ValueType.TEXT_VALUE).build());
        ruleMeta2.put("k2", ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue("v2").build())
                .setTypeValue(ModelProto.MatchString.ValueType.TEXT_VALUE).build());

        assertThat(matchMetadata(ruleMeta2, null)).isFalse();

        assertThat(matchMetadata(ruleMeta2, destMeta1)).isFalse();

        assertThat(matchMetadata(ruleMeta2, destMeta2)).isFalse();

        assertThat(matchMetadata(ruleMeta2, destMeta3)).isTrue();

        // rule metadata with * value
        Map<String, ModelProto.MatchString> ruleMeta3 = new HashMap<>();
        ruleMeta3.put("k1", ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue("v1").build())
                .setTypeValue(ModelProto.MatchString.ValueType.TEXT_VALUE).build());
        ruleMeta3.put("k2", ModelProto.MatchString.newBuilder()
                .setType(ModelProto.MatchString.MatchStringType.EXACT)
                .setValue(StringValue.newBuilder().setValue("*").build())
                .setTypeValue(ModelProto.MatchString.ValueType.TEXT_VALUE).build());

        assertThat(matchMetadata(ruleMeta3, null)).isFalse();

        assertThat(matchMetadata(ruleMeta3, destMeta1)).isTrue();

        assertThat(matchMetadata(ruleMeta3, destMeta2)).isFalse();

        assertThat(matchMetadata(ruleMeta3, destMeta3)).isTrue();
    }
}
