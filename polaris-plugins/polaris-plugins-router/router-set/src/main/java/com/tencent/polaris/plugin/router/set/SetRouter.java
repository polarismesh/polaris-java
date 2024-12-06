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

package com.tencent.polaris.plugin.router.set;

import static com.tencent.polaris.api.plugin.route.RouterConstants.SET_ENABLED;
import static com.tencent.polaris.api.plugin.route.RouterConstants.SET_ENABLE_KEY;
import static com.tencent.polaris.api.plugin.route.RouterConstants.SET_NAME_KEY;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.RouterConstants;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * set路由
 *
 * @author Thrteenwang
 * @date 20200613
 */

public class SetRouter extends AbstractServiceRouter {

    private static final Logger LOG = LoggerFactory.getLogger(SetRouter.class);

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException {
        //是否指定了被调的set
        if (routeInfo.getDestService() != null && MapUtils
                .isNotEmpty(routeInfo.getDestService().getMetadata())) {
            Map<String, String> metadata = routeInfo.getDestService().getMetadata();

            // 如果指定了被调的set，则不走就近，不论结果如何直接返回
            String setEnabled = metadata.get(RouterConstants.SET_ENABLE_KEY);
            if (SET_ENABLED.equals(setEnabled)) {
                routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY);
                String destSetName = metadata.get(RouterConstants.SET_NAME_KEY);
                List<Instance> filteredResult = instances.getInstances().stream().filter(instance ->
                        calleeSetEnabled(instance) && setIsAllSame(instance, destSetName))
                        .collect(Collectors.toList());
                LOG.debug("[doSetRouter] destinationSet result:{},{}", destSetName,
                        filteredResult.size());
                return new RouteResult(filteredResult, RouteResult.State.Next);
            }
        }

        //是否指定的主调的set
        if (routeInfo.getDestService() != null && MapUtils
                .isNotEmpty(routeInfo.getDestService().getMetadata())) {
            Map<String, String> metadata = routeInfo.getDestService().getMetadata();
            String setEnabled = metadata.get(RouterConstants.SET_ENABLE_KEY);

            if (SET_ENABLED.equals(setEnabled)) {
                String sourceSetName = metadata.get(RouterConstants.SET_NAME_KEY);
                //2、被调是否启用set
                boolean calleeSetEnabled = instances.getInstances().stream()
                        .anyMatch(instance -> calleeSetEnabled(instance)
                                && setNameIsSame(instance, sourceSetName));

                LOG.debug("[doSetRouter] calleeSetEnabled={}", calleeSetEnabled);
                //没有启用直接返回所有，走就近路由
                if (!calleeSetEnabled) {
                    return new RouteResult(instances.getInstances(), RouteResult.State.Next);
                }
                //只要主被调都启用了，就不走就近了
                routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY);

                boolean callIsLikeMatch = isLikeMatch(sourceSetName);
                LOG.debug("[doSetRouter] callIsLikeMatch={}", callIsLikeMatch);
                //如果是完整的三段匹配，先尝试三段匹配
                if (!callIsLikeMatch) {
                    //3、三段匹配
                    List<Instance> filteredResult = instances.getInstances().stream()
                            .filter(instance -> setIsAllSame(instance, sourceSetName)
                            ).collect(Collectors.toList());
                    //如果匹配到了
                    LOG.debug("[doSetRouter] allMatch result:{},{}", sourceSetName,
                            filteredResult.size());
                    if (CollectionUtils.isNotEmpty(filteredResult)) {
                        return new RouteResult(filteredResult, RouteResult.State.Next);
                    }
                    //同地而且被调最后一段是*
                    filteredResult = instances.getInstances().stream()
                            .filter(instance -> setIsSameArea(instance, sourceSetName)
                                    && groupIsLike(
                                    instance)
                            ).collect(Collectors.toList());
                    return new RouteResult(filteredResult, RouteResult.State.Next);
                }
                //4、两段匹配
                List<Instance> filteredResult = instances.getInstances().stream()
                        .filter(instance -> setIsSameArea(instance, sourceSetName)
                        ).collect(Collectors.toList());
                LOG.debug("[doSetRouter] likeMatch result:{},{}", sourceSetName,
                        filteredResult.size());
                return new RouteResult(filteredResult, RouteResult.State.Next);
            }
        }
        //直接走就近了
        return new RouteResult(instances.getInstances(), RouteResult.State.Next);
    }


    /**
     * 主被调是否三段匹配
     */
    private boolean setIsAllSame(Instance instance, String setName) {
        return instance.getMetadata() != null && setName
                .equals(instance.getMetadata().get(RouterConstants.SET_NAME_KEY));
    }

    /**
     * 是否是模糊匹配
     *
     * @param instance 实例对象
     * @return boolean
     */
    public boolean groupIsLike(Instance instance) {
        String calleeSet = null;
        if (instance.getMetadata() != null) {
            calleeSet = instance.getMetadata().get(SET_NAME_KEY);
        }
        return isLikeMatch(calleeSet);
    }

    /**
     * 分组是否是带*的匹配
     */
    private boolean isLikeMatch(String callSet) {
        if (callSet != null) {
            String[] callSetTmp = callSet.split("\\.");
            return callSetTmp.length == 3 && "*".equals(callSetTmp[2]);
        }
        return false;
    }

    /**
     * 被调是否启用了set
     */
    private boolean calleeSetEnabled(Instance instance) {
        return instance.getMetadata() != null && SET_ENABLED
                .equals(instance.getMetadata().get(SET_ENABLE_KEY));
    }


    /**
     * 主调被调是否在同一个地区
     */
    private boolean setIsSameArea(Instance instance, String callSet) {
        String calleeSet = null;
        if (instance.getMetadata() != null) {
            calleeSet = instance.getMetadata().get(SET_NAME_KEY);
        }

        if (callSet == null || calleeSet == null) {
            return false;
        }
        String[] callSetTmp = callSet.split("\\.");
        String[] calleeSetTmp = calleeSet.split("\\.");
        if (calleeSetTmp.length > 2 && callSetTmp.length > 2) {
            return calleeSetTmp[0].equals(callSetTmp[0]) && calleeSetTmp[1].equals(callSetTmp[1]);
        }
        return false;
    }

    /**
     * 主被调set是否set名相同
     */
    private boolean setNameIsSame(Instance instance, String callerSet) {
        String calleeSet = null;
        if (instance.getMetadata() != null) {
            calleeSet = instance.getMetadata().get(SET_NAME_KEY);
        }
        if (callerSet == null || calleeSet == null) {
            return false;
        }
        String[] callSetTmp = callerSet.split("\\.");
        String[] calleeSetTmp = calleeSet.split("\\.");
        if (calleeSetTmp.length > 0 && callSetTmp.length > 0) {
            return calleeSetTmp[0].equals(callSetTmp[0]);
        }
        return false;
    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_SET;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public Aspect getAspect() {
        return Aspect.MIDDLE;
    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        return super.enable(routeInfo, dstSvcInfo);
    }
}
