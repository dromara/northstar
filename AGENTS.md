# Northstar Pro - AGENTS.md

> 本文件包含 AI 编程代理处理 Northstar Pro 项目所需的关键信息。信息基于项目实际文件整理，如发现与源码不一致，请以源码为准。

## 项目概述

**Northstar Pro（盈富量化交易平台）** 是一个基于 Java 和 Spring Boot 开发的量化交易平台，提供模块化的策略开发、回测、模拟交易和实盘部署框架。

- **当前源码版本**: `9.0.0-M2`（定义在根 `pom.xml` 的 `<northstar-version>` 属性）
- **当前分支**: `upgrade/jdk25`
- **语言**: Java 25（源码 `pom.xml` 当前声明）
- **构建工具**: Maven 3.x
- **框架**: Spring Boot 3.5.11
- **Group ID**: `org.dromara.northstar.pro`
- **Artifact ID**: `northstar`
- **仓库**: `https://gitee.com/dromara/northstar`
- **前端子模块**: `northstar-monitor-next`（Git 子模块，独立仓库 `git@gitee.com:kevinhuangwl/northstar-monitor-next.git`）

### 重要说明：JDK 25 升级

当前检出分支为 `upgrade/jdk25`，源码 `pom.xml` 已声明 `<java.version>25</java.version>`，Spring Boot 已升级到 `3.5.11`。`env.sh` / `env.ps1` 环境脚本会自动安装 Oracle JDK 25。工作目录中存在的 `.flattened-pom.xml` 会在构建时重新生成，未纳入 Git 跟踪，应以源码 `pom.xml` 为准。

## 技术栈

### 后端

- **Spring Boot 3.5.11** - 应用框架
- **Spring Data JPA** - 数据持久化
- **Spring Web** - REST API 层
- **Spring Scheduling** - 任务调度
- **Spring Retry 2.0.5** - 重试机制
- **Spring Boot Cache** - 缓存支持
- **H2 Database 2.2.224** - 嵌入式数据库（生产环境文件模式，测试内存模式）
- **Protocol Buffers 4.28.3** - 数据序列化
- **Disruptor 3.4.4** - 高性能事件处理
- **Netty-SocketIO 2.0.11** / **socket.io-client 1.0.0** - 实时 WebSocket 通信
- **Netty 4.1.104.Final**
- **Lombok 1.18.42** - 减少样板代码
- **Fastjson2 2.0.56** / **Jackson 2.16.1** - JSON 处理
- **Guava 33.0.0-jre** / **Apache Commons**（Lang3 3.17.0、IO 2.15.1、Math3 3.6.1、Codec 1.16.0）- 工具库
- **Hutool-crypto 5.7.22** - 加密
- **OkHttp3 4.12.0** / **Retrofit2 2.11.0** - HTTP 客户端
- **java-jwt 4.4.0** - 仅用于解码数据服务订阅令牌
- **Ehcache 3.10.8**
- **可选 `smart` profile**: TensorFlow Java 0.5.0

### AI Copilot 模块

- **OkHttp3 SSE** / **jtokkit 1.1.0** - Token 管理
- 支持多种 LLM 平台：OpenAI、Moonshot、DeepSeek、Ollama、Hunyuan、Zhipu 等
- Pinecone 向量存储、SearXNG 网页搜索、函数调用框架

### 前端

- **Vue 3.4.27** + **Vite 5.2.12** + **TypeScript 5.4.5**
- **Vue Router 4.3.2** + **Pinia 2.1.7** + **Vue I18n 9.13.1**
- **Element Plus 2.8.4** + **TailwindCSS 3.4.4**
- **Klinecharts 9.8.12** + **ECharts 5.5.0**
- **Monaco Editor 0.55.1**
- **Cypress 15.11.0** - E2E 测试
- 包管理器：**pnpm >= 9**

前端项目有独立的 `northstar-monitor-next/AGENTS.md`，前端开发请优先参考该文件。

## 项目结构

```
northstar/
├── pom.xml                          # 父 POM，定义版本、模块、依赖管理
├── northstar-api/                   # API 模块 - 核心接口、数据模型、指标与策略框架
├── northstar-gateway-sim/           # 模拟网关模块（模拟行情/交易）
├── northstar-gateway-playback/      # 回放网关模块（回测）
├── northstar-strategy-example/      # 策略示例模块
├── northstar-copilot/               # AI 助手模块
├── northstar-main/                  # 主 Spring Boot 应用模块
├── northstar-monitor-next/          # 前端项目（Vue3 + Vite），以 Git 子模块引入
└── northstar-dist/                  # 构建产物目录，非 Maven 模块
```

### 后端源码根包

| 模块 | 根包 |
|---|---|
| `northstar-api` | `org.dromara.northstar` |
| `northstar-main` | `org.dromara.northstar` |
| `northstar-gateway-sim` | `org.dromara.northstar.gateway.sim` |
| `northstar-gateway-playback` | `org.dromara.northstar.gateway.playback` |
| `northstar-strategy-example` | `org.dromara.northstar.strategy.example` |
| `northstar-copilot` | `org.dromara.northstar.copilot` |

### 关键代码目录

- `northstar-api/src/main/java/org/dromara/northstar/common/model/core/` - 核心数据模型（`Tick`、`Bar`、`Contract`、`Order`、`Trade`、`Position`、`Account`、`SubmitOrderReq` 等）
- `northstar-api/src/main/java/org/dromara/northstar/common/constant/` - 公共常量（`Constants.java`）
- `northstar-api/src/main/java/org/dromara/northstar/strategy/` - 策略框架（`TradeStrategy`、`AbstractStrategy`、`IModuleContext`、`IAccount`、`IModuleAccount`）
- `northstar-api/src/main/java/org/dromara/northstar/indicator/` - 指标框架（`Indicator`、`AbstractIndicator`、`Configuration`、`Num`）及指标实现
- `northstar-api/src/main/java/org/dromara/northstar/gateway/` - 网关抽象层（`Gateway`、`MarketGateway`、`TradeGateway`、`IMarketCenter`）
- `northstar-main/src/main/java/org/dromara/northstar/app/restful/` - REST 控制器
- `northstar-main/src/main/java/org/dromara/northstar/app/service/` - 服务层
- `northstar-main/src/main/java/org/dromara/northstar/data/jdbc/` - JPA Repository、Adapter、Entity
- `northstar-main/src/main/java/org/dromara/northstar/event/` - 事件引擎/Disruptor 处理器
- `northstar-main/src/main/java/org/dromara/northstar/module/` - 模组运行时
- `northstar-main/src/main/java/org/dromara/northstar/account/` - 账户与网关管理器
- `northstar-main/src/main/java/org/dromara/northstar/config/` - Spring 配置类
- `northstar-main/src/main/java/org/dromara/northstar/app/auth/`、`app/security/` - 认证与安全
- `northstar-monitor-next/src/` - 前端源码

### 主应用入口

`northstar-main/src/main/java/org/dromara/northstar/NorthstarApplication.java`

```java
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "org.dromara.northstar.data.jdbc")
public class NorthstarApplication { ... }
```

## 关键配置文件

| 文件 | 说明 |
|---|---|
| `pom.xml` | 根 Maven POM，定义版本、模块、依赖管理 |
| `northstar-main/src/main/resources/application.yml` | 主应用配置（Profile、SSL、数据库、日志、虚拟线程） |
| `northstar-main/src/main/resources/logback-spring.xml` | 日志配置 |
| `northstar-monitor-next/package.json` | 前端依赖与脚本 |
| `northstar-monitor-next/vite.config.ts` | Vite 构建配置 |
| `lombok.config` | Lombok 配置（`lombok.addLombokGeneratedAnnotation=true`） |
| `.gitmodules` | Git 子模块配置 |
| `env.sh` / `env.ps1` | Linux / Windows 环境初始化脚本（安装 JDK 25） |
| `startup.sh` | 生产启动脚本 |
| `update-protobuf-obj.ps1` | 重新生成 protobuf Java 类 |

## 构建命令

### 后端

```bash
# 编译打包（默认会执行单元测试）
mvn clean package

# 跳过测试加速构建
mvn clean package -DskipTests

# 包含 TensorFlow 的 smart profile
mvn clean package -P smart

# 运行单元测试
mvn test

# 运行集成测试（Failsafe，当前项目无 *IT.java）
mvn failsafe:integration-test

# 生成覆盖率报告
mvn jacoco:report

# 完整验证（含覆盖率检查）
mvn clean verify
```

构建完成后，`northstar-main` 的可执行 jar 会输出到 `northstar-dist/`，默认名称为 `northstar-9.0.0-M2.jar`。

### 前端

```bash
cd northstar-monitor-next
pnpm install
pnpm dev       # 开发服务（默认端口 8848）
pnpm build     # 生产构建，输出到 dist/
pnpm lint      # eslint + prettier + stylelint
pnpm typecheck # tsc + vue-tsc
pnpm e2e       # Cypress E2E 测试
```

前端 `dist/` 会在 `northstar-main` 的 Maven `generate-resources` 阶段被复制到 `src/main/resources/static/`。因此完整构建时需要先执行 `pnpm build`。

## 测试策略

### 单元测试

- 位置：各模块的 `src/test/java`
- 框架：JUnit 5（Jupiter）、Mockito、AssertJ、Spring Boot Test
- 命名：`*Test.java`
- 插件：Maven Surefire 3.5.3
- 当前项目约有 80+ 个 `*Test.java` 文件

### 集成测试

- 位置：`northstar-main/src/test/java`
- 命名：`*IT.java`
- 插件：Maven Failsafe 3.5.3
- 当前工作树中未发现 `*IT.java` 文件，但 Failsafe 已配置支持

### 测试覆盖率

- 工具：JaCoCo 0.8.13
- 最低行覆盖率：**30%**
- 排除项：
  - `northstar-api`: protobuf 生成代码、指标、策略、合约代码
  - `northstar-gateway-sim`: `org/dromara/northstar/gateway/sim/market/*`

### 测试 Profile

多数 `northstar-main` 的 Spring 测试使用：

```java
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
```

## 运行时配置

主配置文件：`northstar-main/src/main/resources/application.yml`

### Spring Profile

| Profile | 端口 | SSL | 数据库 | 虚拟线程 |
|---|---|---|---|---|
| `prod` / `demo`（默认） | 443 | 启用 | H2 文件模式 `./data/storage` | 启用 |
| `dev` | 80 | 禁用 | H2 文件模式 | 禁用 |
| `test` / `unittest` / `train` / `devtest` | 80 | 禁用 | H2 内存模式 `jdbc:h2:mem:testdb` | 启用 |
| `e2e` | 443 | 启用 | H2 内存模式 | 启用 |

默认激活的 profile 由环境变量 `env` 决定：

```yaml
spring:
  profiles:
    active: ${env:prod}
```

可通过 `-Denv=<profile>` 或 `-Dspring.profiles.active=<profile>` 覆盖。

### SSL/TLS

```yaml
server:
  ssl:
    key-store-type: JKS
    key-store: classpath:keystore/northstar.quantit.tech.jks
    key-store-password: s20ku0o017
    enabled-protocols: TLSv1.2,TLSv1.3
```

### 数据库

- 生产/默认：`jdbc:h2:file:./data/storage;DB_CLOSE_ON_EXIT=FALSE;`
- 测试内存模式：`jdbc:h2:mem:testdb`
- JPA：`ddl-auto: update`，H2Dialect
- H2 Console 在所有 profile 下均启用，路径 `/h2-console`

### 日志

- 使用 SLF4J + Logback
- 日志文件输出到 `logs/` 目录
- 主日志：`logs/Northstar_%d{yyyy-MM-dd}.log`，保留 30 天
- 调试日志：`BroadcastHandler`、`MarketCenter`、`KlineData` 等，保留 3 天

### 外部数据服务

- 数据源基础 URL：`https://marketplace.quantit.tech`
- 订阅令牌通过 `quantit.datasource.secret` 或环境变量 `NS_DS_SECRET` 配置

## 安全考虑

### 认证机制

项目使用**自定义 Session + CSRF Token 方案**，**未使用 Spring Security**。

- `AuthCheckerInterceptor`：对 `/northstar/**` 请求校验 CSRF Token，放行 `/northstar/user/**` 和 `/northstar/reset`
- `UserController`：登录、登出、校验接口
- `UserInfo`：保存用户账号密码及登录失败次数
- 默认账号密码定义在 `northstar-api/src/main/java/org/dromara/northstar/common/constant/Constants.java`：
  - 用户 ID：`admin`
  - 密码：`123456`
- 可通过环境变量覆盖：
  - `NS_USER`
  - `NS_PWD`
- 登录失败 3 次后锁定账户

### CSRF / Session 流程

1. `POST /northstar/user/login` 校验凭据
2. 成功后，随机 UUID 作为 CSRF Token 存入 HTTP Session（键 `SESS_CSRF`）
3. 后续 `/northstar/**` 请求须携带请求头 `CSRF_token` 并匹配 Session 值
4. `InternalLoopCallUtils` 生成一次性 UUID 用于内部循环调用（如 Copilot 函数调用），3 分钟后过期
5. `GET /northstar/user/validate` 检查 Session 有效性

### 其他安全措施

- `MaliciousIPCollector` / `MaliciousRequestFilter`：统计 404 请求 IP，单日超过 10 次则拦截
- `CommonControllerAdvice`：全局异常处理，将 404 请求 IP 记录到 `MaliciousIPCollector`
- Session Cookie 配置：`SameSite=strict`、`secure=true`
- CORS：在 `AppConfig` 中配置，允许任意来源，暴露 `token` 响应头

### JWT

`java-jwt` 仅用于**解码数据服务订阅令牌**（`UserController.payedServices()`），不用于用户认证。

## REST API 控制器

位于 `northstar-main/src/main/java/org/dromara/northstar/app/restful/`，基础路径 `/northstar/...`：

| 控制器 | 功能 |
|---|---|
| `GatewayController` | 网关 CRUD 和连接管理 |
| `ModuleController` | 交易模组管理 |
| `TradeController` | 订单提交和管理 |
| `MarketDataController` | 行情数据查询 |
| `PlaybackController` | 回测回放控制 |
| `StrategyExplorerController` | 策略开发工具 |
| `CustomIndicatorController` | 自定义指标管理 |
| `CopilotChatController` | AI 助手对话接口 |
| `UserController` | 用户认证和管理 |
| `SseStreamController` | 服务器推送实时数据 |
| `MultichartsController` | 多图表数据接口 |
| `LoggingController` | 应用日志访问 |
| `MetaDataController` | 元数据管理 |
| `NotificationController` | 通知设置 |
| `ContractConfigController` | 合约配置 |
| `ResetController` | 系统重置操作（仅在 `e2e`、`dev` profile 启用） |
| `CommonControllerAdvice` | 全局异常处理 |

## 服务层

位于 `northstar-main/src/main/java/org/dromara/northstar/app/service/`，与控制器一一对应：

- `GatewayService` - 网关生命周期管理，启动时恢复网关
- `ModuleService` - 模组生命周期、回测、模拟交易，启动时恢复模组
- `TradeService` - 交易订单处理
- `MarketDataService` - 行情数据服务
- `PlaybackService` - 回放服务
- `StrategyExplorerService` - 策略探索
- `CopilotChatService` - AI 对话
- `ContractConfigService` - 合约配置
- `CustomIndicatorService` - 自定义指标
- `LoggingService` - 日志服务
- `MetaDataService` - 元数据
- `MultichartsService` - 多图表
- `NotificationService` - 通知

## 数据访问层

采用 **Adapter 模式** 包装 Spring Data JPA Repository：

- `northstar-api/src/main/java/org/dromara/northstar/data/` 定义 Repository 接口：
  - `IGatewayRepository`
  - `IModuleRepository`
  - `IMarketDataRepository`
  - `IAccountRepository`
- `northstar-main/src/main/java/org/dromara/northstar/data/jdbc/` 提供：
  - `entity/` - JPA 实体（以 `DO` 为后缀）
  - `*Repository.java` - Spring Data JPA Repository
  - `*Adapter.java` - Repository 适配器
- `RepositoryConfig` 通过 `@Bean` 将 Adapter 暴露为接口实现

部分 JPA 实体采用 JSON 字符串字段（`dataStr`、`dataEncoded`）存储复杂对象，并提供 `convertFrom` / `convertTo` 静态方法。

## 事件引擎

- `DisruptorFastEventEngine` 基于 LMAX Disruptor 实现，RingBuffer 大小 65536
- 事件处理器位于 `northstar-main/src/main/java/org/dromara/northstar/event/`，包括：
  - `AccountHandler` - 账户事件
  - `ConnectionHandler` - 网关连接事件
  - `ModuleHandler` - 模组事件
  - `SseBroadcastHandler` - SSE 实时广播
  - `MarketDataPersistanceHandler` - 行情数据持久化
  - `SimMarketHandler` - 模拟市场
  - `EventNotificationHandler` - 事件通知
  - `IllegalOrderHandler` - 废单处理
  - `PlannedTradeHandler` - 计划交易
  - `KlineDispatcher` - K 线分发
  - `InternalDispatcher` - 内部事件分发

## 代码组织与开发规范

### 命名规范

- 接口：`I` 前缀（如 `IAccount`、`IModuleContext`、`IGatewayRepository`）或无前缀（如 `TradeStrategy`、`Indicator`）
- 抽象类：`Abstract` 前缀（如 `AbstractStrategy`、`AbstractIndicator`、`AbstractEventHandler`）
- 实现类：描述性名称（如 `GatewayManager`、`MarketCenter`）
- 测试类：`*Test.java`（单元测试），`*IT.java`（集成测试）
- JPA 实体：以 `DO` 为后缀（如 `GatewayDescriptionDO`、`BarDO`）
- API 模型：视情况使用 `VO` / `DTO` 后缀

### 代码风格

- 使用 Lombok 减少样板代码（`@Data`、`@Slf4j`、`@Builder`、`@AllArgsConstructor` 等）
- 控制器优先使用构造器注入配合 `private final` 字段
- 服务/管理器类也常见 `@Autowired` 字段注入
- 不可变字段使用 `final`
- 代码注释主要使用中文
- 编码：UTF-8

### 版本管理

- 使用 Maven Flatten Plugin 1.2.5 管理版本
- 版本定义在父 POM：`<northstar-version>9.0.0-M2</northstar-version>`
- 通过 `<revision>${northstar-version}</revision>` 占位符供各模块继承
- 构建时生成 `.flattened-pom.xml`
- 前端 `package.json` 版本同步为 `9.0.0-M2`

## 数据流架构

1. **行情数据流**：
   ```
   外部网关 → MarketGateway → IMarketCenter → 模组 → 策略
   ```

2. **订单流**：
   ```
   策略 → IModuleContext → 模组 → TradeGateway → 外部券商
   ```

3. **事件流**：
   ```
   FastEventEngine / Disruptor → 事件处理器 → 组件更新
   ```

4. **实时推送**：
   ```
   事件 → SseBroadcastHandler → SseStreamController → 前端 SSE 连接
   ```

## 策略开发指南

### 核心接口

```java
TradeStrategy          # 策略接口 - 所有策略必须实现
AbstractStrategy       # 抽象基类 - 推荐继承
IModuleContext         # 模组上下文 - 策略与框架交互
IAccount               # 合约绑定物理账户
IModuleAccount         # 模组级账户操作
```

策略使用 `@StrategicComponent("策略名称")` 注解标记，框架通过 Spring 自动发现。

### 指标框架

```java
public interface Indicator {
    Num get(int step);                 // 0=当前, -1=上一根
    double value(int step);
    boolean isReady();
    void update(Num num);              // 幂等更新
    List<Indicator> dependencies();
    Configuration getConfiguration();
}
```

`Num` 记录：

```java
public record Num(double value, long timestamp, boolean unstable)
```

### 交易操作

使用 `TradeIntent` 提交订单，可自动处理撤单追单：

```java
ctx.submitOrderReq(TradeIntent.builder()
    .contract(contract)
    .operation(SignalOperation.BUY_OPEN)
    .priceType(PriceType.OPP_PRICE)
    .volume(1)
    .timeout(5000)
    .build());
```

或使用 `TradeHelper` 简化交易：

```java
TradeHelper helper = TradeHelper.builder()
    .context(ctx)
    .tradeContract(contract)
    .build();
helper.doBuyOpen(1);
helper.doSellCloseAll();
```

### 实现步骤概要

1. 类标注 `@StrategicComponent(MyStrategy.NAME)`，继承 `AbstractStrategy`，实现 `TradeStrategy`
2. 定义 `InitParams extends DynamicParams`，用 `@Setting` 标注参数字段
3. 重写 `initWithParams(DynamicParams params)` 接收参数
4. 重写 `initIndicators()` 创建并注册指标
5. 实现 `onMergedBar()` 或 `onTick()` 交易逻辑
6. 交易前检查 `ctx.getState()` 和 `indicator.isReady()`

### 模组状态

`ModuleState` 主要取值：

- `EMPTY` - 空仓
- `HOLDING_LONG` - 持多仓
- `HOLDING_SHORT` - 持空仓
- `PLACING_ORDER` - 下单中
- `PENDING_ORDER` - 等待订单反馈

### 自定义指标

1. 继承 `AbstractIndicator`，实现 `evaluate(Num num)`
2. 如有依赖其他指标，重写 `dependencies()`
3. 使用 `Configuration.builder()` 配置参数
4. 返回 `Num.of(value, timestamp, unstable)`
5. 在策略中通过 `ctx.registerIndicator(indicator)` 注册

### 示例参考

- `BeginnerExampleStrategy` - 最简策略示例
- `IndicatorExampleStrategy` - 带指标的策略示例
- `MultiContractExampleStrategy` - 多合约示例
- `SimpleSpreadStrategy` - 套利策略示例

## 部署与启动

### 生产启动（`startup.sh`）

```bash
nohup java -Xlog:gc*:gc.log -Xms2g -Xmx2g \
  -Dloader.path=$(pwd) \
  -Denv=prod \
  -jar $(find /root/northstar-dist -name 'northstar-[0-9]*.*.jar') \
  >/root/northstar-dist/ns.log &
```

### 环境初始化

- `env.sh`（Linux）：创建 `~/northstar-env` 和 `~/northstar-dist`，安装 Oracle JDK 25
- `env.ps1`（Windows）：创建 `c:\northstar_env\` 和 `c:\northstar_dist\`，安装 JDK 25 MSI 并更新用户 PATH

### Docker / CI

- 仓库中**没有后端 Dockerfile 或 docker-compose.yml**
- 仅有前端 Dockerfile：`northstar-monitor-next/Dockerfile`（基于 Nginx 部署构建产物）
- 仓库中**没有 GitHub Actions、Gitee CI、Jenkinsfile 等 CI/CD 配置**

## Git 子模块

```
northstar-external       → git@gitee.com:kevinhuangwl/northstar-external.git
northstar-monitor-next   → git@gitee.com:kevinhuangwl/northstar-monitor-next.git
```

## 重要注意事项

1. **JDK 25 升级已完成**：源码 POM 已目标 Java 25 / Spring Boot 3.5.11。构建前请确认本地 JDK 与 POM 配置一致。
2. **Protobuf 生成类不可手动修改**：生成类位于 `xyz.redtorch.pb` 包中，更新请使用 `update-protobuf-obj.ps1`。
3. **H2 用于生产数据存储**：生产环境使用 H2 文件模式，历史行情数据主要依赖外部 marketplace 服务。
4. **虚拟线程**：默认在非 `dev` profile 下启用。
5. **`.flattened-pom.xml` 不一致**：工作目录中的 `.flattened-pom.xml` 文件未纳入 Git，且可能与源码 POM 不符，请勿以其为准。
6. **H2 Console 在生产环境启用**：路径 `/h2-console`，仅通过 CSRF 拦截器保护。
7. **前端构建顺序**：打包后端前必须先在前端目录执行 `pnpm build` 生成 `dist/`。
8. **测试环境默认账号**：`admin` / `123456`，可通过 `NS_USER` / `NS_PWD` 环境变量覆盖。
