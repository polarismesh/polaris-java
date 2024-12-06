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

import com.google.protobuf.Any;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.tencent.polaris.client.util.Utils.isHealthyInstance;

/**
 * 就近接入路由
 *
 * @author vickliu
 * @date 2019/11/10
 */
public class NearbyRouter extends AbstractServiceRouter implements PluginConfigProvider {

    public static final String ROUTER_TYPE_NEAR_BY = "nearByRoute";
    public static final String ROUTER_ENABLED = "enabled";
    public static final String ROUTER_METADATA_KEY_ZONE = "zone";
    public static final String ROUTER_METADATA_KEY_REGION = "region";
    public static final String ROUTER_METADATA_KEY_CAMPUS = "campus";

    private static final String NEARBY_METADATA_ENABLE = "internal-enable-nearby";

    private static final Logger LOG = LoggerFactory.getLogger(NearbyRouter.class);

    private static final RoutingProto.NearbyRoutingConfig.LocationLevel defaultMinLevel = RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE;
    /**
     * 主调的地域信息
     */
    private final AtomicReference<Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String>> locationInfo = new AtomicReference<>();
    /**
     * 等待地域信息就绪的超时时间
     */
    long locationReadyTimeout;
    private ValueContext valueContext;

    private final AtomicBoolean firstRoute = new AtomicBoolean(true);

    /**
     * # 默认就近区域：默认城市 matchLevel: zone # 最大就近区域，默认为空（全匹配） maxMatchLevel: all #
     * 假如开启了严格就近，插件的初始化会等待地域信息获取成功才返回，假如获取失败（server获取失败或者IP地域信息缺失），则会初始化失败，而且必须按照 strictNearby: false #
     * 是否启用按服务不健康实例比例进行降级 enableDegradeByUnhealthyPercent: true，假如不启用，则不会降级#
     * 需要进行降级的实例比例，不健康实例达到百分之多少才进行降级。值(0, 100]。 # 默认100，即全部不健康才进行切换。
     */
    private NearbyRouterConfig config;
    /**
     * 降级的剩余健康比例
     */
    private double healthyPercentToDegrade;

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances serviceInstances)
            throws PolarisException {
        if (firstRoute.compareAndSet(true, false)) {
            refreshLocationInfo();
        }

        ServiceRule serviceRule = getServiceRule(serviceInstances.getNamespace(), serviceInstances.getService());
        RoutingProto.NearbyRoutingConfig nearbyRoutingConfig = null;
        if (serviceRule != null && serviceRule.getRule() != null) {
            ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) serviceRule.getRule();
            List<RoutingProto.RouteRule> nearByRouteRuleList = discoverResponse.getNearbyRouteRulesList();
            if (CollectionUtils.isNotEmpty(nearByRouteRuleList)) {
                Any any = null;
                for (RoutingProto.RouteRule routeRule : nearByRouteRuleList) {
                    if (routeRule.getEnable()) {
                        any = routeRule.getRoutingConfig();
                        break;
                    }
                }
                if (any != null) {
                    try {
                        nearbyRoutingConfig = any.unpack(RoutingProto.NearbyRoutingConfig.class);
                    } catch (Exception e) {
                        LOG.warn("{} cannot be unpacked to an instance of RoutingProto.NearbyRoutingConfig", any);
                    }
                }
            }
        }

        //先获取最低可用就近级别
        RoutingProto.NearbyRoutingConfig.LocationLevel minAvailableLevel = config.getMatchLevel();
        if (null == minAvailableLevel) {
            minAvailableLevel = defaultMinLevel;
        }
        if (nearbyRoutingConfig != null && nearbyRoutingConfig.getMatchLevel() != RoutingProto.NearbyRoutingConfig.LocationLevel.UNKNOWN) {
            minAvailableLevel = nearbyRoutingConfig.getMatchLevel();
        }
        RoutingProto.NearbyRoutingConfig.LocationLevel minLevel = minAvailableLevel;
        if (null != routeInfo.getNextRouterInfo()) {
            if (null != routeInfo.getNextRouterInfo().getLocationLevel()) {
                minLevel = routeInfo.getNextRouterInfo().getLocationLevel();
            }
            if (null != routeInfo.getNextRouterInfo().getMinAvailableLevel()) {
                minAvailableLevel = routeInfo.getNextRouterInfo().getMinAvailableLevel();
            }
        }
        RoutingProto.NearbyRoutingConfig.LocationLevel maxLevel = config.getMaxMatchLevel();
        if (null == maxLevel) {
            maxLevel = RoutingProto.NearbyRoutingConfig.LocationLevel.ALL;
        }
        if (nearbyRoutingConfig != null && nearbyRoutingConfig.getMaxMatchLevel() != RoutingProto.NearbyRoutingConfig.LocationLevel.UNKNOWN) {
            maxLevel = nearbyRoutingConfig.getMaxMatchLevel();
        }

        Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String> clientLocationInfo = getLocationInfo();
        if (minLevel.ordinal() >= maxLevel.ordinal()) {
            List<Instance> instances = selectInstances(serviceInstances, minAvailableLevel, clientLocationInfo);
            if (CollectionUtils.isEmpty(instances)) {
                throw new PolarisException(ErrorCode.LOCATION_MISMATCH,
                        String.format("can not find any instance by level %s", minLevel.name()));
            }
            //已经循环了一圈
            return new RouteResult(instances, RouteResult.State.Next);
        }
        CheckResult checkResult = new CheckResult();
        for (int i = minLevel.ordinal(); i <= maxLevel.ordinal(); i++) {
            RoutingProto.NearbyRoutingConfig.LocationLevel curLevel = RoutingProto.NearbyRoutingConfig.LocationLevel.values()[i];
            checkResult = hasHealthyInstances(serviceInstances, curLevel,
                    clientLocationInfo);
            checkResult.curLevel = curLevel;
            if (!CollectionUtils.isEmpty(checkResult.instances)) {
                break;
            } else {
                minAvailableLevel = curLevel;
            }
        }
        if (CollectionUtils.isEmpty(checkResult.instances)) {
            throw new PolarisException(ErrorCode.LOCATION_MISMATCH,
                    String.format("can not find any instance by level %s", checkResult.curLevel.name()));
        }
        if (!config.isEnableDegradeByUnhealthyPercent() || checkResult.curLevel == RoutingProto.NearbyRoutingConfig.LocationLevel.ALL) {
            return new RouteResult(checkResult.instances, RouteResult.State.Next);
        }
        int healthyInstanceCount = checkResult.healthyInstanceCount;
        double actualHealthyPercent = (double) healthyInstanceCount / (double) serviceInstances.getInstances().size();
        if (actualHealthyPercent <= healthyPercentToDegrade) {
            LOG.debug("[shouldDegrade] enableDegradeByUnhealthyPercent = {},unhealthyPercentToDegrade={},"
                            + "healthyPercent={},isStrict={},matchLevel={}",
                    config.isEnableDegradeByUnhealthyPercent(), config.getUnhealthyPercentToDegrade(),
                    actualHealthyPercent,
                    config.isStrictNearby(), checkResult.curLevel);
            RouteResult result = new RouteResult(checkResult.instances, RouteResult.State.Retry);
            result.getNextRouterInfo().setLocationLevel(nextLevel(checkResult.curLevel));
            result.getNextRouterInfo().setMinAvailableLevel(minAvailableLevel);
            return result;
        }
        return new RouteResult(checkResult.instances, RouteResult.State.Next);
    }

    private RoutingProto.NearbyRoutingConfig.LocationLevel nextLevel(RoutingProto.NearbyRoutingConfig.LocationLevel current) {
        if (current == RoutingProto.NearbyRoutingConfig.LocationLevel.ALL) {
            return current;
        }
        return RoutingProto.NearbyRoutingConfig.LocationLevel.values()[current.ordinal() + 1];
    }

    private CheckResult hasHealthyInstances(ServiceInstances svcInstances,
                                            RoutingProto.NearbyRoutingConfig.LocationLevel targetLevel, Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String> clientInfo) {
        String clientZone = "";
        String clientRegion = "";
        String clientCampus = "";
        if (null != clientInfo) {
            clientZone = clientInfo.getOrDefault(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE, "");
            clientRegion = clientInfo.getOrDefault(RoutingProto.NearbyRoutingConfig.LocationLevel.REGION, "");
            clientCampus = clientInfo.getOrDefault(RoutingProto.NearbyRoutingConfig.LocationLevel.CAMPUS, "");
        }
        CheckResult checkResult = new CheckResult();
        for (Instance instance : svcInstances.getInstances()) {
            switch (targetLevel) {
                case ZONE:
                    if (clientZone.equals("") || clientZone.equals(getInstanceZone(instance))) {
                        checkResult.instances.add(instance);
                        if (isHealthyInstance(instance)) {
                            checkResult.healthyInstanceCount++;
                        }
                    }
                    break;
                case CAMPUS:
                    if (clientCampus.equals("") || clientCampus.equals(getInstanceCampus(instance))) {
                        checkResult.instances.add(instance);
                        if (isHealthyInstance(instance)) {
                            checkResult.healthyInstanceCount++;
                        }
                    }
                    break;
                case REGION:
                    if (clientRegion.equals("") || clientRegion.equals(getInstanceRegion(instance))) {
                        checkResult.instances.add(instance);
                        if (isHealthyInstance(instance)) {
                            checkResult.healthyInstanceCount++;
                        }
                    }
                    break;
                default:
                    checkResult.instances.add(instance);
                    if (isHealthyInstance(instance)) {
                        checkResult.healthyInstanceCount++;
                    }
                    break;
            }
        }
        return checkResult;
    }

    private List<Instance> selectInstances(
            ServiceInstances svcInstances, RoutingProto.NearbyRoutingConfig.LocationLevel targetLevel,
            Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String> clientInfo) {
        List<Instance> instances = new ArrayList<>();
        String clientZone = "";
        String clientRegion = "";
        String clientCampus = "";
        if (null != clientInfo) {
            clientZone = clientInfo.get(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE);
            clientRegion = clientInfo.get(RoutingProto.NearbyRoutingConfig.LocationLevel.REGION);
            clientCampus = clientInfo.get(RoutingProto.NearbyRoutingConfig.LocationLevel.CAMPUS);
        }
        for (Instance instance : svcInstances.getInstances()) {
            switch (targetLevel) {
                case ZONE:
                    if (clientZone.equals("") || clientZone.equals(getInstanceZone(instance))) {
                        instances.add(instance);
                    }
                    break;
                case CAMPUS:
                    if (clientCampus.equals("") || clientCampus.equals(getInstanceCampus(instance))) {
                        instances.add(instance);
                    }
                    break;
                case REGION:
                    if (clientRegion.equals("") || clientRegion.equals(getInstanceRegion(instance))) {
                        instances.add(instance);
                    }
                    break;
                default:
                    instances.add(instance);
                    break;
            }
        }
        return instances;
    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_NEARBY;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return NearbyRouterConfig.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        valueContext = ctx.getValueContext();
        NearbyRouterConfig config = ctx.getConfig().getConsumer().getServiceRouter()
                .getPluginConfig(getName(), NearbyRouterConfig.class);
        if (config == null) {
            throw new PolarisException(ErrorCode.INVALID_CONFIG,
                    String.format("plugin %s config is missing", getName()));
        }
        this.config = config;
        LOG.debug("[init] config={}", this.config);
        locationReadyTimeout = (ctx.getConfig().getGlobal().getAPI().getReportInterval() + ctx.getConfig().getGlobal()
                .getServerConnector().getConnectTimeout()) * (ctx.getConfig().getGlobal().getAPI().getMaxRetryTimes()
                + 1);
        healthyPercentToDegrade = 1 - (double) config.getUnhealthyPercentToDegrade() / (double) 100;
    }

    /**
     * 在整个AppContext初始化完毕后调用
     *
     * @param extensions 插件对象池
     * @throws PolarisException 异常
     */
    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        this.extensions = extensions;
        //强制就近模式下，需要等待地域信息初始化成功
        ensureLocationReady();
    }

    /**
     * 校验是否严格校验地域信息
     */
    public void ensureLocationReady() throws PolarisException {
        if (!this.config.isStrictNearby()) {
            return;
        }
        try {
            this.valueContext.waitForLocationReady(this.locationReadyTimeout);
            refreshLocationInfo();
        } catch (InterruptedException e) {
            throw new PolarisException(ErrorCode.LOCATION_MISMATCH,
                    "caller location not ready,and strict nearby is true.", e);

        }
    }

    private void refreshLocationInfo() {
        Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String> clientLocationInfo = new HashMap<>();
        for (RoutingProto.NearbyRoutingConfig.LocationLevel key : RoutingProto.NearbyRoutingConfig.LocationLevel.values()) {
            if (valueContext.getValue(key.name()) != null) {
                clientLocationInfo.put(key, valueContext.getValue(key.name()));
            }
        }
        locationInfo.set(clientLocationInfo);
        LOG.debug("[refreshLocationInfo] locationInfo={}", clientLocationInfo);
    }

    private Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String> getLocationInfo() {
        if (MapUtils.isEmpty(locationInfo.get())) {
            refreshLocationInfo();
        }
        return locationInfo.get();
    }

    @Override
    public Aspect getAspect() {
        return Aspect.MIDDLE;
    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        if (!super.enable(routeInfo, dstSvcInfo)) {
            return false;
        }
        Map<RoutingProto.NearbyRoutingConfig.LocationLevel, String> clientLocationInfo = getLocationInfo();
        if (MapUtils.isEmpty(clientLocationInfo)) {
            return false;
        }

        ServiceRule serviceRule = getServiceRule(dstSvcInfo.getNamespace(), dstSvcInfo.getService());
        if (serviceRule != null && serviceRule.getRule() != null) {
            ResponseProto.DiscoverResponse discoverResponse = (ResponseProto.DiscoverResponse) serviceRule.getRule();
            List<RoutingProto.RouteRule> nearByRouteRuleList = discoverResponse.getNearbyRouteRulesList();
            if (CollectionUtils.isNotEmpty(nearByRouteRuleList)) {
                for (RoutingProto.RouteRule routeRule : nearByRouteRuleList) {
                    if (routeRule.getEnable()) {
                        return true;
                    }
                }
            }
        }

        Map<String, String> destSvcMetadata = Optional.ofNullable(dstSvcInfo.getMetadata()).orElse(Collections.emptyMap());
        if (Boolean.parseBoolean(destSvcMetadata.get(NEARBY_METADATA_ENABLE))) {
            return true;
        }

        //默认关闭，需要显示打开
        boolean enabled = false;
        if (routeInfo.getMetadataContainerGroup() != null && routeInfo.getMetadataContainerGroup().getCustomMetadataContainer() != null) {
            String enabledStr = routeInfo.getMetadataContainerGroup().getCustomMetadataContainer().getRawMetadataMapValue(ROUTER_TYPE_NEAR_BY, ROUTER_ENABLED);
            if (StringUtils.isNotBlank(enabledStr) && Boolean.parseBoolean(enabledStr)) {
                enabled = true;
            }
        }
        Map<String, String> routerMetadata = routeInfo.getRouterMetadata(ROUTER_TYPE_NEAR_BY);
        if (MapUtils.isNotEmpty(routerMetadata)) {
            String enabledStr = routerMetadata.get(ROUTER_ENABLED);
            if (StringUtils.isNotBlank(enabledStr) && Boolean.parseBoolean(enabledStr)) {
                enabled = true;
            }
        }
        return enabled;
    }

    @Override
    protected void doDestroy() {
    }

    /**
     * 获取就近路由规则
     *
     * @param dstNamespace   目标服务命名空间
     * @param dstServiceName 目标服务名
     * @return 目标服务就近路由规则
     */
    private ServiceRule getServiceRule(String dstNamespace, String dstServiceName) {
        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
        Set<ServiceEventKey> routerKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = new ServiceEventKey(new ServiceKey(dstNamespace, dstServiceName),
                ServiceEventKey.EventType.NEARBY_ROUTE_RULE);
        routerKeys.add(dstSvcEventKey);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(routerKeys);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
        return resourcesResponse.getServiceRule(dstSvcEventKey);
    }

    private String getInstanceZone(Instance instance) {
        String zone = instance.getZone();
        if (StringUtils.isNotBlank(zone)) {
            return zone;
        }
        return getMetadata(instance, ROUTER_METADATA_KEY_ZONE);
    }

    private String getInstanceRegion(Instance instance) {
        String region = instance.getRegion();
        if (StringUtils.isNotBlank(region)) {
            return region;
        }
        return getMetadata(instance, ROUTER_METADATA_KEY_REGION);
    }

    private String getInstanceCampus(Instance instance) {
        String campus = instance.getCampus();
        if (StringUtils.isNotBlank(campus)) {
            return campus;
        }
        return getMetadata(instance, ROUTER_METADATA_KEY_CAMPUS);
    }

    private String getMetadata(Instance instance, String key) {
        Map<String, String> metadata = instance.getMetadata();
        if (MapUtils.isEmpty(metadata)) {
            return "";
        }
        return metadata.get(key);
    }

    private static class CheckResult {

        RoutingProto.NearbyRoutingConfig.LocationLevel curLevel;
        int healthyInstanceCount;
        List<Instance> instances = new ArrayList<>();
    }
}
