# Northstar Pro - AGENTS.md

> 本文件面向 AI 编程代理，包含处理 Northstar Pro 项目所需的关键信息。信息基于对项目实际文件的核对整理；如发现与源码不一致，请以源码为准。

## 项目概述

**Northstar Pro（盈富量化交易平台）** 是一个基于 Java 和 Spring Boot 开发的量化交易平台，提供模块化的策略开发、回测、模拟交易和实盘部署框架。

- **当前源码版本**: `9.1.1`（定义在根 `pom.xml` 的 `<northstar-version>` 属性）
- **当前分支**: `feature/copilot`
- **语言**: Java 25（根 `pom.xml` 声明 `<java.version>25</java.version>`）
- **构建工具**: Maven 3.x
- **框架**: Spring Boot 4.0.7（`spring-boot-starter-parent`）
- **Group ID**: `org.dromara.northstar.pro`
- **Artifact ID**: `northstar`
- **仓库**: `https://gitee.com/dromara/northstar`
- **前端子模块**: `northstar-monitor-next`（Git 子模块，独立仓库 `git@gitee.com:kevinhuangwl/northstar-monitor-next.git`）

### 版本与分支说明

- 源码目标为 Java 25 / Spring Boot 4.0.7，源码版本为 `9.1.1`，构建前请确认本地 JDK 版本与 POM 一致。
- 各模块下的 `.flattened-pom.xml` 由 Flatten Maven Plugin 在构建时生成，未纳入 Git 跟踪，请勿以其为准。
- `northstar-monitor-next/package.json` 版本号为 `9.1.1`，与后端源码版本一致。
- `.gitmodules` 登记了两个子模块：`northstar-monitor-next` 和 `northstar-external`。其中 `northstar-monitor-next` 在父仓库中尚未初始化（`git submodule status` 显示 `-` 前缀），其目录内容系独立克隆；`northstar-external` 未注册进 git 索引，目录不存在。

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
├── northstar-monitor-next/          # 前端项目（Vue3 + Vite），以 Git 子模块引入，另有独立 AGENTS.md
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

## 技术栈

### 后端

- **Spring Boot 4.0.7** - 应用框架（Web / Data JPA / Scheduling / Cache）
- **Spring Retry 2.0.5** - 重试机制
- **H2 Database 2.2.224** - 嵌入式数据库（生产环境文件模式，测试内存模式）
- **Protocol Buffers 4.28.3** / protobuf-java-format 1.4 - 数据序列化
- **Disruptor 3.4.4** - 高性能事件处理
- **Netty 4.1.104.Final**
- **Lombok 1.18.46** - 减少样板代码（编译期注解处理）
- **Fastjson2 2.0.56** / **Jackson 2.16.1** - JSON 处理
- **Guava 33.0.0-jre** / **Apache Commons**（Lang3 3.17.0、IO 2.15.1、Math3 3.6.1、Codec 1.16.0）- 工具库
- **Hutool-crypto 5.7.22** - 加密
- **OkHttp3 4.12.0** / **Retrofit2 2.11.0** - HTTP 客户端
- **java-jwt 4.4.0** - 仅用于解码数据服务订阅令牌
- **Ehcache 3.10.8**（jakarta classifier）
- **可选 `smart` profile**: TensorFlow Java 0.5.0（`tensorflow-core-platform`）

### AI Copilot 模块

- **Spring AI 2.0.0**（`spring-ai-bom`，与 Spring Boot 4.x 配套）- 大模型接入框架
- 统一经 Spring AI OpenAI ChatModel 接入 OpenAI 兼容平台：Kimi、DeepSeek、MiniMax、GLM、OpenAI
- 代码组织：`org.dromara.northstar.copilot.llm` - `LlmProvider`（平台枚举，封装 baseUrl/默认模型/环境变量）与 `LlmChatClientFactory`（构建 `ChatClient`/`ChatModel`）；`org.dromara.northstar.copilot.tool` - `CopilotTool`（工具类标记注解）与 `ToolLoader`（基于 reflections 的运行时工具扫描动态加载）
- 各平台 API Key 经环境变量配置（`KIMI_API_KEY` / `DEEPSEEK_API_KEY` / `MINIMAX_API_KEY` / `GLM_API_KEY` / `OPENAI_API_KEY`），用法见 `northstar-copilot/README.md`
- 注意：该模块在根 `pom.xml` 的 `<modules>` 中暂被注释，不参与聚合构建，需用 `mvn -f northstar-copilot/pom.xml ...` 单独构建
- **OkHttp3 SSE** / **jtokkit** - Token 管理与流式对话

### 前端

- **Vue 3** + **Vite 5** + **TypeScript**
- **Vue Router** + **Pinia** + **Vue I18n**
- **Element Plus** + **TailwindCSS**
- **Klinecharts** + **ECharts** + **Monaco Editor**
- **Cypress** - E2E 测试
- **protobuf-ts** - TypeScript protobuf 生成
- 包管理器：**pnpm >= 9**（`package.json` 通过 `only-allow pnpm` 强制）
- Node 版本要求：`^18.18.0 || ^20.9.0 || >=21.1.0`

前端项目有独立的 `northstar-monitor-next/AGENTS.md`，前端开发请优先参考该文件。

## 关键配置文件

| 文件 | 说明 |
|---|---|
| `pom.xml` | 根 Maven POM，定义版本、模块、依赖管理 |
| `northstar-main/pom.xml` | 主应用 POM，定义可执行 jar 名称、前端静态资源拷贝、产物输出目录 |
| `northstar-main/src/main/resources/application.yml` | 主应用配置（Profile、SSL、数据库、日志、虚拟线程、数据服务） |
| `northstar-main/src/main/resources/logback-spring.xml` | 日志配置 |
| `northstar-monitor-next/package.json` | 前端依赖与脚本 |
| `northstar-monitor-next/vite.config.ts` | Vite 构建配置（端口由 `.env` 中 `VITE_PORT` 决定，默认 8848） |
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

构建完成后，`northstar-main` 的可执行 jar 会输出到 `northstar-dist/`，默认名称为 `northstar-9.1.1.jar`（由 `finalName` 决定）；`smart` profile 输出为 `northstar-9.1.1-smart.jar`。

### 前端

```bash
cd northstar-monitor-next
pnpm install
pnpm dev       # 开发服务（默认端口 8848）
pnpm build     # 生产构建，输出到 dist/
pnpm lint      # eslint + prettier + stylelint
pnpm typecheck # tsc + vue-tsc
pnpm e2e       # Cypress E2E 测试（chrome / firefox / edge）
```

前端 `dist/` 会在 `northstar-main` 的 Maven `generate-resources` 阶段被复制到 `src/main/resources/static/`（见 `northstar-main/pom.xml`）。因此完整构建时需要先执行 `pnpm build`。

## 代码组织

### `northstar-api` 核心目录

- `org.dromara.northstar.common.model.core/` - 核心数据模型（`Tick`、`Bar`、`Contract`、`Order`、`Trade`、`Position`、`Account`、`SubmitOrderReq` 等）
- `org.dromara.northstar.common.constant/` - 公共常量（`Constants.java`）
- `org.dromara.northstar.strategy/` - 策略框架（`TradeStrategy`、`AbstractStrategy`、`IModuleContext`、`IAccount`、`IModuleAccount`）
- `org.dromara.northstar.indicator/` - 指标框架（`Indicator`、`AbstractIndicator`）及指标实现；`Configuration`、`Num` 位于 `indicator/model/` 子包
- `org.dromara.northstar.gateway/` - 网关抽象层（`Gateway`、`MarketGateway`、`TradeGateway`、`IMarketCenter`）
- `org.dromara.northstar.data/` - 数据仓库接口（`IGatewayRepository`、`IModuleRepository`、`IMarketDataRepository`、`IAccountRepository`）

### `northstar-main` 主要目录

- `org.dromara.northstar.app.restful/` - REST 控制器
- `org.dromara.northstar.app.service/` - 服务层
- `org.dromara.northstar.app.auth/` - `AuthCheckerInterceptor`
- `org.dromara.northstar.app.security/` - 登录 IP 锁定、恶意 IP 收集与拦截
- `org.dromara.northstar.data.jdbc/` - JPA Repository、Adapter、Entity（`DO` 后缀）
- `org.dromara.northstar.event/` - 事件引擎/Disruptor 处理器
- `org.dromara.northstar.module/` - 模组运行时
- `org.dromara.northstar.account/` - 账户与网关管理器
- `org.dromara.northstar.config/` - Spring 配置类
- `org.dromara.northstar.support/` - 定时任务、消息发送、工具类

### 主应用入口

`northstar-main/src/main/java/org/dromara/northstar/NorthstarApplication.java`

```java
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "org.dromara.northstar.data.jdbc")
public class NorthstarApplication { ... }
```

### REST API 控制器

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

### 服务层

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

### 事件引擎

- `DisruptorFastEventEngine` 基于 LMAX Disruptor 实现。
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

### 数据访问层

采用 **Adapter 模式** 包装 Spring Data JPA Repository：

- `northstar-api/src/main/java/org/dromara/northstar/data/` 定义 Repository 接口。
- `northstar-main/src/main/java/org/dromara/northstar/data/jdbc/` 提供：
  - `entity/` - JPA 实体（以 `DO` 为后缀）
  - `*Repository.java` - Spring Data JPA Repository
  - `*Adapter.java` - Repository 适配器
- `RepositoryConfig` 通过 `@Bean` 将 Adapter 暴露为接口实现。

部分 JPA 实体采用 JSON 字符串字段（`dataStr`、`dataEncoded`）存储复杂对象，并提供 `convertFrom` / `convertTo` 静态方法。

### 数据流架构

1. **行情数据流**：外部网关 → `MarketGateway` → `IMarketCenter` → 模组 → 策略
2. **订单流**：策略 → `IModuleContext` → 模组 → `TradeGateway` → 外部券商
3. **事件流**：`DisruptorFastEventEngine` → 事件处理器 → 组件更新
4. **实时推送**：事件 → `SseBroadcastHandler` → `SseStreamController` → 前端 SSE 连接

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
- JPA：`ddl-auto: update`，`H2Dialect`
- H2 Console 在所有 profile 下均启用，路径 `/h2-console`

### 日志

- 使用 SLF4J + Logback
- 日志文件输出到 `logs/` 目录
- 主日志：`logs/Northstar_%d{yyyy-MM-dd}.log`，保留 30 天
- 调试日志：`logs/DEBUG/` 下分主题输出（BroadcastHandler、MarketCenter、KlineData），保留 3 天

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
  - 用户 ID：`admin`（`DEFAULT_USERID`）
  - 密码：`123456`（`DEFAULT_PASSWORD`）
- 可通过环境变量覆盖：
  - `NS_USER`
  - `NS_PWD`
- 登录失败 3 次后锁定账户；同时 `LoginIPLockService` 对 IP 进行限制：5 次失败（`MAX_FAILURES`）冻结 1 小时（`FREEZE_DURATION_MS`），冻结期间再次失败则封禁 1 天（`BAN_DURATION_MS`）。

### CSRF / Session 流程

拦截器在 `AppConfig` 中注册（仅非 `unittest` profile 生效），拦截 `/northstar/**`，排除 `/northstar/user/**` 和 `/northstar/reset`；`AuthCheckerInterceptor` 内部还直接放行 `/northstar/sse/stream`。

1. `POST /northstar/user/login` 校验凭据
2. 成功后，随机 UUID 作为 CSRF Token 存入 HTTP Session（键 `SESS_CSRF`）
3. 后续 `/northstar/**` 请求须携带请求头 `CSRF_token` 并匹配 Session 值
4. `InternalLoopCallUtils` 生成一次性 UUID 用于内部循环调用（如 Copilot 函数调用），3 分钟后过期
5. `GET /northstar/user/validate` 检查 Session 有效性

### 其他安全措施

- `MaliciousIPCollector` / `MaliciousRequestFilter`：统计 404 请求 IP，单日超过 10 次则拦截
- `CommonControllerAdvice`：全局异常处理，将 404 请求 IP 记录到 `MaliciousIPCollector`
- Session Cookie 配置：`SameSite=strict`、`secure=true`
- CORS：在 `AppConfig` 中以 `CorsFilter` Bean 配置，`allowedOriginPattern("*")` 允许任意来源、`allowCredentials(true)`，暴露 `token` 响应头

### JWT

`java-jwt` 仅用于**解码数据服务订阅令牌**（`UserController.payedServices()`），不用于用户认证。

## 测试策略

### 单元测试

- 位置：各模块的 `src/test/java`
- 框架：JUnit 5（Jupiter）、Mockito、AssertJ、Spring Boot Test
- 命名：`*Test.java`
- 插件：Maven Surefire 3.5.6
- 当前项目共有 **89** 个 `*Test.java` 文件，分布如下：
  - `northstar-api`: 40
  - `northstar-main`: 33
  - `northstar-gateway-sim`: 6
  - `northstar-gateway-playback`: 4
  - `northstar-strategy-example`: 0
  - `northstar-copilot`: 6

### 集成测试

- 命名：`*IT.java`
- 插件：Maven Failsafe 3.5.6
- 当前工作树中**未发现** `*IT.java` 文件，但 Failsafe 已配置支持

### 测试覆盖率

- 工具：JaCoCo 0.8.13
- 最低行覆盖率：**30%**（`mvn clean verify` 时校验）
- 排除项：
  - `northstar-api`: protobuf 生成代码、指标、策略、`gateway/contract/**`
  - `northstar-gateway-sim`: `org/dromara/northstar/gateway/sim/market/*`

### 测试 Profile

多数 `northstar-main` 的 Spring 测试使用：

```java
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
```

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

- `env.sh`（Linux）：创建 `~/northstar-env` 和 `~/northstar-dist`，安装 JDK 25
- `env.ps1`（Windows）：创建 `c:\northstar_env\` 和 `c:\northstar_dist\`，安装 JDK 25 MSI 并更新用户 PATH

### Docker / CI

- 仓库中**没有后端 Dockerfile 或 docker-compose.yml**
- 仅有前端 Dockerfile：`northstar-monitor-next/Dockerfile`（基于 Nginx 部署构建产物）
- 仓库中**没有 GitHub Actions、Gitee CI、Jenkinsfile 等 CI/CD 配置**

## 代码风格与开发规范

### 命名规范

- 接口：`I` 前缀（如 `IAccount`、`IModuleContext`、`IGatewayRepository`）或无前缀（如 `TradeStrategy`、`Indicator`）
- 抽象类：`Abstract` 前缀（如 `AbstractStrategy`、`AbstractIndicator`、`AbstractEventHandler`）
- 实现类：描述性名称（如 `GatewayManager`、`MarketCenter`）
- 测试类：`*Test.java`（单元测试），`*IT.java`（集成测试）
- JPA 实体：以 `DO` 为后缀（如 `GatewayDescriptionDO`、`BarDO`）
- API 模型：视情况使用 `VO` / `DTO` 后缀

### 代码风格

- 使用 Lombok 减少样板代码（`@Data`、`@Slf4j`、`@Builder`、`@AllArgsConstructor` 等）
- 控制器与服务类中**字段注入（`@Autowired`）和构造器注入并存**；控制器以字段注入为主
- 不可变字段使用 `final`
- 代码注释主要使用中文
- 编码：UTF-8（`maven.compiler.encoding` / `file.encoding` 均已配置）

### 版本管理

- 使用 Maven Flatten Plugin 1.7.3 管理版本
- 版本定义在父 POM：`<northstar-version>9.1.1</northstar-version>`
- 通过 `<revision>${northstar-version}</revision>` 占位符供各模块继承
- 构建时生成 `.flattened-pom.xml`

## 重要注意事项

1. **JDK 25 要求**：源码 POM 目标为 Java 25 / Spring Boot 4.0.7。构建前请确认本地 JDK 与 POM 配置一致。
2. **Protobuf 生成类不可手动修改**：生成类位于 `xyz.redtorch.pb` 包中，更新请使用 `update-protobuf-obj.ps1`（前端侧使用 `pnpm update-proto`）。
3. **H2 用于生产数据存储**：生产环境使用 H2 文件模式，历史行情数据主要依赖外部 marketplace 服务。
4. **虚拟线程**：默认在非 `dev` profile 下启用。
5. **`.flattened-pom.xml` 不可信**：工作目录中的 `.flattened-pom.xml` 文件未纳入 Git，且可能与源码 POM 不符，请勿以其为准。
6. **H2 Console 在生产环境启用**：路径 `/h2-console`，仅通过 CSRF 拦截器保护。
7. **前端构建顺序**：打包后端前必须先在前端目录执行 `pnpm build` 生成 `dist/`。
8. **测试环境默认账号**：`admin` / `123456`，可通过 `NS_USER` / `NS_PWD` 环境变量覆盖。
9. **版本对齐**：后端源码版本与 `northstar-monitor-next/package.json` 均为 `9.1.1`，已对齐。
10. **子模块状态**：`northstar-monitor-next` 在父仓库中未初始化（目录为独立克隆）；`northstar-external` 在 `.gitmodules` 中登记但未注册进索引、目录不存在。
