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

package com.tencent.polaris.ratelimit.client.sync.tsf;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Junit for {@link TsfRateLimitMasterUtils}.
 *
 * @author Haotian Zhang
 */
public class TsfRateLimitMasterUtilsTest {

    @Test
    public void testSerializeToJson() {
        List<ReportRequest.RuleStatics> ruleStaticsList = new ArrayList<>();
        ruleStaticsList.add(new ReportRequest.RuleStatics("rule-abcd1234", 1, 2));
        ReportRequest request = new ReportRequest(ruleStaticsList);

        String postBody = TsfRateLimitMasterUtils.serializeToJson(request);
        assertThat(postBody).isEqualTo("{\"rates\":[{\"rule_id\":\"rule-abcd1234\",\"pass\":1,\"block\":2}]}");
    }
}
