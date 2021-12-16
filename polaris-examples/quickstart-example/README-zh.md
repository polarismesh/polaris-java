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

- 启动被调方：找到 `quickstart-example-provider` 项目的主类 `Provider`，执行 main 方法启动。
- 启动主调方：找到 `quickstart-example-consumer` 项目的主类 `Consumer`，执行 main 方法启动。

2. 打包编译后启动：

- 启动被调方：找到 `quickstart-example-provider` 项目下，执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar quickstart-example-provider-${version}.jar`
- 启动主调方：找到 `quickstart-example-consumer` 项目下，执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar quickstart-example-consumer-${version}.jar`