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

package com.tencent.polaris.plugins.router.metadata;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;

import java.util.*;

/**
 * 正常场景：选出的实例子集不为空，那么优先返回健康子集，如果全部不健康则进行全死全活返回不健康子集。
 * <p>
 * 异常场景：需要根据GetOneInstanceRequest的请求策略进行降级决策
 * <p>
 * 不降级（默认）：返回未找到实例错误
 * 返回所有节点：优先返回服务下的健康子集，如果全部不健康则全死全活返回不健康子集
 * 返回实例元数据不包含请求metadata的key的节点：优先返回筛选出的健康子集，如果全部不健康则返回不健康子集
 * 例如：ip1 set=1 ; ip2 set=2 ; ip3 ; 请求时 set=0 返回的 ip3（这个时候只判断key）
 * 降级使用指定metadata进行实例筛选。(未实现)
 *
 * @author starkwen
 * @date 2021/2/24 下午3:23
 */
public class MetadataRouter extends AbstractServiceRouter implements PluginConfigProvider {

    public static final String ROUTER_TYPE_METADATA = "metadataRoute";

    private static final String KEY_METADATA_FAILOVER_TYPE = "internal-metadata-failover-type";

    public static final String KEY_METADATA_KEYS = "metadataRouteKeys";

    private static final Map<String, FailOverType> valueToFailoverType = new HashMap<>();

    private static final Map<MetadataFailoverType, FailOverType> inputToFailoverType = new HashMap<>();

    static {
        valueToFailoverType.put("none", FailOverType.none);
        valueToFailoverType.put("all", FailOverType.all);
        valueToFailoverType.put("others", FailOverType.others);

        inputToFailoverType.put(MetadataFailoverType.METADATAFAILOVERNONE, FailOverType.none);
        inputToFailoverType.put(MetadataFailoverType.METADATAFAILOVERALL, FailOverType.all);
        inputToFailoverType.put(MetadataFailoverType.METADATAFAILOVERNOTKEY, FailOverType.others);
    }

    private MetadataRouterConfig config;

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances) throws PolarisException {
        boolean availableInsFlag;
        Map<String, String> reqMetadata = getRouterMetadata(routeInfo);
        List<Instance> instanceList = new ArrayList<>();
        for (Instance ins : instances.getInstances()) {
            availableInsFlag = true;
            // 要满足请求中的metadata K-V全部存在于实例的metadata中
            for (Map.Entry<String, String> entry : reqMetadata.entrySet()) {
                if (ins.getMetadata().containsKey(entry.getKey())
                        && ins.getMetadata().get(entry.getKey()).equals(entry.getValue())) {
                    continue;
                }
                availableInsFlag = false;
                break;
            }
            if (availableInsFlag) {
                instanceList.add(ins);
            }
        }
        if (!CollectionUtils.isEmpty(instanceList)) {
            return new RouteResult(instanceList, RouteResult.State.Next);
        }
        FailOverType failOverType = config.getMetadataFailOverType();
        Map<String, String> svcMetadata = instances.getMetadata();
        if (MapUtils.isNotEmpty(svcMetadata)) {
            if (svcMetadata.containsKey(KEY_METADATA_FAILOVER_TYPE)) {
                String value = svcMetadata.get(KEY_METADATA_FAILOVER_TYPE);
                if (valueToFailoverType.containsKey(value)) {
                    failOverType = valueToFailoverType.get(value);
                }
            }
        }
        MetadataFailoverType metadataFailoverType = routeInfo.getMetadataFailoverType();
        if (null != metadataFailoverType) {
            failOverType = inputToFailoverType.get(metadataFailoverType);
        }
        switch (failOverType) {
            case all:
                return new RouteResult(instances.getInstances(), RouteResult.State.Next);
            case others:
                return new RouteResult(addNotContainKeyIns(instances, reqMetadata), RouteResult.State.Next);
            default:
                // none或其他情况不降级
                throw new PolarisException(ErrorCode.METADATA_MISMATCH,
                        String.format("can not find any instance by service %s", routeInfo.getDestService()));
        }
    }

    private List<Instance> addNotContainKeyIns(ServiceInstances instances, Map<String, String> reqMetadata) {
        List<Instance> instanceList = new ArrayList<>();
        for (Instance ins : instances.getInstances()) {
            boolean containKey = true;
            for (Map.Entry<String, String> entry : reqMetadata.entrySet()) {
                if (ins.getMetadata().containsKey(entry.getKey())) {
                    continue;
                }
                containKey = false;
            }
            // 如果实例的metadata不包含传入的metadata，或者传入的metadata为空
            if (!containKey) {
                instanceList.add(ins);
            }
        }
        return instanceList;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        this.config = ctx.getConfig().getConsumer().getServiceRouter()
                .getPluginConfig(getName(), MetadataRouterConfig.class);
    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_METADATA;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return MetadataRouterConfig.class;
    }

    public MetadataRouterConfig getConfig() {
        return config;
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
        Map<String, String> metadata = getRouterMetadata(routeInfo);
        return !MapUtils.isEmpty(metadata);
    }

    private Map<String, String> getRouterMetadata(RouteInfo routeInfo) {
        Map<String, String> metadata = new HashMap<>(routeInfo.getRouterMetadata(ROUTER_TYPE_METADATA));

        if (routeInfo.getMetadataContainerGroup() != null && routeInfo.getMetadataContainerGroup().getCustomMetadataContainer() != null) {
            String metadataRouteKeys = routeInfo.getMetadataContainerGroup().getCustomMetadataContainer().getRawMetadataMapValue(ROUTER_TYPE_METADATA, KEY_METADATA_KEYS);
            if (StringUtils.isNotBlank(metadataRouteKeys)) {
                String[] keysArr = metadataRouteKeys.split(",");
                Set<String> keysSet = new HashSet<>(Arrays.asList(keysArr));
                for (String key : keysSet) {
                    String value = routeInfo.getMetadataContainerGroup().getCustomMetadataContainer().getRawMetadataStringValue(key);
                    metadata.put(key, value);
                }
            }
        }
        return metadata;
    }
}
