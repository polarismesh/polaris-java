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

package com.tencent.polaris.plugin.location.base;

import com.google.gson.Gson;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.IdAwarePlugin;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.location.LocationProvider;
import com.tencent.polaris.factory.config.global.LocationConfigImpl;
import com.tencent.polaris.factory.config.global.LocationProviderConfigImpl;
import com.tencent.polaris.specification.api.v1.model.ModelProto;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public abstract class BaseLocationProvider<T> extends Destroyable implements LocationProvider, IdAwarePlugin {

    protected LocationProviderConfigImpl providerConfig;

    private int id;

    protected ModelProto.Location cache;

    private final Class<T> typeClass;

    protected Configuration configuration;

    protected BaseLocationProvider(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return getProviderType().getName();
    }

    @Override
    public PluginType getType() {
        return PluginTypes.LOCAL_PROVIDER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        configuration = ctx.getConfig();
        LocationConfigImpl config = (LocationConfigImpl) ctx.getConfig().getGlobal().getLocation();
        providerConfig = config.getByType(getName());
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

    }

    @Override
    public ModelProto.Location getLocation() {
        if (cache == null) {
            if (providerConfig == null) {
                return null;
            }

            Gson gson = new Gson();
            T option = gson.fromJson(gson.toJson(providerConfig.getOptions()), typeClass);

            cache = doGet(option);
        }
        return cache;
    }

    public abstract ModelProto.Location doGet(T option);

    public static class GetOption {

        private String region;
        private String zone;
        private String campus;

        public String getRegion() {
            return region;
        }

        void setRegion(String region) {
            this.region = region;
        }

        public String getZone() {
            return zone;
        }

        void setZone(String zone) {
            this.zone = zone;
        }

        public String getCampus() {
            return campus;
        }

        void setCampus(String campus) {
            this.campus = campus;
        }
    }

}
