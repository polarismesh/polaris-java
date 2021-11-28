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

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.LocationLevel;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.api.plugin.server.ReportClientResponse;
import com.tencent.polaris.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportClientTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ReportClientTask.class);

    private final Extensions extensions;

    private final String version;
    private final String clientHost;

    private final ValueContext shareContext;

    public ReportClientTask(Extensions extensions, ValueContext shareContext) {
        this.extensions = extensions;
        this.version = Version.VERSION;
        this.clientHost = shareContext.getHost();
        this.shareContext = shareContext;
    }

    @Override
    public void run() {
        ReportClientResponse rsp = doReport(clientHost, version);
        if (null == rsp) {
            return;
        }

        LOG.debug("current client Region:{}, Zone:{}, Campus:{}", rsp.getRegion(), rsp.getZone(), rsp.getCampus());

        shareContext.setValue(LocationLevel.region.name(), rsp.getRegion());
        shareContext.setValue(LocationLevel.zone.name(), rsp.getZone());
        shareContext.setValue(LocationLevel.campus.name(), rsp.getCampus());
        shareContext.notifyAllForLocationReady();
    }

    private ReportClientResponse doReport(String clientHost, String version) {
        ReportClientRequest req = new ReportClientRequest();
        req.setClientHost(clientHost);
        req.setVersion(version);
        ReportClientResponse rsp = null;
        try {
            rsp = extensions.getServerConnector().reportClient(req);
        } catch (PolarisException e) {
            LOG.warn("fail to report client info(clientHost={}, version={}), cause is {}", clientHost, version,
                    e.getMessage());
        }
        return rsp;
    }


}
