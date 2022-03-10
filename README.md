polaris-java
========================================
[![Build Status](https://github.com/polarismesh/polaris-java/actions/workflows/testing.yml/badge.svg)](https://github.com/PolarisMesh/polaris-java/actions/workflows/testing.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.polaris/polaris-dependencies?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.polaris%20AND%20a:polaris-dependencies)

See the [中文文档](https://github.com/polarismesh/polaris-java/blob/master/README-zh.md) for Chinese readme.

polaris-java is the Java language SDK for polarismesh, support SDK API for application to use polaris

## How to build

polaris-java uses Maven for most build-related activities, and JDK 1.8 or later versions are supported.
You should be able to get off the ground quite quickly by cloning the project you are interested in and typing:
 ```
 mvn clean install
 ```
 
 ## How to Use
 
 ### Add maven dependency
 
 These artifacts are available from Maven Central via BOM:
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
 add the module in dependencies.
 
 ## Examples
 
 A polaris-examples module is included in our project for you to get started with polaris-java quickly. It contains an example, and you can refer to the readme file in the example project for a quick walkthrough.
 
 [QuickStart Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/quickstart-example)
 
 [Discovery Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/discovery-example)
 
 [Router Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/router-example)
 
 [CircuitBreaker Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/circuitbreaker-example)
 
 [RateLimit Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/ratelimit-example)
 
[Configuration Example](https://github.com/polarismesh/polaris-java/tree/main/polaris-examples/configuration-example)
