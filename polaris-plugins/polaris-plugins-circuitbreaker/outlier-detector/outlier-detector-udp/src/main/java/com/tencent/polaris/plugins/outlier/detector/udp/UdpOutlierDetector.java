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

package com.tencent.polaris.plugins.outlier.detector.udp;

import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.OutlierDetector;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UdpOutlierDetector.java
 *
 * @author andrewshan
 * @date 2019/9/19
 */
public class UdpOutlierDetector implements OutlierDetector {

    private static final Logger LOG = LoggerFactory.getLogger(UdpOutlierDetector.class);

    @Override
    public DetectResult detectInstance(Instance instance) throws PolarisException {
        DatagramSocket socket = null;
        try {
            //TODO 从配置中读取
            String sendStr = "detect";
            InetAddress inet = InetAddress.getByName(instance.getHost());
            byte[] sendBytes = sendStr.getBytes("UTF8");

            socket = new DatagramSocket();
            // 两秒接收不到数据认为超时，防止获取不到连接一直在receive阻塞
            socket.setSoTimeout(2000);
            //发送数据
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, inet, instance.getPort());
            socket.send(sendPacket);
            byte[] recvBuf = new byte[1024];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(recvPacket);

            socket.close();
            String expectRecvStr = "ok";
            byte[] expectRecvBytes = expectRecvStr.getBytes("UTF8");
            if (!Arrays.equals(Arrays.copyOfRange(recvBuf, 0, expectRecvBytes.length), expectRecvBytes)) {
                return new DetectResult(RetStatus.RetFail);
            }
            return new DetectResult(RetStatus.RetSuccess);

        } catch (Exception e) {
            LOG.error("udp detect instance exception, host:{}, port:{}, e:{}", instance.getHost(), instance.getPort(),
                    e);
            return null;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    @Override
    public String getName() {
        return DefaultValues.DEFAULT_UDP_OUTLIER_DETECT;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.OUTLIER_DETECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {

    }

    @Override
    public void destroy() {

    }
}