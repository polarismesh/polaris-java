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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.Service;

/**
 * Instance invocation metrics.
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class ServiceCallResult implements InstanceGauge {

    /**
     * 服务名
     */
    private String service;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 被调节点IP
     */
    private String host;

    /**
     * 被调端口
     */
    private int port;

    /**
     * 服务实例信息
     */
    private Instance instance;

    /**
     * 主调方标签，代表服务调用的接口等信息
     */
    private String labels;

    /**
     * 请求是否成功以retStatus为准，retCode不生效
     */
    private RetStatus retStatus = RetStatus.RetUnknown;

    /**
     * 返回码
     */
    private Integer retCode;

    /**
     * 时延 单位：ms
     */
    private Long delay;

    /**
     * 实例分组
     */
    private String subset;

    /**
     * 方法，指HTTP Path，不是HTTP Method
     */
    private String method;

    /**
     * 主调服务
     */
    private Service callerService;

    @Override
    public String getHost() {
        if (null != instance) {
            return instance.getHost();
        }
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        if (null != instance) {
            return instance.getPort();
        }
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public RetStatus getRetStatus() {
        return retStatus;
    }

    public void setRetStatus(RetStatus retStatus) {
        this.retStatus = retStatus;
    }

    @Override
    public String getNamespace() {
        if (null != instance) {
            return instance.getNamespace();
        }
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getService() {
        if (null != instance) {
            return instance.getService();
        }
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String getInstanceId() {
        if (null == instance) {
            return "";
        }
        return instance.getId();
    }

    @Override
    public Integer getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    @Override
    public Long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public Instance getInstance() {
        return instance;
    }

    @Override
    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    @Override
    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    @Override
    public String getSubset() {
        return subset;
    }

    public void setSubset(String subset) {
        this.subset = subset;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public Service getCallerService() {
        return callerService;
    }

    public void setCallerService(Service callerService) {
        this.callerService = callerService;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceCallResult{" +
                "service='" + service + '\'' +
                ", namespace='" + namespace + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", instance=" + instance +
                ", labels='" + labels + '\'' +
                ", retStatus=" + retStatus +
                ", retCode=" + retCode +
                ", delay=" + delay +
                ", subset='" + subset + '\'' +
                ", method='" + method + '\'' +
                ", callerService=" + callerService +
                '}';
    }
}
