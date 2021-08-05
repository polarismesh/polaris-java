polaris-java
========================================
[![Build Status](https://github.com/polarismesh/polaris-java/actions/workflows/testing.yml/badge.svg)](https://github.com/PolarisMesh/polaris-java/actions/workflows/testing.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.polaris/polaris-dependencies?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.polaris%20AND%20a:polaris-dependencies)

Polaris is an operation centre that supports multiple programming languages, with high compatibility to different application framework. Polaris - java is Polaris's Java embedded operation SDK.

## Quick Start

### Package Dependencies

#### PreCondition: DependencyManagement
Add the below script into the root pom element <dependencyManagement></dependencyManagement>, after that you can refer all the polaris dependency freely. 
```xml
<dependencyManagement>        
    <dependencies>
        <dependency>
            <groupId>com.tencent.polaris</groupId>
            <artifactId>polaris-dependencies</artifactId>
            <version>${version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
#### Use all functions
   ```xml
   <dependency>
       <groupId>com.tencent.polaris</groupId>
       <artifactId>polaris-factory</artifactId>
   </dependency>
   ```
#### Use discovery only
   ```xml
   <dependency>
       <groupId>com.tencent.polaris</groupId>
       <artifactId>polaris-discovery-factory</artifactId>
   </dependency>
   ```
#### Use circuitbreak and degrade only
   ```xml
   <dependency>
       <groupId>com.tencent.polaris</groupId>
       <artifactId>polaris-circuitbreaker-factory</artifactId>
   </dependency>
   ```
#### Use ratelimit
   ```xml
   <dependency>
       <groupId>com.tencent.polaris</groupId>
       <artifactId>polaris-ratelimit-factory</artifactId>
   </dependency>
   ```   

### User manual

You can find detail in：[using polaris-java](https://github.com/PolarisMesh/website/blob/main/docs/zh/doc/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8/%E4%BD%BF%E7%94%A8polaris-java.md)

## License

The polaris-java is licensed under the BSD 3-Clause License. Copyright and license information can be found in the file [LICENSE](LICENSE)

