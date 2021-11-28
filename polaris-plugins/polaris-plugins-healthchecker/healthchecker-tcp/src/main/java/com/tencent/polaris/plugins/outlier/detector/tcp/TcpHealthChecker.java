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

package com.tencent.polaris.plugins.outlier.detector.tcp;

import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TcpOutlierDetector.java
 *
 * @author andrewshan
 * @date 2019/9/19
 */
public class TcpHealthChecker implements HealthChecker {

    private static final Logger LOG = LoggerFactory.getLogger(TcpHealthChecker.class);

    @Override
    public DetectResult detectInstance(Instance instance) throws PolarisException {
        String host = instance.getHost();
        int port = instance.getPort();

        Socket socket = null;
        try {
            socket = new Socket(host, port);
            //TODO 从配置中读取
            String sendStr = "detect";
            String expectRecvStr = "ok";

            boolean needSendData = !(sendStr == null || "".equals(sendStr));
            if (!needSendData) {
                //未配置发送包，则连接成功即可
                return new DetectResult(RetStatus.RetSuccess);
            }

            byte[] sendBytes = sendStr.getBytes("UTF8");
            byte[] expectRecvBytes = expectRecvStr.getBytes("UTF8");

            OutputStream os = socket.getOutputStream();
            //发包
            os.write(sendBytes);

            byte[] recvBytes = recvFromSocket(socket, expectRecvBytes.length);

            if (Arrays.equals(Arrays.copyOfRange(recvBytes, 0, expectRecvBytes.length), expectRecvBytes)) {
                //回包符合预期
                return new DetectResult(RetStatus.RetSuccess);
            }

            return new DetectResult(RetStatus.RetFail);

        } catch (IOException e) {
            LOG.info("tcp detect instance, create sock exception, host:{}, port:{}, e:{}", host, port, e);
            return null;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOG.info("tcp detect instance, close sock exception, host:{}, port:{}, e:{}", host, port, e);
                }
            }
        }
    }

    private byte[] recvFromSocket(Socket socket, int maxLen) throws IOException {
        InputStream is = socket.getInputStream();
        byte[] recvBytes = new byte[1024];
        int recvLen = 0;
        int tempLen;
        do {
            if (recvLen + maxLen > recvBytes.length) {
                break;
            }
            tempLen = is.read(recvBytes, recvLen, maxLen);
            if (tempLen >= 0) {
                recvLen += tempLen;
            } else {
                // 当返回-1时代表已经读完，防止死循环
                return recvBytes;
            }
        } while (tempLen >= 0 || recvLen >= maxLen);

        return recvBytes;
    }

    @Override
    public String getName() {
        return DefaultValues.DEFAULT_HEALTH_CHECKER_TCP;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.HEALTH_CHECKER.getBaseType();
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