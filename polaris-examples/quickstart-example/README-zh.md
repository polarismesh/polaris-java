# 快速开始样例

## 样例说明

本样例演示如何使用 polaris-java 完成被调端以及主调端应用接入polaris，并完成服务调用流程。

## 样例

### 如何接入

1. 首先，修改 pom.xml 文件，引入 polaris-all。
```
<dependency>
    <groupId>com.tencent.polaris</groupId>
    <artifactId>polaris-all</artifactId>
</dependency>
```

2. 在应用的 /src/main/resources/polaris.yml 配置文件中配置 Polaris Server 地址。
```
global:
  serverConnector:
    addresses:
    - 127.0.0.1:8091
```

### 执行样例

1. IDE直接启动：
找到 quickstart-example 项目的主类 QuickStartExample，编辑启动参数为 -namespace \<namespace\> -service \<service_name\> ，执行 main 方法启动样例。

2. 打包编译后启动：
在 quickstart-example 项目中执行 mvn clean package 将工程编译打包，然后执行 java -jar quickstart-example.jar  -namespace \<namespace\> -service \<service_name\>  启动样例。