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

package com.tencent.polaris.client.api;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.pojo.InstanceGauge;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 服务上报数据处理器统一接口
 */
public interface ServiceCallResultListener {

    /**
     * 作为KEY存放在context中进行索引
     */
    String CONTEXT_KEY_RESULT_LISTENERS = "serviceCallResultListeners";

    /**
     * 初始化handler
     *
     * @param context SDK上下文
     */
    void init(SDKContext context);

    /**
     * 收到服务上报数据后的回调处理
     *
     * @param result 上报数据
     */
    void onServiceCallResult(InstanceGauge result);

    /**
     * 停机释放资源
     */
    void destroy();

    /**
     * 从全局变量中获取监听器数组
     *
     * @param sdkContext 全局上下文
     * @return 监听器数组
     */
    static List<ServiceCallResultListener> getServiceCallResultListeners(SDKContext sdkContext) {
        synchronized (ServiceCallResultListener.class) {
            List<ServiceCallResultListener> serviceCallResultListeners = sdkContext.getValueContext()
                    .getValue(CONTEXT_KEY_RESULT_LISTENERS);
            if (null != serviceCallResultListeners) {
                return serviceCallResultListeners;
            }
            serviceCallResultListeners = new ArrayList<>();
            ServiceLoader<ServiceCallResultListener> listeners = ServiceLoader.load(ServiceCallResultListener.class);
            for (ServiceCallResultListener listener : listeners) {
                listener.init(sdkContext);
                serviceCallResultListeners.add(listener);
            }
            final List<ServiceCallResultListener> outListeners = serviceCallResultListeners;
            sdkContext.registerDestroyHook(new Destroyable() {
                @Override
                protected void doDestroy() {
                        for (ServiceCallResultListener listener : outListeners) {
                            listener.destroy();
                        }
                    }
                });
            sdkContext.getValueContext().setValue(CONTEXT_KEY_RESULT_LISTENERS, outListeners);
            return serviceCallResultListeners;
        }
    }
}
