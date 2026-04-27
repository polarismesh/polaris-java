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

import com.tencent.polaris.api.plugin.server.CommonProviderRequest;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.DUBBO_CATEGORY_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.DUBBO_GROUP_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.DUBBO_VERSION_KEY;

/**
 * NacosConnector 单元测试
 */
public class NacosConnectorTests {

    // ---- 注册侧 ----

    /**
     * dubboAdapt=false 时，服务名保持原样
     */
    @Test
    public void getServiceName_dubboAdaptDisabled_returnsOriginal() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(false);

        TestableNacosConnector connector = new TestableNacosConnector(ctx);

        CommonProviderRequest req = buildProviderRequest("com.example.IFoo", null);
        Assert.assertEquals("com.example.IFoo", connector.exposeGetServiceName(req));
    }

    /**
     * dubboAdapt=true，metadata 仅有 interface（category/version/group 均使用默认值）
     */
    @Test
    public void getServiceName_dubboAdaptEnabled_allDefaults() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);

        TestableNacosConnector connector = new TestableNacosConnector(ctx);

        CommonProviderRequest req = buildProviderRequest("com.example.IFoo", null);
        Assert.assertEquals("providers:com.example.IFoo::", connector.exposeGetServiceName(req));
    }

    /**
     * dubboAdapt=true，metadata 中指定了 category、version、group
     */
    @Test
    public void getServiceName_dubboAdaptEnabled_allProvided() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);

        TestableNacosConnector connector = new TestableNacosConnector(ctx);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(DUBBO_CATEGORY_KEY, "consumers");
        metadata.put(DUBBO_VERSION_KEY, "2.0.0");
        metadata.put(DUBBO_GROUP_KEY, "groupB");
        CommonProviderRequest req = buildProviderRequest("com.example.IFoo", metadata);
        Assert.assertEquals("consumers:com.example.IFoo:2.0.0:groupB", connector.exposeGetServiceName(req));
    }

    /**
     * dubboAdapt=true，nacosContext.serviceName 优先于 interface 名
     */
    @Test
    public void getServiceName_dubboAdaptEnabled_serviceNameOverride() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);
        ctx.setServiceName("overrideService");

        TestableNacosConnector connector = new TestableNacosConnector(ctx);

        CommonProviderRequest req = buildProviderRequest("com.example.IFoo", null);
        // serviceName override 后 dubboAdapt 仍生效，以 overrideService 为 interface 部分
        Assert.assertEquals("providers:overrideService::", connector.exposeGetServiceName(req));
    }

    // ---- 辅助方法 ----

    private CommonProviderRequest buildProviderRequest(String service, Map<String, String> metadata) {
        CommonProviderRequest req = new CommonProviderRequest();
        req.setService(service);
        req.setNamespace("default");
        if (metadata != null) {
            req.setMetadata(metadata);
        }
        return req;
    }

    // ---- 发现侧 ----

    /**
     * dubboAdapt=false，映射表即便有也不生效，返回原 polaris 服务名
     */
    @Test
    public void resolveSubscribeName_dubboAdaptDisabled_returnsOriginal() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(false);
        ctx.putServiceNameMapping("com.example.IFoo", "providers:com.example.IFoo:1.0.0:groupA");

        TestableNacosService service = new TestableNacosService(ctx);

        ServiceUpdateTask task = buildTask("com.example.IFoo");
        Assert.assertEquals("com.example.IFoo", service.exposeResolveSubscribeName(task));
    }

    /**
     * dubboAdapt=true，映射表为空，透传原名
     */
    @Test
    public void resolveSubscribeName_dubboAdaptEnabled_emptyMappings_returnsOriginal() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);

        TestableNacosService service = new TestableNacosService(ctx);

        ServiceUpdateTask task = buildTask("com.example.IFoo");
        Assert.assertEquals("com.example.IFoo", service.exposeResolveSubscribeName(task));
    }

    /**
     * dubboAdapt=true，映射表中无该 polaris 名，透传原名
     */
    @Test
    public void resolveSubscribeName_dubboAdaptEnabled_noMatch_returnsOriginal() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);
        ctx.putServiceNameMapping("com.example.IOther", "providers:com.example.IOther:1.0.0:groupA");

        TestableNacosService service = new TestableNacosService(ctx);

        ServiceUpdateTask task = buildTask("com.example.IFoo");
        Assert.assertEquals("com.example.IFoo", service.exposeResolveSubscribeName(task));
    }

    /**
     * dubboAdapt=true，映射命中，返回映射值
     */
    @Test
    public void resolveSubscribeName_dubboAdaptEnabled_hit_returnsMappedName() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);
        ctx.putServiceNameMapping("com.example.IFoo", "providers:com.example.IFoo:1.0.0:groupA");

        TestableNacosService service = new TestableNacosService(ctx);

        ServiceUpdateTask task = buildTask("com.example.IFoo");
        Assert.assertEquals("providers:com.example.IFoo:1.0.0:groupA",
                service.exposeResolveSubscribeName(task));
    }

    /**
     * dubboAdapt=true，通过 put(k, "") 移除后，回退到透传原名
     */
    @Test
    public void resolveSubscribeName_dubboAdaptEnabled_emptyValueRemoved_returnsOriginal() {
        NacosContext ctx = new NacosContext();
        ctx.setDubboAdapt(true);
        ctx.putServiceNameMapping("com.example.IFoo", "providers:com.example.IFoo:1.0.0:groupA");
        ctx.putServiceNameMapping("com.example.IFoo", "");

        TestableNacosService service = new TestableNacosService(ctx);

        ServiceUpdateTask task = buildTask("com.example.IFoo");
        Assert.assertEquals("com.example.IFoo", service.exposeResolveSubscribeName(task));
    }

    private ServiceUpdateTask buildTask(String service) {
        ServiceKey sk = new ServiceKey("default", service);
        ServiceEventKey eventKey = new ServiceEventKey(sk, ServiceEventKey.EventType.INSTANCE);
        return new StubServiceUpdateTask(eventKey);
    }

    /**
     * 测试用 ServiceUpdateTask stub，仅实现 getServiceEventKey()。
     */
    static class StubServiceUpdateTask extends ServiceUpdateTask {

        private final ServiceEventKey eventKey;

        public StubServiceUpdateTask(ServiceEventKey eventKey) {
            super(null, null);
            this.eventKey = eventKey;
        }

        @Override
        public ServiceEventKey getServiceEventKey() {
            return eventKey;
        }

        @Override
        public void execute() {}

        @Override
        public void execute(ServiceUpdateTask task) {}

        @Override
        protected void handle(Throwable throwable) {}

        @Override
        public boolean notifyServerEvent(ServerEvent serverEvent) {
            return false;
        }
    }
}
