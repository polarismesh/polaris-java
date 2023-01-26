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

package com.tencent.polaris.plugin.location.remoteservice;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pb.LocationGRPCGrpc;
import com.tencent.polaris.client.pb.LocationGRPCGrpc.LocationGRPCBlockingStub;
import com.tencent.polaris.client.pb.LocationGRPCService;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugin.location.base.BaseLocationProvider;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class RemoteServiceLocationProvider extends BaseLocationProvider<RemoteServiceLocationProvider.ServiceOption> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteServiceLocationProvider.class);

    private LocationGRPCBlockingStub stub;

    public RemoteServiceLocationProvider() {
        super(ServiceOption.class);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.REMOTE_SERVICE;
    }

    @Override
    public ModelProto.Location doGet(ServiceOption option) {
        buildGrpcStub(option);

        try {
            LocationGRPCService.LocationResponse response = stub
                    .getLocation(LocationGRPCService.LocationRequest.newBuilder()
                            .setClientIp(getLocalHost(option.getTarget()))
                            .build());

            return ModelProto.Location.newBuilder()
                    .setRegion(StringValue.newBuilder().setValue(StringUtils.defaultString(response.getRegion()))
                            .build())
                    .setZone(StringValue.newBuilder().setValue(StringUtils.defaultString(response.getZone())).build())
                    .setCampus(StringValue.newBuilder().setValue(StringUtils.defaultString(response.getCampus()))
                            .build())
                    .build();
        } catch (Exception e) {
            LOGGER.error("[Location][Provider][RemoteService] get location from remote service fail, option : {}",
                    option, e);
            return null;
        }
    }

    public synchronized void buildGrpcStub(ServiceOption option) {
        if (stub != null) {
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(option.getTarget()).usePlaintext().build();
        stub = LocationGRPCGrpc.newBlockingStub(channel);
    }

    public static class ServiceOption {

        private String target;

        String getTarget() {
            return target;
        }

        void setTarget(String target) {
            this.target = target;
        }
    }

    private String getLocalHost(String addresses) throws Exception {
        if (addresses == null || addresses.length() == 0) {
            return configuration.getGlobal().getAPI().getBindIP();
        }

        String[] addressList = addresses.split(",");

        String[] tokens = addressList[0].split(":");
        try (Socket socket = new Socket(tokens[0], Integer.parseInt(tokens[1]))) {
            return socket.getLocalAddress().getHostAddress();
        }
    }
}
