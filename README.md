# Polaris Java

[![codecov](https://codecov.io/gh/polarismesh/polaris-java/branch/main/graph/badge.svg?token=4M42F4S0FR)](https://codecov.io/gh/polarismesh/polaris-java)
[![Build Status](https://github.com/polarismesh/polaris-java/actions/workflows/testing.yml/badge.svg)](https://github.com/PolarisMesh/polaris-java/actions/workflows/testing.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.polaris/polaris-dependencies?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.polaris%20AND%20a:polaris-dependencies)

English | [简体中文](./README-zh.md)

---

README：

- [Introduction](#introduction)
- [How to build](#how-to-build)
- [How to use](#how-to-use)
- [Examples](#examples)
- [Frameworks](#frameworks)

## Introduction

polaris-java is the Java SDK, support SDK API for application to use polaris

## How to build

polaris-java uses Maven for most build-related activities, and JDK 1.8 or later versions are supported.
You should be able to get off the ground quite quickly by cloning the project you are interested in and typing:
 ```
 mvn clean install
 ```

## How to use

Modify pom.xml in application root, add dependencyManagement for polaris-java:

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

add the module in dependencies.

## Examples

A polaris-examples module is included in our project for you to get started with polaris-java quickly. It contains an example, and you can refer to the readme file in the example project for a quick walkthrough.

- [QuickStart Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/quickstart-example)
- [Discovery Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/discovery-example)
- [Router Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/router-example)
- [CircuitBreaker Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/circuitbreaker-example)
- [RateLimit Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/ratelimit-example)
- [Configuration Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/configuration-example)

## Frameworks

Developers usually use HTTP or RPC frameworks to develop distributed service. Polaris Java SDK is already integrated into some open source development frameworks. If using these frameworks, you can enable Polaris Service Governance functions without using Polaris Java SDK directly.

- Spring Boot
  - [spring-cloud-tencent](https://github.com/Tencent/spring-cloud-tencent)
  - [spring-boot-polaris](https://github.com/polarismesh/spring-boot-polaris)
- Dubbo
  - [registry, discovery and routing](https://github.com/apache/dubbo-spi-extensions/tree/master/dubbo-registry-extensions)
  - [circuit breaker and rate limiter](https://github.com/apache/dubbo-spi-extensions/tree/master/dubbo-filter-extensions)
- grpc-java
  - [grpc-java-polaris](https://github.com/polarismesh/grpc-java-polaris)
