# Polaris Java

[![codecov](https://codecov.io/gh/polarismesh/polaris-java/branch/main/graph/badge.svg?token=4M42F4S0FR)](https://codecov.io/gh/polarismesh/polaris-java)
[![Build Status](https://github.com/polarismesh/polaris-java/actions/workflows/testing.yml/badge.svg)](https://github.com/PolarisMesh/polaris-java/actions/workflows/testing.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.polaris/polaris-dependencies?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.polaris%20AND%20a:polaris-dependencies)

[English](./README.md) | 简体中文

---

README：

- [介绍](#介绍)
- [如何构建](#如何构建)
- [如何使用](#如何使用)
- [使用示例](#使用示例)
- [开发框架](#开发框架)

## 介绍

polaris-java是北极星网格的Java语言SDK，供Java语言的应用通过接口调用的方式接入北极星网格。

## 如何构建

polaris-java使用Maven进行构建，最低支持JDK 1.8。将本项目clone到本地后，执行以下命令进行构建：
```
mvn clean install
```

## 如何使用

修改应用程序的pom.xml，在 dependencyManagement 中添加如下配置：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tencent.polaris</groupId>
            <artifactId>polaris-dependencies</artifactId>
            <version>1.9.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后在 dependencies 中添加自己所需使用的依赖即可使用。

## 使用示例

为了演示功能如何使用，polaris-java 项目包含了一个子模块polaris-examples。此模块中提供了演示用的 example ，您可以阅读对应的 example 工程下的 README-zh 文档，根据里面的步骤来体验。

- [快速开始](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/quickstart-example/README-zh.md)
- [注册发现示例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/discovery-example)
- [动态路由示例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/router-example)
- [熔断示例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/circuitbreaker-example)
- [限流示例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/ratelimit-example)
- [配置中心示例](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/configuration-example)

## 开发框架

通常，开发者使用开源或者自研的 HTTP 或者 RPC 框架开发分布式服务。北极星提供多个框架和北极星 SDK 的集成方案和实现。如果你在使用这些框架，不需要直接调用北极星 SDK，就可以使用北极星的服务治理功能。

- Spring Boot
  - [spring-cloud-tencent](https://github.com/Tencent/spring-cloud-tencent)
  - [spring-boot-polaris](https://github.com/polarismesh/spring-boot-polaris)
- Dubbo
  - [registry, discovery and routing](https://github.com/apache/dubbo-spi-extensions/tree/master/dubbo-registry-extensions)
  - [circuit breaker and rate limiter](https://github.com/apache/dubbo-spi-extensions/tree/master/dubbo-filter-extensions)
- grpc-java
  - [grpc-java-polaris](https://github.com/polarismesh/grpc-java-polaris)
