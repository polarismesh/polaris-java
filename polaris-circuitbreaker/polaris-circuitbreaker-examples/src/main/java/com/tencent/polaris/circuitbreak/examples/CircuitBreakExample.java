package com.tencent.polaris.circuitbreak.examples;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.examples.CircuitBreakExampleUtils.InitResult;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

/**
 * Example for how to use polaris circuit breaking
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class CircuitBreakExample {

    public static void main(String[] args) throws Throwable {
        InitResult initResult = CircuitBreakExampleUtils.initConfiguration(args);
        //由于需要用到多个API对象，因此可以使用SDKContext使得多个API对象的资源都可以共享
        try (SDKContext sdkContext = SDKContext.initContext()) {
            ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);
            //1. 先注册服务实例
            registerInstances(providerAPI, initResult);
            //2. 获取可用的服务实例列表
            ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
            Instance[] availableInstances = getAvailableInstances(consumerAPI, initResult);
            System.out.println("available instances count is " + availableInstances.length);
            //3. 针对某个服务实例上报调用结果失败，默认连续10次失败，或者1分钟错误率50%，就会对实例进行熔断
            Instance targetInstance = availableInstances[0];
            System.out.printf("target instance is %s:%d%n", targetInstance.getHost(), targetInstance.getPort());
            for (int i = 0; i < 120; i++) {
                //这里要进行服务调用，用户可以写自己的服务调用代码
                for (Instance instance : availableInstances) {
                    ServiceCallResult serviceCallResult = new ServiceCallResult();
                    serviceCallResult.setInstance(targetInstance);
                    if (instance.getHost().equals(targetInstance.getHost()) && instance.getPort() == targetInstance
                            .getPort()) {
                        serviceCallResult.setRetStatus(RetStatus.RetFail);
                        serviceCallResult.setDelay(2000);
                    } else {
                        serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                        serviceCallResult.setDelay(20);
                    }
                    consumerAPI.updateServiceCallResult(serviceCallResult);
                }
                System.out.printf("report call result %d%n", i);
                CircuitBreakExampleUtils.doSleep(500);
            }
            //4. 判断服务实例是否还在可用列表中，已经熔断的实例是不会再次返回
            availableInstances = getAvailableInstances(consumerAPI, initResult);
            System.out.println("available instances count is " + availableInstances.length);
            for (Instance instance : availableInstances) {
                System.out.printf("available instance is %s:%d%n", instance.getHost(), instance.getPort());
            }
            //4. 反注册服务实例
            System.out.println("start to deregister instances");
            deregisterInstances(providerAPI, initResult);
        }
    }

    private static Instance[] getAvailableInstances(ConsumerAPI consumerAPI, InitResult initResult) {
        GetInstancesRequest getInstancesRequest = new GetInstancesRequest();
        getInstancesRequest.setNamespace(initResult.getNamespace());
        getInstancesRequest.setService(initResult.getService());
        InstancesResponse instances = consumerAPI.getInstances(getInstancesRequest);
        return instances.getInstances();
    }

    private static void registerInstances(ProviderAPI providerAPI, InitResult initResult) {
        for (int i = 0; i < initResult.getInstanceCount(); i++) {
            InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
            instanceRegisterRequest.setNamespace(initResult.getNamespace());
            instanceRegisterRequest.setService(initResult.getService());
            instanceRegisterRequest.setHost("127.0.0.1");
            instanceRegisterRequest.setPort(13000 + i);
            instanceRegisterRequest.setToken(initResult.getToken());
            InstanceRegisterResponse instanceRegisterResponse = providerAPI.register(instanceRegisterRequest);
            System.out.println("response after register is " + instanceRegisterResponse);
        }
        CircuitBreakExampleUtils.doSleep(5000);
    }

    private static void deregisterInstances(ProviderAPI providerAPI, InitResult initResult) {
        for (int i = 0; i < initResult.getInstanceCount(); i++) {
            InstanceDeregisterRequest instanceDeregisterRequest = new InstanceDeregisterRequest();
            instanceDeregisterRequest.setNamespace(initResult.getNamespace());
            instanceDeregisterRequest.setService(initResult.getService());
            instanceDeregisterRequest.setHost("127.0.0.1");
            instanceDeregisterRequest.setPort(13000 + i);
            instanceDeregisterRequest.setToken(initResult.getToken());
            providerAPI.deRegister(instanceDeregisterRequest);
        }
    }

}
