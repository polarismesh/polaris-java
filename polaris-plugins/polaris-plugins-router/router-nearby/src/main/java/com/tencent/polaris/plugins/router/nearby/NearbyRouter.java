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
import com.tencent.polaris.api.plugin.route.LocationLevel;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.pojo.StatusDimension.Level;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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


    private static final Logger LOG = LoggerFactory.getLogger(NearbyRouter.class);

    private static final LocationLevel defaultMinLevel = LocationLevel.zone;
    /**
     * 主调的地域信息
     */
    private final AtomicReference<Map<LocationLevel, String>> locationInfo = new AtomicReference<>();
    /**
     * 等待地域信息就绪的超时时间
     */
    long locationReadyTimeout;
    private ValueContext valueContext;
    private ScheduledExecutorService reportClientExecutor;
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
        //先获取最低可用就近级别
        LocationLevel minAvailableLevel = config.getMatchLevel();
        if (null == minAvailableLevel) {
            minAvailableLevel = defaultMinLevel;
        }
        LocationLevel minLevel = minAvailableLevel;
        if (null != routeInfo.getNextRouterInfo()) {
            if (null != routeInfo.getNextRouterInfo().getLocationLevel()) {
                minLevel = routeInfo.getNextRouterInfo().getLocationLevel();
            }
            if (null != routeInfo.getNextRouterInfo().getMinAvailableLevel()) {
                minAvailableLevel = routeInfo.getNextRouterInfo().getMinAvailableLevel();
            }
        }
        LocationLevel maxLevel = config.getMaxMatchLevel();
        if (null == maxLevel) {
            maxLevel = LocationLevel.all;
        }

        Map<LocationLevel, String> clientLocationInfo = getLocationInfo();
        if (minLevel.ordinal() >= maxLevel.ordinal()) {
            List<Instance> instances = selectInstances(serviceInstances, minAvailableLevel, clientLocationInfo);
            if (CollectionUtils.isEmpty(instances)) {
                throw new PolarisException(ErrorCode.LOCATION_MISMATCH,
                        String.format("can not find any instance by level %s", minLevel.name()));
            }
            //已经循环了一圈
            return new RouteResult(selectInstances(
                    serviceInstances, minAvailableLevel, clientLocationInfo), RouteResult.State.Next);
        }
        CheckResult checkResult = new CheckResult();
        for (int i = minLevel.ordinal(); i <= maxLevel.ordinal(); i++) {
            LocationLevel curLevel = LocationLevel.values()[i];
            checkResult = hasHealthyInstances(serviceInstances, routeInfo.getStatusDimensions(), curLevel,
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
        if (!config.isEnableDegradeByUnhealthyPercent() || checkResult.curLevel == LocationLevel.all) {
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

    private LocationLevel nextLevel(LocationLevel current) {
        if (current == LocationLevel.all) {
            return current;
        }
        return LocationLevel.values()[current.ordinal() + 1];
    }

    private CheckResult hasHealthyInstances(ServiceInstances svcInstances, Map<Level, StatusDimension> dimensions,
                                            LocationLevel targetLevel, Map<LocationLevel, String> clientInfo) {
        String clientZone = "";
        String clientRegion = "";
        String clientCampus = "";
        if (null != clientInfo) {
            clientZone = clientInfo.getOrDefault(LocationLevel.zone, "");
            clientRegion = clientInfo.getOrDefault(LocationLevel.region, "");
            clientCampus = clientInfo.getOrDefault(LocationLevel.campus, "");
        }
        CheckResult checkResult = new CheckResult();
        for (Instance instance : svcInstances.getInstances()) {
            switch (targetLevel) {
                case zone:
                    if (clientZone.equals("") || clientZone.equals(getInstanceZone(instance))) {
                        checkResult.instances.add(instance);
                        if (isHealthyInstance(instance, dimensions)) {
                            checkResult.healthyInstanceCount++;
                        }
                    }
                    break;
                case campus:
                    if (clientCampus.equals("") || clientCampus.equals(getInstanceCampus(instance))) {
                        checkResult.instances.add(instance);
                        if (isHealthyInstance(instance, dimensions)) {
                            checkResult.healthyInstanceCount++;
                        }
                    }
                    break;
                case region:
                    if (clientRegion.equals("") || clientRegion.equals(getInstanceRegion(instance))) {
                        checkResult.instances.add(instance);
                        if (isHealthyInstance(instance, dimensions)) {
                            checkResult.healthyInstanceCount++;
                        }
                    }
                    break;
                default:
                    checkResult.instances.add(instance);
                    if (isHealthyInstance(instance, dimensions)) {
                        checkResult.healthyInstanceCount++;
                    }
                    break;
            }
        }
        return checkResult;
    }

    private List<Instance> selectInstances(
            ServiceInstances svcInstances, LocationLevel targetLevel, Map<LocationLevel, String> clientInfo) {
        List<Instance> instances = new ArrayList<>();
        String clientZone = "";
        String clientRegion = "";
        String clientCampus = "";
        if (null != clientInfo) {
            clientZone = clientInfo.get(LocationLevel.zone);
            clientRegion = clientInfo.get(LocationLevel.region);
            clientCampus = clientInfo.get(LocationLevel.campus);
        }
        for (Instance instance : svcInstances.getInstances()) {
            switch (targetLevel) {
                case zone:
                    if (clientZone.equals("") || clientZone.equals(getInstanceZone(instance))) {
                        instances.add(instance);
                    }
                    break;
                case campus:
                    if (clientCampus.equals("") || clientCampus.equals(getInstanceCampus(instance))) {
                        instances.add(instance);
                    }
                    break;
                case region:
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
        if (this.config.isEnableReportLocalAddress()) {
            reportClientExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName()));
        }
    }

    /**
     * 在整个AppContext初始化完毕后调用
     *
     * @param extensions 插件对象池
     * @throws PolarisException 异常
     */
    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        //加载本地配置文件的地址
        if (!this.config.getRegion().isEmpty() || !this.config.getZone().isEmpty() || !this.config.getCampus().isEmpty()) {
            LOG.info("config client location Region:{}, Zone:{}, Campus:{}", this.config.getRegion(),
                    this.config.getZone(), this.config.getCampus());

            if(!config.getRegion().isEmpty()){
                valueContext.setValue(LocationLevel.region.name(), this.config.getRegion());
            }
            if(!config.getZone().isEmpty()){
                valueContext.setValue(LocationLevel.zone.name(), this.config.getZone());
            }
            if(!config.getCampus().isEmpty()){
                valueContext.setValue(LocationLevel.campus.name(), this.config.getCampus());
            }
            valueContext.notifyAllForLocationReady();
        }



        if (null != reportClientExecutor) {
            //执行并定时进行客户端上报
            reportClientExecutor.scheduleAtFixedRate(
                    new ReportClientTask(extensions, valueContext), 0, 60, TimeUnit.SECONDS);
            LOG.info("reportClientExecutor has been started");
            //强制就近模式下，需要等待地域信息初始化成功
            ensureLocationReady();
        }
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
        Map<LocationLevel, String> clientLocationInfo = new HashMap<>();
        for (LocationLevel key : LocationLevel.values()) {
            if (valueContext.getValue(key.name()) != null) {
                clientLocationInfo.put(key, valueContext.getValue(key.name()));
            }
        }
        locationInfo.set(clientLocationInfo);
        LOG.debug("[refreshLocationInfo] locationInfo={}", clientLocationInfo);
    }

    private Map<LocationLevel, String> getLocationInfo() {
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
        Map<LocationLevel, String> clientLocationInfo = getLocationInfo();
        if (MapUtils.isEmpty(clientLocationInfo)) {
            return false;
        }
        //默认关闭，需要显示打开
        Map<String, String> routerMetadata = routeInfo.getRouterMetadata(ROUTER_TYPE_NEAR_BY);
        if (MapUtils.isNotEmpty(routerMetadata)) {
            String enabled = routerMetadata.get(ROUTER_ENABLED);
            return StringUtils.isNotBlank(enabled) && Boolean.parseBoolean(enabled);
        }
        return false;
    }

    @Override
    protected void doDestroy() {
        LOG.info("reportClientExecutor has been stopped");
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{reportClientExecutor});
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

        LocationLevel curLevel;
        int healthyInstanceCount;
        List<Instance> instances = new ArrayList<>();
    }
}
