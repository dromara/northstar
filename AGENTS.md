# Northstar Pro - AGENTS.md

> 本文件包含 AI 编程代理处理 Northstar Pro 项目所需的关键信息。

## 项目概述

**Northstar Pro（盈富量化交易平台）** 是一个基于 Java 21 和 Spring Boot 3.3.5 开发的量化交易平台，提供模块化的策略开发、回测和交易部署框架。

- **版本**: 8.9.0-BETA1
- **语言**: Java 21
- **构建工具**: Maven
- **框架**: Spring Boot 3.3.5
- **许可证**: Dromara 开源组织
- **仓库**: https://gitee.com/dromara/northstar

## 技术栈

### 核心框架
- **Spring Boot 3.3.5** - 应用框架
- **Spring Data JPA** - 数据持久化
- **Spring Web** - REST API 层
- **Spring Scheduling** - 任务调度
- **Spring Retry** - 重试机制
- **Spring Boot Cache** - 缓存支持

### 数据与消息
- **H2 Database 2.2.224** - 嵌入式数据库（生产环境使用文件模式，测试使用内存模式）
- **Protocol Buffers 4.28.3** - 数据序列化
- **Disruptor 3.4.4** - 高性能事件处理
- **Netty-SocketIO 2.0.11** - 实时 WebSocket 通信

### 工具库
- **Lombok 1.18.42** - 减少样板代码
- **Fastjson2 2.0.56** - JSON 处理
- **Guava 33.0.0** - Google 核心库
- **Apache Commons** (Lang3, Math3, IO, Codec) - 工具函数
- **Hutool 5.7.22** - 国产工具库（加密）
- **Jackson 2.16.1** - JSON/XML 处理

### AI/ML（可选）
- **TensorFlow Java 0.5.0** - 机器学习集成
- **ND4J 1.0.0-M2.1** - JVM 科学计算

### AI Copilot 模块
- **OkHttp3 4.12.0** - HTTP 客户端
- **jtokkit 1.1.0** - Token 管理
- 支持多种 LLM 平台：OpenAI、Moonshot、DeepSeek、Ollama、Hunyuan、Zhipu 等

## 项目结构

```
northstar-pro/
├── pom.xml                          # 父 POM，定义版本和依赖管理
├── northstar-api/                   # API 模块 - 核心接口和数据模型
│   ├── 策略框架接口 (TradeStrategy, IModuleContext)
│   ├── 指标框架 (Indicator, AbstractIndicator)
│   ├── 网关接口 (Gateway, MarketGateway, TradeGateway)
│   └── 核心数据模型 (Tick, Bar, Contract, Order, Trade, Position)
├── northstar-main/                  # 主应用模块
│   ├── REST 控制器
│   ├── 服务层
│   ├── 数据访问层 (JPA)
│   └── 启动类 (NorthstarApplication)
├── northstar-gateway-sim/           # 模拟网关模块
│   ├── 模拟行情网关
│   └── 模拟交易网关
├── northstar-gateway-playback/      # 回放网关模块（回测）
│   └── 历史数据回放功能
├── northstar-strategy-example/      # 策略示例模块
│   └── 各种示例策略实现
├── northstar-copilot/               # AI 助手模块
│   ├── 多平台 LLM 集成
│   ├── 向量存储 (Pinecone)
│   └── 工具调用框架
└── northstar-monitor-next/          # 前端项目（Vue3 + Vite）
```

## 构建命令

### 基本构建
```bash
# 编译打包
mvn clean package

# 跳过测试加速构建
mvn clean package -DskipTests

# 运行特定 Profile（smart = 包含 TensorFlow）
mvn clean package -P smart
```

### 测试
```bash
# 运行单元测试
mvn test

# 运行集成测试（*IT.java 结尾的测试）
mvn failsafe:integration-test

# 生成代码覆盖率报告（JaCoCo）
mvn jacoco:report

# 覆盖率要求：最低 30% 行覆盖率
```

### 开发 Profile
- `prod` - 生产环境（端口 443，SSL 启用）
- `demo` - 演示模式（端口 443，SSL 启用）
- `dev` - 开发环境（端口 80，SSL 禁用）
- `test` - 测试环境（内存 H2）
- `unittest` - 单元测试
- `train` - 机器学习训练模式
- `e2e` - 端到端测试

## 运行时配置

### 应用配置
主配置文件：`northstar-main/src/main/resources/application.yml`

关键配置段：
- **服务器**: SSL 配置（JKS 密钥库），压缩，会话管理
- **数据库**: H2 文件模式（生产），内存模式（测试）
- **数据服务**: Marketplace URL 行情数据源
- **日志**: 文件日志输出到 `logs/` 目录
- **虚拟线程**: Spring Boot 3.2+ 启用虚拟线程

### 环境变量
- `env` - 激活的 Spring Profile（默认 `prod`）
- `server.port` - 覆盖服务端口
- `loader.path` - 外部库路径

### 启动脚本
生产启动示例（`startup.sh`）：
```bash
java -Xms2500m -Xmx2500m \
     -Dserver.port=443 \
     -Dloader.path=$(pwd) \
     -Denv=demo \
     -jar northstar-8.9.0-BETA1.jar
```

## 代码组织

### 策略框架
交易策略实现 `TradeStrategy` 接口或继承 `AbstractStrategy`：

```java
// 关键接口
TradeStrategy          # 策略接口 - 所有策略必须实现
AbstractStrategy       # 抽象基类 - 推荐继承
IModuleContext         # 模组上下文 - 策略与框架交互
IAccount               # 合约绑定物理账户
IModuleAccount         # 模组级账户操作
```

策略注解：`@StrategicComponent` - 标记可发现策略

### 指标框架
技术指标实现 `Indicator` 接口：

```java
Indicator              # 所有指标的基础接口
Configuration          # 指标配置
Num                    # 带元数据的数值包装
```

指标分类：
- `momentum/` - 动量指标（RSI、KD 等）
- `trend/` - 趋势指标（MA、EMA、PBX 等）
- `volatility/` - 波动率指标（BOLL、TrueRange 等）
- `volume/` - 成交量指标（VWAP、VolumeBoosting 等）
- `wave/` - 波浪分析指标

### 网关框架
网关抽象层用于行情和交易：

```java
Gateway                # 基础网关接口
MarketGateway          # 行情网关（Tick/K线）
TradeGateway           # 交易网关（订单/成交）
IMarketCenter          # 合约和行情数据管理
```

### 核心数据模型
位于 `northstar-api/src/main/java/org/dromara/northstar/common/model/core/`：
- `Tick` - 实时行情 Tick 数据
- `Bar` - OHLCV K线数据
- `Contract` - 交易工具定义
- `Order` - 订单对象
- `Trade` - 成交记录
- `Position` - 当前持仓
- `Account` - 账户信息
- `SubmitOrderReq` - 订单提交请求

### 数据访问层
使用 Spring Data JPA，位于 `northstar-main/src/main/java/org/dromara/northstar/data/jdbc/`：
- Repository 接口定义查询
- Adapter 类处理业务逻辑适配
- Entity 类对应数据库表

## 测试策略

### 单元测试
- 位置：各模块的 `src/test/java`
- 框架：JUnit 5 (Jupiter)
- 模拟：Mockito
- 命名：`*Test.java`

### 集成测试
- 位置：`northstar-main/src/test/java`
- 命名：`*IT.java` (Integration Test)
- 插件：Maven Failsafe Plugin

### 测试覆盖率
- 工具：JaCoCo
- 最低行覆盖率：30%
- `northstar-api` 排除项：protobuf 生成代码、指标、策略、合约代码
- `northstar-gateway-sim` 排除项：市场模拟代码

### 主要测试类
- `AccountManagerTest` - 账户管理测试
- `GatewayManagerTest` - 网关生命周期测试
- `TradeAccountTest` - 交易账户测试
- 各种 Controller 测试在 `northstar-main` 中

## REST API 控制器

位于 `northstar-main/src/main/java/org/dromara/northstar/app/restful/`：

| 控制器 | 功能 |
|--------|------|
| `GatewayController` | 网关 CRUD 和连接管理 |
| `ModuleController` | 交易模组管理 |
| `TradeController` | 订单提交和管理 |
| `MarketDataController` | 行情数据查询 |
| `PlaybackController` | 回测回放控制 |
| `StrategyExplorerController` | 策略开发工具 |
| `CopilotChatController` | AI 助手对话接口 |
| `UserController` | 用户认证和管理 |
| `SseStreamController` | 服务器推送实时数据 |
| `MultichartsController` | 多图表数据接口 |
| `LoggingController` | 应用日志访问 |
| `MetaDataController` | 元数据管理 |
| `NotificationController` | 通知设置 |
| `ResetController` | 系统重置操作 |

API 基础路径：`/northstar/...`

## 安全考虑

### 认证
- 基于 JWT 的认证（`java-jwt` 库）
- 使用 Hutool crypto 进行密码加密
- Session Cookie 设置 `SameSite=strict` 和 `secure` 标志

### SSL/TLS
- 生产环境默认启用 SSL（端口 443）
- JKS 密钥库：`keystore/northstar.quantit.tech.jks`
- 支持协议：TLSv1.2、TLSv1.3

### 输入验证
- `AuthCheckerInterceptor` - 请求验证
- `MaliciousIPCollector` - IP 安全监控

## 开发规范

### 代码风格
- 使用 Lombok 减少样板代码（`@Data`、`@Slf4j` 等）
- 优先使用构造器注入而非字段注入
- 不可变字段使用 `final`
- 代码注释主要使用中文

### 命名规范
- 接口：`I` 前缀（如 `IGatewayService`）或无前缀
- 抽象类：`Abstract` 前缀
- 实现类：描述性名称（如 `GatewayManager`）
- 测试类：`*Test.java`（单元测试），`*IT.java`（集成测试）

### 异常处理
- `CommonControllerAdvice` - 全局异常处理器
- 可恢复错误使用受检异常
- 编程错误使用非受检异常

### 日志
- 使用 SLF4J + Logback
- 日志级别：ERROR、WARN、INFO、DEBUG
- 日志文件：`logs/` 目录，支持轮转

## 版本管理

- 使用 Maven Flatten Plugin 管理版本
- 版本定义在父 POM：`northstar-version` 属性（8.9.0-BETA1）
- `${revision}` 占位符用于 CI/CD 集成
- 构建时生成 `.flattened-pom.xml`

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
   FastEventEngine → 事件处理器 → 组件更新
   ```

## 外部集成

### 数据服务
- 基础 URL：`https://marketplace.quantit.tech`
- 提供历史行情数据
- 合约定义

### AI Copilot
- 通过 HTTP/SSE 集成 LLM
- 使用 `jtokkit` 进行 Token 管理
- OkHttp3 客户端进行 API 调用

## 策略开发指南

### 核心架构

#### 1. 策略框架关键接口

```java
TradeStrategy              # 策略接口
├── onTick()              # TICK 数据回调
├── onMergedBar()         # K线数据回调
├── onOrder()             # 订单状态回调
├── onTrade()             # 成交回调
└── name()                # 策略名称

AbstractStrategy           # 策略抽象基类
├── ctx                   # IModuleContext 模组上下文
├── logger                # 日志对象
├── initIndicators()      # 指标初始化钩子
└── storeObject           # 状态存储对象

IModuleContext             # 模组上下文
├── submitOrderReq()      # 提交订单
├── getState()            # 获取模组状态
├── registerIndicator()   # 注册指标
└── getModuleAccount()    # 获取模组账户
```

#### 2. 指标框架

```java
public interface Indicator {
    Num get(int step);           // 回溯获取指标值（0=当前, -1=上一根）
    double value(int step);      // 获取数值
    boolean isReady();           // 指标是否初始化完成
    void update(Num num);        // 数据更新（幂等设计）
    List<Indicator> dependencies(); // 依赖指标
    Configuration getConfiguration(); // 配置信息
}
```

**Configuration** 配置示例：
```java
Configuration.builder()
    .contract(contract)          // 绑定合约
    .indicatorName("EMA10")      // 指标名称
    .numOfUnits(5)               // 周期数
    .period(PeriodUnit.MINUTE)   // 周期单位
    .valueType(ValueType.CLOSE)  // 值类型
    .cacheLength(16)             // 缓存长度
    .visible(true)               // 是否可见
    .build();
```

**Num Record** - 指标值包装：
```java
public record Num(double value, long timestamp, boolean unstable)
```
- `unstable=true` 表示当前 K 线未完成，值可能变化

#### 3. 模组状态机

```java
ModuleState state = ctx.getState();
// EMPTY          - 空仓
// HOLDING_LONG   - 持多仓
// HOLDING_SHORT  - 持空仓
// PLACING_ORDER  - 下单中
// PENDING_ORDER  - 等待订单反馈
```

#### 4. 交易操作

**TradeIntent** - 交易意图（自动撤单追单）：
```java
ctx.submitOrderReq(TradeIntent.builder()
    .contract(contract)
    .operation(SignalOperation.BUY_OPEN)   // BUY_OPEN, SELL_OPEN, BUY_CLOSE, SELL_CLOSE, BUY_REVERSE, SELL_REVERSE
    .priceType(PriceType.OPP_PRICE)        // ANY_PRICE, OPP_PRICE, LAST_PRICE, WAITING_PRICE, LIMIT_PRICE
    .volume(1)
    .timeout(5000)                         // 超时毫秒
    .build());
```

**TradeHelper** - 简化交易助手：
```java
TradeHelper helper = TradeHelper.builder()
    .context(ctx)
    .tradeContract(contract)
    .build();

helper.doBuyOpen(1);      // 对手价买开
helper.doSellClose(1);    // 对手价卖平
helper.doBuyCloseAll();   // 平掉所有多头持仓
```

### 实现交易策略 - 关键步骤

#### Step 1: 类定义
```java
@StrategicComponent(MyStrategy.NAME)   // 必须注解，value 为策略名称
public class MyStrategy extends AbstractStrategy implements TradeStrategy {
    protected static final String NAME = "我的策略";
    
    private InitParams params;          // 策略参数
    private Indicator fastLine;         // 指标定义
    private TradeHelper helper;         // 交易助手
}
```

#### Step 2: 参数配置
```java
public static class InitParams extends DynamicParams {
    @Setting(label="指标合约", order=0)
    private String indicatorSymbol;
    
    @Setting(label="快线周期", type = FieldType.NUMBER, order=1)
    private int fast = 5;
    
    @Setting(label="价格类型", type = FieldType.SELECT, 
             options = {"市价","对手价","排队价"}, 
             optionsVal = {"ANY_PRICE", "OPP_PRICE", "WAITING_PRICE"}, 
             order=2)
    private String priceType = "OPP_PRICE";
}

@Override
public DynamicParams getDynamicParams() {
    return new InitParams();
}

@Override
public void initWithParams(DynamicParams params) {
    this.params = (InitParams) params;
}
```

#### Step 3: 指标初始化
```java
@Override
protected void initIndicators() {
    logger = ctx.getLogger(getClass());
    Contract contract = ctx.getContract(params.indicatorSymbol);
    
    // 创建指标
    this.fastLine = new EMAIndicator(Configuration.builder()
        .contract(contract)
        .indicatorName("EMA10")
        .numOfUnits(ctx.numOfMinPerMergedBar())  // 使用模组 K 线周期
        .build(), 10);
    
    // 注册指标（必须注册才会被框架管理）
    ctx.registerIndicator(fastLine);
    
    // 初始化交易助手
    helper = TradeHelper.builder().context(ctx).tradeContract(contract).build();
}
```

#### Step 4: 交易逻辑实现

**K线驱动策略** (`onMergedBar`)：
```java
@Override
public void onMergedBar(Bar bar) {
    // 确保指标已准备好
    if(!fastLine.isReady() || !slowLine.isReady()) {
        return;
    }
    
    switch (ctx.getState()) {
        case EMPTY -> {
            if(shouldBuy()) {
                helper.doBuyOpen(1);
            }
            if(shouldSell()) {
                helper.doSellOpen(1);
            }
        }
        case HOLDING_LONG -> {
            if(fastLine.value(0) < slowLine.value(0)) {
                helper.doSellClose(1);
            }
        }
        case HOLDING_SHORT -> {
            if(fastLine.value(0) > slowLine.value(0)) {
                helper.doBuyClose(1);
            }
        }
        default -> { /* 其他状态不处理 */ }
    }
}
```

**TICK驱动策略** (`onTick`)：
```java
@Override
public void onTick(Tick tick) {
    logger.info("时间：{} {} 价格：{} 指标值：{}", 
        tick.actionDay(), tick.actionTime(), 
        tick.lastPrice(), fastLine.value(0));
}
```

### 实现自定义指标

```java
public class MyIndicator extends AbstractIndicator implements Indicator {
    
    private double factor;
    private Indicator srcIndicator;  // 可选：依赖其他指标
    
    public MyIndicator(Configuration cfg, int barCount) {
        super(cfg);
        this.factor = 2D / (barCount + 1);
    }
    
    @Override
    protected Num evaluate(Num num) {
        // num 包含当前 K 线的值、时间戳、稳定性标识
        // 返回计算后的值
        double val = calculate(...);
        return Num.of(val, num.timestamp(), num.unstable());
    }
    
    @Override
    public List<Indicator> dependencies() {
        // 如果有依赖其他指标，返回依赖列表
        return Objects.isNull(srcIndicator) ? 
            Collections.emptyList() : List.of(srcIndicator);
    }
}
```

### 核心原则总结

1. **事件驱动**: 策略通过事件回调 (`onTick`, `onMergedBar`, `onOrder`, `onTrade`) 驱动
2. **基于状态的交易**: 始终检查 `ctx.getState()` 避免无效交易
3. **指标就绪检查**: 交易前检查 `indicator.isReady()` 确保指标已初始化
4. **幂等更新**: 指标 `update()` 方法是幂等的，同一数据多次更新效果相同
5. **TradeIntent**: 使用 `TradeIntent` 自动处理撤单追单，避免手动管理订单生命周期
6. **模组状态**: 框架管理模组状态，策略根据状态决定交易行为
7. **依赖注入**: 通过 `ctx.registerIndicator()` 让框架管理指标生命周期
8. **配置**: 使用 `Configuration.builder()` 配置指标参数，与模组 K 线周期对齐

### 示例参考

- `BeginnerExampleStrategy` - 最简策略示例
- `IndicatorExampleStrategy` - 带指标的策略示例
- `MultiContractExampleStrategy` - 多合约多周期示例
- `SimpleSpreadStrategy` - 套利策略示例

## 常见任务

### 添加新策略
1. 在 `northstar-strategy-example` 或独立模块创建类
2. 继承 `AbstractStrategy` 并实现 `TradeStrategy`
3. 使用 `@StrategicComponent("策略名称")` 注解
4. 定义 `InitParams extends DynamicParams` 和 `@Setting` 字段
5. 重写 `initIndicators()` 创建和注册指标
6. 实现 `onMergedBar()` 或 `onTick()` 交易逻辑
7. 适当处理不同 `ModuleState` 值
8. 使用 `TradeHelper` 或 `TradeIntent` 提交订单

### 添加新指标
1. 在 `northstar-api/src/main/java/org/dromara/northstar/indicator/` 下创建类，继承 `AbstractIndicator`
2. 重写 `evaluate(Num num)` 实现计算逻辑
3. 如需依赖其他指标，重写 `dependencies()`
4. 使用 Configuration builder 配置指标参数
5. 返回 `Num.of(value, timestamp, unstable)` 带正确元数据
6. 通过 `ctx.registerIndicator(indicator)` 在策略中注册

### 添加新网关
1. 实现 `MarketGateway` 或 `TradeGateway` 接口
2. 创建 `GatewayFactory` 实现
3. 处理连接生命周期（connect/disconnect）
4. 实现到内部模型的数据转换

## 重要注意事项

- **虚拟线程**: Spring 配置中启用（Spring Boot 3.2+）
- **编码**: 整个项目使用 UTF-8
- **Protobuf**: 生成类在 `xyz.redtorch.pb` 包中 - **请勿手动修改**
- **H2 Console**: 开发 Profile 中在 `/h2-console` 启用
- **静态资源**: Web UI 资源从 `src/main/resources/static` 提供
- **前端项目**: `northstar-monitor-next` 是 Vue3 + Vite 前端，构建时复制到 `northstar-main`
