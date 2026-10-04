# Northstar Pro - AGENTS.md

> 本文件面向 AI 编程代理，包含处理 Northstar Pro 项目所需的关键信息。信息基于对项目实际文件的核对整理；如发现与源码不一致，请以源码为准。

## 项目概述

**Northstar Pro（盈富量化交易平台）** 是一个基于 Java 和 Spring Boot 开发的量化交易平台，提供模块化的策略开发、回测、模拟交易和实盘部署框架。

- **当前源码版本**: `9.1.6`（定义在根 `pom.xml` 的 `<northstar-version>` 属性）
- **当前分支**: `master`
- **语言**: Java 25（根 `pom.xml` 声明 `<java.version>25</java.version>`）
- **构建工具**: Maven 3.x
- **框架**: Spring Boot 4.1.1（`spring-boot-starter-parent`，基于 Spring Framework 7）
- **Group ID**: `org.dromara.northstar.pro`
- **Artifact ID**: `northstar`
- **仓库**: `https://gitee.com/dromara/northstar`
- **前端子模块**: `northstar-monitor-next`（Git 子模块，独立仓库 `git@gitee.com:kevinhuangwl/northstar-monitor-next.git`）

### 版本与分支说明

- 源码目标为 Java 25 / Spring Boot 4.1.1，源码版本为 `9.1.6`，构建前请确认本地 JDK 版本与 POM 一致。
- 各模块下的 `.flattened-pom.xml` 由 Flatten Maven Plugin 在构建时生成，未纳入 Git 跟踪，请勿以其为准。
- `northstar-monitor-next/package.json` 版本号为 `9.1.6`，与后端源码版本一致。
- `.gitmodules` 登记了两个子模块：`northstar-monitor-next` 和 `northstar-external`。其中 `northstar-monitor-next` 在父仓库中尚未初始化（`git submodule status` 显示 `-` 前缀），其目录内容系独立克隆；`northstar-external` 未注册进 git 索引，目录不存在。

## 项目结构

```
northstar/
├── pom.xml                          # 父 POM，定义版本、模块、依赖管理
├── northstar-api/                   # API 模块 - 核心接口、数据模型、指标与策略框架
├── northstar-gateway-sim/           # 模拟网关模块（模拟行情/交易）
├── northstar-gateway-playback/      # 回放网关模块（回测）
├── northstar-strategy-example/      # 策略示例模块
├── northstar-rl/                    # 强化学习模块（RL 策略 SPI 实现、模型版本管理、DJL 推理、Python 离线训练），另有 DESIGN.md 与 README.md
├── northstar-main/                  # 主 Spring Boot 应用模块
├── northstar-monitor-next/          # 前端项目（Vue3 + Vite），以 Git 子模块引入，另有独立 AGENTS.md
├── northstar-doc/                   # 用户使用文档（VitePress 静态站点，pnpm dev/build/preview）
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
| `northstar-rl` | `org.dromara.northstar.rl` |

## 技术栈

### 后端

- **Spring Boot 4.1.1** - 应用框架（Web / Data JPA / Scheduling / Cache），基于 Spring Framework 7
- **Spring Retry 2.0.13** - 重试机制（Spring Boot 4 起不再托管该依赖，版本在根 POM 显式钉版）
- **H2 Database 2.4.240**（随 Boot 托管）- 嵌入式数据库（生产环境文件模式，测试内存模式）
- **Protocol Buffers 4.28.3** / protobuf-java-format 1.4 - 数据序列化
- **Disruptor 3.4.4** - 高性能事件处理
- **Netty 4.2.17.Final**（随 Boot 托管）
- **Lombok 1.18.46** - 减少样板代码（编译期注解处理）
- **Fastjson2 2.0.56** - JSON 处理主力；**Jackson 双版本并存**：Boot 4 内部使用 Jackson 3（`tools.jackson`），项目代码/retrofit 使用 Jackson 2（`com.fasterxml.jackson:jackson-bom` 2.22.3 导入式管理）
- **Guava 33.0.0-jre** / **Apache Commons**（Lang3 3.17.0、IO 2.15.1、Math3 3.6.1、Codec 1.16.0）- 工具库
- **Hutool-crypto 5.7.22** - 加密
- **OkHttp3 4.12.0** / **Retrofit2 2.11.0** - HTTP 客户端
- **java-jwt 4.4.0** - 仅用于解码数据服务订阅令牌
- **RestTemplateBuilder**：Boot 4 移至 `spring-boot-restclient` 模块（`org.springframework.boot.restclient` 包），`spring-boot-starter-web` 不再传递引入
- **可选 `smart` profile**: TensorFlow Java 0.5.0（`tensorflow-core-platform`）
- **DJL 0.38.0**（`northstar-rl` 模块）：DJL API + PyTorch 引擎 + `pytorch-native-cpu` 2.7.1（classifier 按平台选择，默认 `win-x86_64`），版本由根 POM 的 DJL BOM 管理

### 前端

- **Vue 3** + **Vite 5** + **TypeScript**
- **Vue Router** + **Pinia** + **Vue I18n**
- **Element Plus** + **TailwindCSS**
- **Klinecharts** + **ECharts** + **Monaco Editor**
- **Cypress** - E2E 测试
- **protobuf-ts** - TypeScript protobuf 生成
- 包管理器：**pnpm >= 9**（`package.json` 通过 `only-allow pnpm` 强制）

前端项目有独立的 `northstar-monitor-next/AGENTS.md`，前端开发请优先参考该文件。

## 关键配置文件

| 文件 | 说明 |
|---|---|
| `pom.xml` | 根 Maven POM，定义版本、模块、依赖管理 |
| `northstar-main/pom.xml` | 主应用 POM，定义可执行 jar 名称、前端静态资源拷贝、产物输出目录 |
| `northstar-main/src/main/resources/application.yml` | 主应用配置（Profile、SSL、数据库、日志、虚拟线程、数据服务） |
| `northstar-main/src/main/resources/logback-spring.xml` | 日志配置 |
| `northstar-monitor-next/package.json` | 前端依赖与脚本 |
| `northstar-monitor-next/vite.config.ts` | Vite 构建配置 |
| `lombok.config` | Lombok 配置（`lombok.addLombokGeneratedAnnotation=true`） |
| `.gitmodules` | Git 子模块配置 |
| `env.sh` / `env.ps1` | Linux / Windows 环境初始化脚本（安装 JDK 25；并准备 RL 训练 Python 虚拟环境 rl-venv + 依赖，写入 NS_RL_PYTHON） |
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

构建完成后，`northstar-main` 的可执行 jar 会输出到 `northstar-dist/`，默认名称为 `northstar-9.1.6.jar`（由 `finalName` 决定）；`smart` profile 输出为 `northstar-9.1.6-smart.jar`。

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

前端 `dist/` 会在 `northstar-main` 的 Maven `generate-resources` 阶段被复制到 `src/main/resources/static/`（见 `northstar-main/pom.xml`）。因此完整构建时需要先执行 `pnpm build`。

## 代码组织

### `northstar-api` 核心目录

- `org.dromara.northstar.common.model.core/` - 核心数据模型（`Tick`、`Bar`、`Contract`、`Order`、`Trade`、`Position`、`Account`、`SubmitOrderReq` 等）
- `org.dromara.northstar.common.model.rl/` - RL 训练 Web 契约（`RLTrainingTaskDTO/VO`、`RLRoundMetrics`、`RLModelVO`、`RLStrategyVO` 等）
- `org.dromara.northstar.strategy.rl/` - RL 策略 SPI（`RLStrategy`、`RLModelSpec`、`RLFeatureSpec`、`RLRewardContext`、`TradeAction`）
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
| `UserController` | 用户认证和管理 |
| `SseStreamController` | 服务器推送实时数据 |
| `MultichartsController` | 多图表数据接口 |
| `LoggingController` | 应用日志访问 |
| `MetaDataController` | 元数据管理 |
| `NotificationController` | 通知设置 |
| `ContractConfigController` | 合约配置 |
| `RLController` | RL 强化学习训练（策略/模型目录、训练任务 CRUD 与三段式生命周期控制：创建/加载数据/启动训练/暂停/继续/停止/删除，路径 `/northstar/rl`） |
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
- `ContractConfigService` - 合约配置
- `CustomIndicatorService` - 自定义指标
- `LoggingService` - 日志服务
- `MetaDataService` - 元数据
- `MultichartsService` - 多图表
- `NotificationService` - 通知
- `RLTrainingService`（`org.dromara.northstar.app.rl`）- RL 训练任务三段式生命周期（创建 CREATED → 数据加载 LOADING/READY → 启动训练 RUNNING）、模型目录、SSE 推送（事件名 `RL_TRAINING`）；同包还有 `RLTrainingDataManager`（数据加载阶段从数据源拉取行情并落地 CSV，训练阶段只读 CSV，CSV 按任务ID分目录存于 `northstar.rl.data-root`）、`TradingEnvironment`（单合约交易环境）、`RLTrainingRunner`（按轮驱动）、`RLModuleSandbox`（模组沙箱）

### RL 训练配置（application.yml `northstar.rl.*`）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `models-root` | `models` | 模型根目录（缺省按 `models`、`northstar-rl/models`、`../northstar-rl/models` 候选解析） |
| `python` | `python` | Python 解释器路径（显式配置优先；缺省为 `python` 时自动探测环境脚本安装的 rl-venv（`~/northstar-env/rl-venv` / `c:\northstar_env\rl-venv`），其次系统 python） |
| `training-dir` | 自动解析 | `train_round.py` 所在目录 |
| `train-executor` | `python` | `python` 子进程 / `stub`（无 PyTorch 的测试环境） |
| `data-root` | `data/rl-training` | 训练数据根目录（数据加载阶段落地的 CSV，按任务ID分目录，任务数据仅加载一次，重启后仍有效） |

存放归属原则：`data-root` 存任务级临时生产资料（行情 CSV，随任务删除）；`models-root` 存版本级长期资产（checkpoint.pt、model.pt、rounds/ 经验分片、buffer.npz、ingested.json、版本 metadata，随版本保留，供跨任务 fork 继承、模组绑定与血缘复盘）。详见 `northstar-rl/DESIGN.md` §5.2「数据归属原则」。

沙箱每局初始资金、决策点超时强触发 bar 数、权益击穿阈值、单局最长 bar 数四项已移至训练任务创建窗口（任务级参数，随任务持久化），不再由配置文件管理。

均可用环境变量覆盖（`NS_RL_MODELS_ROOT`、`NS_RL_PYTHON`、`NS_RL_TRAINING_DIR`、`NS_RL_TRAIN_EXECUTOR`、`NS_RL_DATA_ROOT` 等）。

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
- 调试日志：`logs/DEBUG/` 下分主题输出，保留 3 天

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
- 登录失败 3 次后锁定账户；同时 `LoginIPLockService` 对 IP 进行限制：5 次失败冻结 1 小时，冻结期间再次失败则封禁 1 天。

### CSRF / Session 流程

拦截器在 `AppConfig` 中注册（仅非 `unittest` profile 生效），拦截 `/northstar/**`，排除 `/northstar/user/**` 和 `/northstar/reset`；`AuthCheckerInterceptor` 内部还直接放行 `/northstar/sse/stream`。

1. `POST /northstar/user/login` 校验凭据
2. 成功后，随机 UUID 作为 CSRF Token 存入 HTTP Session（键 `SESS_CSRF`）
3. 后续 `/northstar/**` 请求须携带请求头 `CSRF_token` 并匹配 Session 值
4. `InternalLoopCallUtils` 生成一次性 UUID 用于内部循环调用，3 分钟后过期
5. `GET /northstar/user/validate` 检查 Session 有效性

### 其他安全措施

- `MaliciousIPCollector` / `MaliciousRequestFilter`：统计 404 请求 IP，单日超过 10 次则拦截
- `CommonControllerAdvice`：全局异常处理，将 404 请求 IP 记录到 `MaliciousIPCollector`
- Session Cookie 配置：`SameSite=strict`、`secure=true`
- CORS：在 `AppConfig` 中配置，允许任意来源，暴露 `token` 响应头

### JWT

`java-jwt` 仅用于**解码数据服务订阅令牌**（`UserController.payedServices()`），不用于用户认证。

## 测试策略

### 单元测试

- 位置：各模块的 `src/test/java`
- 框架：JUnit 5（Jupiter）、Mockito、AssertJ、Spring Boot Test
- 命名：`*Test.java`
- 插件：Maven Surefire 3.5.6
- **Spring Boot 4.1 测试注解已迁移**（经典包名已移除）：
  - `@MockitoBean` → `org.springframework.test.context.bean.override.mockito.MockitoBean`（spring-test）
  - `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`（依赖 `spring-boot-data-jpa-test`，northstar-main 已声明）
  - `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`（依赖 `spring-boot-webmvc-test`，northstar-main 已声明）
- 当前项目共有 **104** 个 `*Test.java` 文件，分布如下：
  - `northstar-api`: 41（含 `RLModelSpecTest`：RL 模型规格 specHash 稳定性与结构化 diff）
  - `northstar-main`: 42（含 `TradePlanE2ETest`：画线交易计划的 Controller 层 e2e 测试，通过向事件引擎投递指定价格的 Tick 模拟行情，替代原 Cypress 版本中依赖随机行情的用例；以及 `PlannedTradeHandlerTest` / `LineTraderTest`：画线交易计划的纯 mock 单元测试；`RLTrainingServiceTest` / `RLTrainingRunnerTest` / `TradingEnvironmentTest` / `RLTrainingDataManagerTest`：RL 训练服务与生命周期、按轮驱动（串行信号量/轮边界调参/达标判定/配额分配）、交易环境（三类强制触发点）与训练数据 CSV 存取；`RLControllerTest`：RL REST 端点）
  - `northstar-gateway-sim`: 6
  - `northstar-gateway-playback`: 5
  - `northstar-strategy-example`: 0
  - `northstar-rl`: 10（含 `DqnInferenceExampleTest`：真实加载 `models/cartpole/versions/v1/model.pt` 的推理示例，默认跳过，需 `-Drl.inference.test=true` 显式开启；`RlTrendStrategyGoldenTest`：rlSpec 的 specHash golden 测试，防策略无意识演进；`RlTrendStrategyTest`：observe() golden 快照、reward 重罚分支与模组加载 specHash 失败路径；`RLModelStoreTest`：模型/版本目录、fork 校验与回收；`PythonTrainExecutorTest`：训练 stdout 协议解析）
- Python 侧另有 `northstar-rl/training/tests/`（pytest，覆盖 DQN 训练器、增量经验读取、版本目录契约、预处理存量转换与原子写不残留）

### 集成测试

- 命名：`*IT.java`
- 插件：Maven Failsafe 3.5.6（绑定 integration-test + verify）
- 现有 6 个 RESTful API e2e 测试位于 `northstar-main/src/test/java/org/dromara/northstar/app/restful/it/`（含 `RLTrainingFlowIT`：RL 训练全链路，数据源与 Python 执行器以替身注入）
- 运行：`mvn failsafe:integration-test` 或随 `mvn clean verify` 执行

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

- `env.sh`（Linux）：创建 `~/northstar-env` 和 `~/northstar-dist`，安装 JDK 25；并在 `~/northstar-env/rl-venv` 创建 RL 训练虚拟环境、安装依赖（`REQUIREMENTS` 环境变量可指定 requirements.txt，找不到时安装内置默认依赖）、写入 `NS_RL_PYTHON`
- `env.ps1`（Windows）：创建 `c:\northstar_env\` 和 `c:\northstar_dist\`，安装 JDK 25 MSI 并更新用户 PATH；并在 `c:\northstar_env\rl-venv` 创建 RL 训练虚拟环境、写入用户级环境变量 `NS_RL_PYTHON`

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
- 版本定义在父 POM：`<northstar-version>9.1.6</northstar-version>`
- 通过 `<revision>${northstar-version}</revision>` 占位符供各模块继承
- 构建时生成 `.flattened-pom.xml`

## 重要注意事项

1. **JDK 25 要求**：源码 POM 目标为 Java 25 / Spring Boot 4.1.1。构建前请确认本地 JDK 与 POM 配置一致。
2. **Protobuf 生成类不可手动修改**：生成类位于 `xyz.redtorch.pb` 包中，更新请使用 `update-protobuf-obj.ps1`。
3. **H2 用于生产数据存储**：生产环境使用 H2 文件模式，历史行情数据主要依赖外部 marketplace 服务。
4. **虚拟线程**：默认在非 `dev` profile 下启用。
5. **`.flattened-pom.xml` 不可信**：工作目录中的 `.flattened-pom.xml` 文件未纳入 Git，且可能与源码 POM 不符，请勿以其为准。
6. **H2 Console 在生产环境启用**：路径 `/h2-console`，仅通过 CSRF 拦截器保护。
7. **前端构建顺序**：打包后端前必须先在前端目录执行 `pnpm build` 生成 `dist/`。
8. **测试环境默认账号**：`admin` / `123456`，可通过 `NS_USER` / `NS_PWD` 环境变量覆盖。
9. **版本对齐**：后端源码版本与 `northstar-monitor-next/package.json` 均为 `9.1.6`，已对齐。
10. **子模块状态**：`northstar-monitor-next` 在父仓库中未初始化（目录为独立克隆）；`northstar-external` 在 `.gitmodules` 中登记但未注册进索引、目录不存在。
