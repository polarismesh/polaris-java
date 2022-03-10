polaris-java
========================================
[![Build Status](https://github.com/polarismesh/polaris-java/actions/workflows/testing.yml/badge.svg)](https://github.com/PolarisMesh/polaris-java/actions/workflows/testing.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.polaris/polaris-dependencies?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.polaris%20AND%20a:polaris-dependencies)

polaris-java是北极星网格的Java语言SDK，供Java语言的应用通过接口调用的方式接入北极星网格。

## 如何构建

polaris-java使用Maven进行构建，最低支持JDK 1.8。将本项目clone到本地后，执行以下命令进行构建：
```
mvn clean install
```

## 如何使用

### 如何引入依赖

在 dependencyManagement 中添加如下配置：
```xml
<dependencyManagement>        
    <dependencies>
        <dependency>
            <groupId>com.tencent.polaris</groupId>
            <artifactId>polaris-dependencies</artifactId>
            <version>1.2.2-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
然后在 dependencies 中添加自己所需使用的依赖即可使用。

## 功能样例

为了演示功能如何使用，polaris-java 项目包含了一个子模块polaris-examples。此模块中提供了演示用的 example ，您可以阅读对应的 example 工程下的 README-zh 文档，根据里面的步骤来体验。

[快速开始样例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/quickstart-example/README-zh.md)

[注册发现样例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/discovery-example)

[动态路由样例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/router-example)

[故障熔断样例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/circuitbreaker-example)

[限流样例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/ratelimit-example)

[配置中心样例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/configuration-example)





