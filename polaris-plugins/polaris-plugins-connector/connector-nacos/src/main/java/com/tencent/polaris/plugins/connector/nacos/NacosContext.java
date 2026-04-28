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

package com.tencent.polaris.plugins.connector.nacos;


import static com.alibaba.nacos.api.common.Constants.DEFAULT_CLUSTER_NAME;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;

import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.utils.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context of nacos server connector.
 *
 * @author Yuwei Fu
 */
public class NacosContext {

    private String groupName;

    private String clusterName;

    private String namespace;

    private boolean ephemeral;

    private String serviceName;

    private Long nacosErrorSleep;

    private double nacosWeight;

    private boolean dubboAdapt;

    /**
     * 发现侧 polaris 服务名 -> nacos 服务名映射表。
     * 仅 dubboAdapt=true 时生效；未命中/空值透传原服务名。
     */
    private final Map<String, String> serviceNameMappings = new ConcurrentHashMap<>();


    public NacosContext(){
        groupName = DEFAULT_GROUP;
        clusterName = DEFAULT_CLUSTER_NAME;
        namespace = DEFAULT_NAMESPACE_ID;
        nacosErrorSleep = 1000L;
        ephemeral = false;
        nacosWeight = 1;
    }


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getNacosErrorSleep() {
        return nacosErrorSleep;
    }

    public void setNacosErrorSleep(Long nacosErrorSleep) {
        this.nacosErrorSleep = nacosErrorSleep;
    }

    public double getNacosWeight() {
        return nacosWeight;
    }

    public void setNacosWeight(double nacosWeight) {
        this.nacosWeight = nacosWeight;
    }

    public boolean isDubboAdapt() {
        return dubboAdapt;
    }

    public void setDubboAdapt(boolean dubboAdapt) {
        this.dubboAdapt = dubboAdapt;
    }

    /**
     * 注册一条服务名映射。polaris 名或 nacos 名为空（null/空串）时按移除处理。
     *
     * <p><b>重要:</b> 映射应在该 polaris 服务首次被订阅之前写入。一个 polaris 服务首次 watch
     * 后,NacosService 会以转换后的 nacos 名为 key 缓存 EventListener;此时再修改映射不会
     * 触发重订阅,也不会取消旧订阅。若确需切换订阅名,请显式 unsubscribe 后再 subscribe。
     */
    public void putServiceNameMapping(String polarisServiceName, String nacosServiceName) {
        if (StringUtils.isEmpty(polarisServiceName)) {
            return;
        }
        if (StringUtils.isEmpty(nacosServiceName)) {
            serviceNameMappings.remove(polarisServiceName);
        } else {
            serviceNameMappings.put(polarisServiceName, nacosServiceName);
        }
    }

    public void removeServiceNameMapping(String polarisServiceName) {
        if (StringUtils.isNotEmpty(polarisServiceName)) {
            serviceNameMappings.remove(polarisServiceName);
        }
    }

    /**
     * 整表替换。传入 null 等价于清空。key 或 value 为空（null/空串）的条目将被丢弃。
     *
     * <p>非原子操作(clear 后再 put),仅适合在连接器首次订阅之前做一次性初始化使用。
     * 运行时动态增删映射请使用 {@link #putServiceNameMapping(String, String)}
     * 与 {@link #removeServiceNameMapping(String)},避免读写端看到空表中间态。
     */
    public void setServiceNameMappings(Map<String, String> mappings) {
        serviceNameMappings.clear();
        if (mappings != null) {
            for (Map.Entry<String, String> e : mappings.entrySet()) {
                if (StringUtils.isNotEmpty(e.getKey()) && StringUtils.isNotEmpty(e.getValue())) {
                    serviceNameMappings.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    /**
     * 返回不可变视图，调用方修改将抛 UnsupportedOperationException。
     */
    public Map<String, String> getServiceNameMappings() {
        return Collections.unmodifiableMap(serviceNameMappings);
    }

    @Override
    public String toString() {
        return "NacosContext{" +
                "groupName='" + groupName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", ephemeral=" + ephemeral +
                ", serviceName='" + serviceName + '\'' +
                ", nacosErrorSleep=" + nacosErrorSleep +
                ", nacosWeight=" + nacosWeight +
                ", dubboAdapt=" + dubboAdapt +
                '}';
    }
}
