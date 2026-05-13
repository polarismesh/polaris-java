# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

polaris-java 是 [Polaris](https://github.com/polarismesh/polaris)（腾讯开源服务治理平台）的 Java SDK，为微服务提供服务发现、路由、熔断、限流和配置管理能力。

## 通用规范

- 所有对话和回复必须使用中文

## 方案设计工作流

**大型需求开发或非平凡 bugfix 在动手写代码前必须先出方案。** 触发条件（满足任一即视为「大型」）：

- 跨 2 个及以上模块的改动
- 新增插件、新增 API、改动 SPI 或公开接口
- 涉及并发模型、缓存、连接器协议、版本兼容等架构面影响
- bug 根因不清晰、需要先调研复现路径

流程：

1. 使用 superpowers 提供的相关 skill 完成方案设计：
   - `superpowers:brainstorming` — 探索用户意图、需求边界与设计空间（创意/新功能场景）
   - `superpowers:systematic-debugging` — 系统化定位 bug 根因（疑难 bug 场景）
   - `superpowers:writing-plans` — 把方案写成可执行的多步实施计划
2. 方案文档及相关材料保存到 `specs/<YYYY-MM-DD-short-topic>/` 子目录下，按需求/bugfix 一个子目录隔离：
   - `specs/<YYYY-MM-DD-short-topic>/design.md` — 主设计文档，包含：背景、目标、非目标、方案对比、最终选型、影响面、实施步骤、回滚策略、测试计划
   - 同目录下放置该需求相关的其他材料：调研笔记、时序图、抓包/日志样本、PoC 代码片段、参考资料链接等
   - 例：`specs/2026-05-13-ratelimit-multi-window/design.md`、`specs/2026-05-13-ratelimit-multi-window/benchmark.md`
   - 注：`specs/` 已在 `.gitignore` 中（仅本地 / AI 工作目录使用，不入库），无需也不要提交到远程仓库
3. 与用户确认方案后，再用 `superpowers:executing-plans` 或 `superpowers:test-driven-development` 进入实现阶段。

小改动（typo、单文件局部修复、纯文档调整）可直接进入实现。

## 构建与测试命令

```bash
# 构建整个项目
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 运行所有测试
mvn clean test

# 运行指定模块的测试（-am 自动构建依赖模块，-DfailIfNoTests=false 跳过无测试模块报错）
mvn test -pl polaris-discovery/polaris-discovery-api -am -DfailIfNoTests=false

# 运行指定测试类
mvn test -Dtest=ClassName -pl module/submodule -am -DfailIfNoTests=false

# 使用 Sonatype profile 构建（CI 中使用）
mvn clean test -B -U -Psonatype

# 仅打包，不跑测试也不安装到本地仓库（最快验证编译通过）
mvn -DskipTests -B clean package

# 运行 Checkstyle 检查
mvn checkstyle:check
```

### 版本号

根 `pom.xml` 通过 `<revision>` 属性统一管理所有子模块版本（当前如 `2.1.2.0-RC1`）。构建时由 `flatten-maven-plugin` 生成根目录的 `.flattened-pom.xml`，该文件会被自动更新，**禁止手工编辑**。改版本号只需改根 pom 的 `<revision>`。

## 架构说明

### 模块结构

项目为 Maven 多模块工程，按层次组织：

**基础层：**
- `polaris-common/` — 核心工具，拆分为子模块：`polaris-client`（SDKContext、插件管理、Flow 基类）、`polaris-config`（配置接口定义）、`polaris-config-default`（默认配置实现）、`polaris-model`（共享 POJO）、`polaris-metadata`、`polaris-logging`、`polaris-protobuf`（protobuf 定义）、`polaris-encrypt`（加解密）、`polaris-threadlocal`（线程上下文传递）
- `polaris-factory/` — 聚合入口，`APIFactory` 委托各功能 Factory 创建所有 API 对象
- `polaris-assembly/` — 组合客户端（`assembly-client` 和 `assembly-factory`），封装跨功能模块的聚合流程

**功能模块**（每个模块均拆分为 `-api` 和 `-client`/`-factory` 子模块）：
- `polaris-discovery/` — `ConsumerAPI`（获取实例、路由、上报调用结果）和 `ProviderAPI`（注册/注销/心跳）
- `polaris-router/` — `RouterAPI`，支持规则路由、元数据路由、金丝雀、就近、泳道路由
- `polaris-circuitbreaker/` — `CircuitBreakAPI`，支持错误数和错误率熔断策略
- `polaris-ratelimit/` — `LimitAPI`，提供配额检查与限流
- `polaris-configuration/` — `ConfigFileService`，远程配置文件管理
- `polaris-auth/` — 认证与鉴权
- `polaris-fault/` — 故障注入
- `polaris-certificate/` — 证书管理

**插件系统：**
- `polaris-plugins/polaris-plugin-api/` — 插件接口定义与 `PluginType` 类（通过 `TypeProvider` SPI 注册）
- `polaris-plugins/polaris-plugins-connector/` — 服务端连接器：`connector-polaris-grpc`（默认）、`connector-consul`、`connector-nacos`、`connector-composite`
- `polaris-plugins/polaris-plugins-router/` — 路由插件：`router-rule`、`router-metadata`、`router-canary`、`router-nearby`、`router-lane`、`router-set`、`router-healthy`、`router-isolated`、`router-namespace`、`router-mirroring`
- `polaris-plugins/polaris-plugins-loadbalancer/` — 负载均衡策略
- `polaris-plugins/polaris-plugins-circuitbreaker/` — 熔断器实现
- `polaris-plugins/polaris-plugins-ratelimiter/` — 限流器实现
- `polaris-plugins/polaris-plugins-observability/` — 可观测性：指标（Prometheus）、链路追踪（OpenTelemetry）
- `polaris-plugins/polaris-plugins-healthchecker/` — 健康检查：HTTP、TCP、UDP
- `polaris-plugins/polaris-plugins-cache/` — 本地缓存实现
- `polaris-plugins/polaris-plugins-lossless/` — 无损上下线功能
- `polaris-plugins/polaris-plugins-registry/` — 服务注册实现
- `polaris-plugins/polaris-plugins-location/` — 地理位置感知
- `polaris-plugins/polaris-plugins-auth/` — 鉴权插件实现
- `polaris-plugins/polaris-plugins-certificate/` — 证书插件实现
- `polaris-plugins/polaris-plugins-configuration-connector/` — 配置中心连接器
- `polaris-plugins/polaris-plugins-configfilefilter/` — 配置文件过滤器

**其他：**
- `polaris-test/` — 共享测试工具与 Mock
- `polaris-examples/` — 各功能可运行示例
- `polaris-dependencies/` — BOM（dependencyManagement 聚合），下游接入用
- `polaris-distribution/polaris-all/` — 全功能 uber-jar 发行包，便于一键依赖

### 核心设计模式

**SDKContext 作为核心枢纽：** 所有 API 对象均通过 `SDKContext` 创建，它负责管理插件生命周期（`PluginManager`）、配置和共享状态（`Extensions`）。共享同一个 `SDKContext` 的多个 API 对象复用同一连接和缓存。

**插件架构：** 功能以插件形式实现，注册至 `PluginManager`。插件通过 Java SPI（`META-INF/services/`）发现，`TypeProvider` 接口声明插件类型及初始化优先级（`level` 字段决定顺序），并通过 `polaris.yaml` 配置。每个插件实现 `polaris-plugin-api` 下对应的类型接口。

**API 优先分层：** 每个功能模块有 `-api` 子模块（纯接口 + 请求/响应模型，无实现）和 `-client`/`-factory` 子模块（组装实现）。调用方只依赖 `-api`。

**Flow 流水线处理：** 运行时调用（如 `getInstances`）经过 `polaris-client` 中的 Flow 类（`AbstractFlow` 子类），按顺序调用插件链：缓存 → 连接器 → 路由器 → 负载均衡 → 熔断器。

### 典型 API 使用方式

```java
// 所有 API 均从 APIFactory 或共享的 SDKContext 获取
SDKContext context = APIFactory.initContext();
ConsumerAPI consumer = APIFactory.createConsumerAPIByContext(context);
ProviderAPI provider = APIFactory.createProviderAPIByContext(context);
LimitAPI limit = APIFactory.createLimitAPIByContext(context);
// 关闭时释放 context
context.close();
```

`SDKContext` 实现了 `AutoCloseable`，推荐在长生命周期组件持有它，并在应用退出时统一 `close()`，避免重复创建导致连接/缓存重复初始化。

### 配置

默认从 classpath 的 `conf/polaris.yaml` 加载配置。也可通过 `ConfigAPIFactory` 编程式构建 `Configuration` 对象，或从流中加载。插件行为通过 YAML 配置控制。

### 新增插件的关键步骤

1. 在对应的 `polaris-plugins-xxx/` 下创建子模块
2. 实现 `polaris-plugin-api` 下的插件接口
3. 在 `META-INF/services/` 下注册 SPI 文件（文件名为接口全限定名，内容为实现类全限定名）
4. 如需声明新插件类型，实现 `TypeProvider` 接口并注册 SPI

### Java 版本兼容性

源码级别为 Java 8（`maven.compiler.source=1.8`）。CI 在 Java 8、11、17、21、25 上测试，禁止使用高版本 API。

## 下游接入方式

下游项目通过 `polaris-dependencies` BOM 统一管理版本：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.tencent.polaris</groupId>
      <artifactId>polaris-dependencies</artifactId>
      <version>${polaris.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

随后在 `dependencies` 中声明所需子模块（如 `polaris-discovery-factory`、`polaris-ratelimit-factory`）。本仓库改版本号只需改根 `pom.xml` 的 `<revision>`，BOM 会跟随生成。

## 代码风格

项目通过 Checkstyle 强制执行编码规范（见 `checkstyle/checkstyle.xml`），关键约束如下：
- **禁止通配符导入**（`import foo.*`）和**静态导入**
- **禁止行尾空白**；文件末尾必须有换行符
- **禁止 Tab 字符**（使用 4 空格缩进）
- **禁止连续空行**（代码块间最多 1 个空行）
- 所有公共类和接口必须有 Javadoc；第一句话必须以大写字母开头
- 变量/成员/参数名须匹配 `^(id)|([a-z][a-z0-9][a-zA-Z0-9]+)$`（除 `id` 外至少 2 个字符）
- 每个方法最多 1 个 return 语句；最多 30 条可执行语句；for 循环最多嵌套 2 层
- 每个 `.java` 文件必须以 BSD 3-Clause 许可证头开始
- `@SuppressWarnings` 仅允许用于 `unchecked`、`deprecation`、`rawtypes`、`resource`

## License 文件头

所有新 Java 文件必须以如下内容开头：

```java
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
```

> 注意：仓库根 `.licenserc.yaml` 的 license 校验只覆盖 `**/tencent/**` 路径。新增文件即使不在该路径，也必须按上述格式手工添加 license 头，Checkstyle 与 Code Review 会校验。

## CI

GitHub Actions 在 Java 8、11、17、21、25 多个版本及多个操作系统平台上运行测试。PR 需通过所有检查后方可合并。贡献代码请提交至 `main` 分支。

## Pull Request 规范

创建 PR 时必须提交到上游仓库 `polarismesh/polaris-java`，而不是 fork 仓库。使用以下命令：

```bash
gh pr create --repo polarismesh/polaris-java --title "..." --body "..."
```

## Git 提交规范

详见 @.claude/rules/git-commit.md

## 单元测试规范

详见 @.claude/rules/unit-test.md

