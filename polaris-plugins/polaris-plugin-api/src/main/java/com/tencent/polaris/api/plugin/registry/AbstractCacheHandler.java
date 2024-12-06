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

package com.tencent.polaris.api.plugin.registry;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse.DiscoverResponseType;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto.Service;
import java.util.function.Function;
import org.slf4j.Logger;

public abstract class AbstractCacheHandler implements CacheHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCacheHandler.class);

    private static final String emptyReplaceHolder = "<empty>";

    protected abstract String getRevision(DiscoverResponse discoverResponse);

    @Override
    public CachedStatus compareMessage(RegistryCacheValue oldValue, Object newValue) {
        return compareMessage(getTargetEventType(), oldValue, (DiscoverResponse) newValue,
                new Function<DiscoverResponse, String>() {
                    @Override
                    public String apply(DiscoverResponse discoverResponse) {
                        return getRevision(discoverResponse);
                    }
                });
    }

    /**
     * 比较消息
     *
     * @param eventType 类型
     * @param oldValue 旧值
     * @param discoverResponse proto应答
     * @param getRevision 获取版本号
     * @return 状态
     */
    public static CachedStatus compareMessage(EventType eventType, RegistryCacheValue oldValue,
            DiscoverResponse discoverResponse, Function<DiscoverResponse, String> getRevision) {
        Service service = discoverResponse.getService();
        ServiceEventKey serviceEventKey = new ServiceEventKey(
                new ServiceKey(service.getNamespace().getValue(), service.getName().getValue()), eventType);
        //判断server的错误码，是否未变更

        if (discoverResponse.getCode().getValue() == ServerCodes.DATA_NO_CHANGE) {
            if (null == oldValue) {
                return CachedStatus.CacheEmptyButNoData;
            }
            return CachedStatus.CacheNotChanged;
        }
        String newRevision = getRevision.apply(discoverResponse);
        String oldRevision;
        boolean oldLoadedFromFile = false;
        CachedStatus cachedStatus;
        if (null == oldValue) {
            oldRevision = emptyReplaceHolder;
            cachedStatus = CachedStatus.CacheNotExists;
        } else {
            oldLoadedFromFile = oldValue.isLoadedFromFile();
            oldRevision = oldValue.getRevision();

            // 如果当前的请求返回是获取服务列表
            // 因为 server 对于 sdk 获取服务列表支持根据 metadata、business、namespace 字段进行筛选操作，无法告知 SDK 一个准确的
            // revision 值，因此对于 SERVICES 类型的请求，默认直接强制更新 cache 数据
            if (discoverResponse.getType() == DiscoverResponseType.SERVICES) {
                cachedStatus = CachedStatus.CacheChanged;
            } else {
                cachedStatus = oldRevision.equals(newRevision) && !oldLoadedFromFile ? CachedStatus.CacheNotChanged
                        : CachedStatus.CacheChanged;
            }
        }
        if (cachedStatus != CachedStatus.CacheNotChanged) {
            LOG.info("resource {} has updated, compare status {}, old revision is {}, old loadedFromFile is {}, "
                            + "new revision is {}",
                    serviceEventKey, cachedStatus, oldRevision, oldLoadedFromFile, newRevision);
        } else {
            LOG.debug("resource {} is not updated, compare status {}, old revision is {}, old loadedFromFile is {}, "
                            + "new revision is {}",
                    serviceEventKey, cachedStatus, oldRevision, oldLoadedFromFile, newRevision);
        }
        return cachedStatus;
    }
}
