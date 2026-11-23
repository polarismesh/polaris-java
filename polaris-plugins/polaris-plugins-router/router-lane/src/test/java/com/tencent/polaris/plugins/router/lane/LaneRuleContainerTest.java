/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.router.lane;

import com.google.protobuf.Any;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Test for {@link LaneRuleContainer}.
 *
 * @author fishtailfu
 */
@RunWith(MockitoJUnitRunner.class)
public class LaneRuleContainerTest {

    private static final String NAMESPACE = "Test";

    private static final String CALLER_SERVICE = "CallerService";

    private static final String CALLEE_SERVICE = "CalleeService";

    private static final String TSF_GROUP = "tsf";

    private static final String NORMAL_GROUP = "lane-group";

    private static final String OLD_CTIME = "2024-01-01 00:00:00";

    private static final String NEW_CTIME = "2024-06-01 00:00:00";

    @Mock
    private MetadataContext manager;

    private ServiceKey caller;

    @Before
    public void setUp() {
        caller = new ServiceKey(NAMESPACE, CALLER_SERVICE);
    }

    /**
     * 测试目的：验证 TSF 场景下同优先级泳道规则以新规则优先。
     * 测试场景：泳道组名称为 tsf，组内两条规则优先级相同但创建时间不同。
     * 验证内容：规则列表按创建时间倒序排列，新规则排在最前。
     */
    @Test
    public void testNewerRuleFirstInTsfScenario() throws Exception {
        LaneProto.LaneRule olderRule = buildRule(TSF_GROUP, "older-rule", 10, OLD_CTIME);
        LaneProto.LaneRule newerRule = buildRule(TSF_GROUP, "newer-rule", 10, NEW_CTIME);
        LaneProto.LaneGroup group = buildGroup(TSF_GROUP, Arrays.asList(olderRule, newerRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(group));

        Assertions.assertThat(ruleNames(container)).containsExactly("newer-rule", "older-rule");
    }

    /**
     * 测试目的：验证非 TSF 场景下仍保持旧规则优先的历史行为。
     * 测试场景：泳道组名称不是 tsf，组内两条规则优先级相同但创建时间不同。
     * 验证内容：规则列表按创建时间正序排列，旧规则排在最前。
     */
    @Test
    public void testOlderRuleFirstInNonTsfScenario() throws Exception {
        LaneProto.LaneRule olderRule = buildRule(NORMAL_GROUP, "older-rule", 10, OLD_CTIME);
        LaneProto.LaneRule newerRule = buildRule(NORMAL_GROUP, "newer-rule", 10, NEW_CTIME);
        LaneProto.LaneGroup group = buildGroup(NORMAL_GROUP, Arrays.asList(olderRule, newerRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(group));

        Assertions.assertThat(ruleNames(container)).containsExactly("older-rule", "newer-rule");
    }

    /**
     * 测试目的：验证 tsf 泳道组只作为排序开关，规则本身可以属于其他泳道组。
     * 测试场景：存在名为 tsf 的泳道组，待匹配的同优先级规则来自另一个泳道组。
     * 验证内容：tsf 组触发新规则优先，低优先级规则仍排在后面。
     */
    @Test
    public void testTsfGroupSwitchesOrderForOtherGroups() throws Exception {
        LaneProto.LaneRule tsfRule = buildRule(TSF_GROUP, "tsf-rule", 20, OLD_CTIME);
        LaneProto.LaneRule olderRule = buildRule(NORMAL_GROUP, "older-rule", 10, OLD_CTIME);
        LaneProto.LaneRule newerRule = buildRule(NORMAL_GROUP, "newer-rule", 10, NEW_CTIME);
        LaneProto.LaneGroup tsfGroup = buildGroup(TSF_GROUP, Arrays.asList(tsfRule), false);
        LaneProto.LaneGroup normalGroup = buildGroup(NORMAL_GROUP, Arrays.asList(olderRule, newerRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(tsfGroup, normalGroup));

        Assertions.assertThat(ruleNames(container)).containsExactly("newer-rule", "older-rule", "tsf-rule");
    }

    /**
     * 测试目的：验证 TSF 场景下优先级仍然优先于创建时间。
     * 测试场景：tsf 组内两条规则优先级不同，且高优先级规则创建时间更早。
     * 验证内容：高优先级规则排在前面，创建时间不参与比较。
     */
    @Test
    public void testPriorityTakesPrecedenceInTsfScenario() throws Exception {
        LaneProto.LaneRule highPriorityRule = buildRule(TSF_GROUP, "high-priority-rule", 5, OLD_CTIME);
        LaneProto.LaneRule lowPriorityRule = buildRule(TSF_GROUP, "low-priority-rule", 10, NEW_CTIME);
        LaneProto.LaneGroup group = buildGroup(TSF_GROUP, Arrays.asList(highPriorityRule, lowPriorityRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(group));

        Assertions.assertThat(ruleNames(container)).containsExactly("high-priority-rule", "low-priority-rule");
    }

    /**
     * 测试目的：验证 TSF 场景下主调泳道入口规则仍优先于创建时间。
     * 测试场景：入口泳道组的旧规则与非入口 tsf 泳道组的新规则优先级相同。
     * 验证内容：入口规则排在前面，新规则优先只在同层比较时生效。
     */
    @Test
    public void testTrafficEntryRuleTakesPrecedenceInTsfScenario() throws Exception {
        LaneProto.LaneRule entryRule = buildRule(NORMAL_GROUP, "entry-rule", 10, OLD_CTIME);
        LaneProto.LaneRule newerRule = buildRule(TSF_GROUP, "newer-rule", 10, NEW_CTIME);
        LaneProto.LaneGroup entryGroup = buildGroup(NORMAL_GROUP, Arrays.asList(entryRule), true);
        LaneProto.LaneGroup tsfGroup = buildGroup(TSF_GROUP, Arrays.asList(newerRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(entryGroup, tsfGroup));

        Assertions.assertThat(ruleNames(container)).containsExactly("entry-rule", "newer-rule");
    }

    /**
     * 测试目的：验证排序结果会体现在无染色流量的规则匹配上。
     * 测试场景：tsf 组存在新旧两条同优先级规则，流量未染色且规则无匹配条件。
     * 验证内容：matchRule 返回排序后的第一条规则，即创建时间最新的规则。
     */
    @Test
    public void testMatchRuleReturnsNewestRuleInTsfScenario() {
        LaneProto.LaneRule olderRule = buildRule(TSF_GROUP, "older-rule", 10, OLD_CTIME);
        LaneProto.LaneRule newerRule = buildRule(TSF_GROUP, "newer-rule", 10, NEW_CTIME);
        LaneProto.LaneGroup group = buildGroup(TSF_GROUP, Arrays.asList(olderRule, newerRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(group));

        Optional<LaneProto.LaneRule> matchedRule = container.matchRule(manager);
        Assertions.assertThat(matchedRule).isPresent();
        Assertions.assertThat(matchedRule.get().getName()).isEqualTo("newer-rule");
    }

    /**
     * 测试目的：验证未启用的规则不参与排序与匹配。
     * 测试场景：tsf 组内包含一条未启用但创建时间最新的规则。
     * 验证内容：规则列表中只剩启用规则，且仍按创建时间倒序排列。
     */
    @Test
    public void testDisabledRuleIsSkippedInTsfScenario() throws Exception {
        LaneProto.LaneRule olderRule = buildRule(TSF_GROUP, "older-rule", 10, OLD_CTIME);
        LaneProto.LaneRule disabledRule = buildRule(TSF_GROUP, "disabled-rule", 10, NEW_CTIME);
        disabledRule = disabledRule.toBuilder().setEnable(false).build();
        LaneProto.LaneGroup group = buildGroup(TSF_GROUP, Arrays.asList(disabledRule, olderRule), false);

        LaneRuleContainer container = new LaneRuleContainer(manager, caller, Arrays.asList(group));

        Assertions.assertThat(ruleNames(container)).containsExactly("older-rule");
    }

    private LaneProto.LaneRule buildRule(String groupName, String ruleName, int priority, String ctime) {
        LaneProto.LaneRule.Builder builder = LaneProto.LaneRule.newBuilder();
        builder.setName(ruleName);
        builder.setGroupName(groupName);
        builder.setEnable(true);
        builder.setPriority(priority);
        builder.setCtime(ctime);
        builder.setDefaultLabelValue(groupName);
        return builder.build();
    }

    private LaneProto.LaneGroup buildGroup(String groupName, List<LaneProto.LaneRule> rules, boolean trafficEntry) {
        LaneProto.LaneGroup.Builder builder = LaneProto.LaneGroup.newBuilder();
        builder.setName(groupName);
        RoutingProto.DestinationGroup.Builder destination = RoutingProto.DestinationGroup.newBuilder();
        destination.setNamespace(NAMESPACE);
        destination.setService(CALLEE_SERVICE);
        builder.addDestinations(destination);
        if (trafficEntry) {
            LaneProto.ServiceSelector.Builder selector = LaneProto.ServiceSelector.newBuilder();
            selector.setNamespace(NAMESPACE);
            selector.setService(CALLER_SERVICE);
            LaneProto.TrafficEntry.Builder entry = LaneProto.TrafficEntry.newBuilder();
            entry.setType("polarismesh.cn/service");
            entry.setSelector(Any.pack(selector.build()));
            builder.addEntries(entry);
        }
        builder.addAllRules(rules);
        return builder.build();
    }

    private List<String> ruleNames(LaneRuleContainer container) throws Exception {
        Field field = LaneRuleContainer.class.getDeclaredField("rules");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<LaneProto.LaneRule> rules = (List<LaneProto.LaneRule>) field.get(container);
        List<String> names = new ArrayList<>();
        for (LaneProto.LaneRule rule : rules) {
            names.add(rule.getName());
        }
        return names;
    }
}
