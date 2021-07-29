polaris-java
========================================
北极星polaris是一个支持多种开发语言、兼容主流开发框架的服务治理中心。polaris-java是北极星的Java语言嵌入式服务治理SDK

## 快速入门

### 包依赖

#### 大前提：依赖管理
在工程根目录的pom中的<dependencyManagement></dependencyManagement>添加如下配置，即可在项目中引用需要的polaris-java子模块依赖。
```xml
<dependencyManagement>        
    <dependencies>
        <dependency>
            <groupId>com.tencent.nameservice</groupId>
            <artifactId>polaris-dependencies</artifactId>
            <version>${version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
#### 使用全量功能
   ```xml
   <dependency>
       <groupId>com.tencent.nameservice</groupId>
       <artifactId>polaris-factory</artifactId>
   </dependency>
   ```
#### 仅服务注册发现
   ```xml
   <dependency>
       <groupId>com.tencent.nameservice</groupId>
       <artifactId>polaris-discovery-factory</artifactId>
   </dependency>
   ```
#### 仅使用熔断降级
   ```xml
   <dependency>
       <groupId>com.tencent.nameservice</groupId>
       <artifactId>polaris-circuitbreaker-factory</artifactId>
   </dependency>
   ```
#### 仅使用服务限流
   ```xml
   <dependency>
       <groupId>com.tencent.nameservice</groupId>
       <artifactId>polaris-ratelimit-factory</artifactId>
   </dependency>
   ```   

### 功能使用

各组件功能以及接口使用方式可参考对外开源文档：[使用polaris-java](https://github.com/PolarisMesh/website/blob/main/docs/zh/doc/%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8/%E4%BD%BF%E7%94%A8polaris-java.md)

## License

The polaris-java is licensed under the BSD 3-Clause License. Copyright and license information can be found in the file [LICENSE](LICENSE)

