---
name: northstar-strategy
description: Northstar Pro 量化平台交易策略开发指南。当用户要求编写、修改或解释一个交易策略（Strategy）、自定义参数（@Setting/InitParams）、指标（Indicator）、事件驱动回调或回测验证时触发。
type: prompt
whenToUse: 用户要求写一个新的交易策略、给策略加自定义参数、使用/创建指标、处理 onTick/onBar/onOrder/onTrade 事件，或用回放网关验证策略时
---

# Northstar Pro 交易策略开发指南

本指南基于源码版本 9.1.6 整理，覆盖策略开发的五个核心环节：**初始化 → 自定义参数 → 指标 → 事件驱动 → 回测验证**。所有 API 均以源码为准，写代码前先核对对应文件。

## 0. 策略五件套（最小骨架）

一个能被平台发现、加载、回测的策略必须具备以下五个要素，缺一不可：

1. `@StrategicComponent(NAME)` 类注解（元注解是 Spring `@Component`，用于平台发现策略）
2. `extends AbstractStrategy implements TradeStrategy`
3. **static 内部类 `InitParams`（类名固定，不可改）**，继承 `DynamicParams`，用 `@Setting` 注解声明参数
4. 实现 `getDynamicParams()` / `initWithParams()` 模板方法接收参数
5. 实现 `name()` 返回策略名

完整最小骨架（改写自 `northstar-strategy-example/src/main/java/org/dromara/northstar/strategy/example/BeginnerExampleStrategy.java`）：

```java
@StrategicComponent(BeginnerExampleStrategy.NAME)
public class BeginnerExampleStrategy extends AbstractStrategy implements TradeStrategy {

    protected static final String NAME = "示例-简单策略";
    private InitParams params;

    @Override
    public DynamicParams getDynamicParams() { return new InitParams(); }

    @Override
    public void initWithParams(DynamicParams params) { this.params = (InitParams) params; }

    @Override
    protected void initIndicators() {
        // 初始化指标 / 合约 / TradeHelper。注意：不要写在构造器里，ctx 此刻才可用
        logger = ctx.getLogger(getClass());
    }

    @Override
    public void onTick(Tick tick) {
        // 每个 TICK 触发一次（模组停用也会被调用）
    }

    @Override
    public void onMergedBar(Bar bar) {
        // 每根已收盘 K 线触发一次（模组停用也会被调用）
    }

    @Override
    public String name() { return NAME; }

    public static class InitParams extends DynamicParams {
        @Setting(label = "操作间隔", type = FieldType.NUMBER, order = 10, unit = "秒")
        private int actionInterval = 60;   // 默认值 = 字段初值

        @Setting(label = "锁仓演示", type = FieldType.SELECT, options = {"true", "false"}, order = 20)
        private boolean showHedge;
    }
}
```

**关键约束：**

- 策略运行时实例由平台**反射 `Class.forName + 无参构造` 新建**（`ModuleService.resolveComponent`，`northstar-main/src/main/java/org/dromara/northstar/app/service/ModuleService.java:281`），**不是 Spring 容器里的 bean**。因此策略类必须有无参构造，`InitParams` 必须是可实例化的 `static` 类；`@Autowired` 字段注入在运行时实例上不生效——策略只应通过 `ctx` 与平台交互。
- 策略类必须位于 `org.dromara.northstar` 包（含子包）下才会被组件扫描发现。
- `AbstractStrategy.setContext()` 不可重写；它在平台装配模组时调用，内部依次执行 `initIndicators()` 和 `initMultiContractHandler()`。

## 1. 初始化

### 1.1 初始化逻辑放哪里

| 时机 | 入口 | 说明 |
|---|---|---|
| 参数注入 | `initWithParams(DynamicParams)` | 模组创建/加载时由平台反射调用，先于 setContext |
| 指标/合约/helper | `initIndicators()` | setContext 内调用，**此时 ctx 已可用**，创建指标并 `ctx.registerIndicator()` |
| 多合约处理 | `initMultiContractHandler()` | 空模板方法，按需重写 |

**禁止在构造器里使用 `ctx`**——构造时上下文尚未注入，会 NPE。

### 1.2 上下文 `IModuleContext` 常用 API

`ctx` 字段类型是 `IModuleStrategyContext`（`northstar-api/src/main/java/org/dromara/northstar/strategy/IModuleStrategyContext.java`）：

```java
ctx.submitOrderReq(SubmitOrderReq orderReq);              // 委托下单（唯一正确的下单入口，带风控过滤与状态机）
ctx.cancelOrder(CancelOrderReq cancelReq);                // 按本地订单 ID 撤单
ctx.getContract(String unifiedSymbol);                    // 按统一编码取合约（仅限绑定合约）
ctx.bindedContracts();                                    // 模组绑定的全部合约
ctx.getAccount(Contract contract);                        // 合约绑定的物理账户（IAccount）
ctx.getModuleAccount();                                   // 模组逻辑账户（IAccount）
ctx.getState();                                           // 模组状态机（ModuleState）
ctx.isEnabled();                                          // 模组是否启用
ctx.getDefaultVolume();                                   // 默认下单量（模组设置）
ctx.getLogger(Class<?> clz);                              // 模组级日志（前端日志页可见）
ctx.registerIndicator(Indicator indicator);               // 注册指标
ctx.addMergedBarListener(contract, interval, listener);   // 注册其它周期的 K 线监听
ctx.disabledModule();                                     // 策略主动停用模组
```

## 2. 自定义参数配置

### 2.1 声明参数

在 **static 内部类 `InitParams`**（类名固定）中，用 `@Setting` 注解声明字段（`northstar-api/src/main/java/org/dromara/northstar/common/model/Setting.java`）：

```java
public static class InitParams extends DynamicParams {
    @Setting(label = "指标合约", order = 0)                        // 文本框，必填
    private String indicatorSymbol;

    @Setting(label = "快线周期", type = FieldType.NUMBER, order = 1)
    private int fast = 5;                                          // 默认值 = 初值

    @Setting(label = "方向", type = FieldType.SELECT,
             options = {"做多", "做空"}, optionsVal = {"LONG", "SHORT"}, order = 2)
    private String direction = "LONG";

    @Setting(label = "启用过滤", type = FieldType.SELECT,
             options = {"true", "false"}, order = 3)
    private boolean filterEnabled;

    @Setting(label = "备注", type = FieldType.TEXT, required = false, order = 4)
    private String remark;
}
```

`@Setting` 常用属性：`label`（前端显示名）、`type`（控件类型）、`order`（显示顺序）、`unit`、`options/optionsVal`（SELECT 选项）、`placeholder`、`required`（默认 true）。

`FieldType` 枚举（`northstar-api/src/main/java/org/dromara/northstar/common/constant/FieldType.java`）：`TEXT, PASSWORD, NUMBER, INTEGER, DATE, SELECT, MULTI_SELECT`。布尔参数用 `FieldType.SELECT` + `options = {"true","false"}`。

### 2.2 读取参数

平台在模组加载时反射调用 `initWithParams`，把前端传来的值经 `DynamicParams.resolveFromSource()` 反射赋进 `InitParams` 字段：

```java
@Override
public void initWithParams(DynamicParams params) { this.params = (InitParams) params; }
```

之后全程用 `params.fast`、`params.indicatorSymbol` 等访问。

### 2.3 模组级设置（不属于策略代码，但创建模组时要懂）

`ModuleDescription`（`northstar-api/src/main/java/org/dromara/northstar/common/model/ModuleDescription.java`）关键字段：

| 字段 | 说明 |
|---|---|
| `name` | 模组名称 |
| `type` | `SPECULATION` / `ARBITRAGE` |
| `usage` | `PLAYBACK`（回测）/ `UAT`（模拟盘）/ `PROD`（实盘） |
| `initBalance` | 模组逻辑账户初始资金（回测时就是初始资金） |
| `defaultVolume` | 默认下单量 |
| `weeksOfDataForPreparation` | 预热数据量（周） |
| `bindedContracts` | 绑定合约 |
| `bindedAccounts` | 绑定账户（网关 ID）；PLAYBACK 模组可为空 |
| `strategySetting` | `StrategyMetaInfo{name, className, settings}`——策略以**全限定类名**标识 |

校验规则：绑定 PLAYBACK 合约的模组 `usage` 必须是 `PLAYBACK`；PROD 模组不能绑 SIM 网关账户。

## 3. 指标的使用与创建

### 3.1 使用内置指标

标准三步（参考 `IndicatorExampleStrategy.java:110-132`）：

```java
@Override
protected void initIndicators() {
    logger = ctx.getLogger(getClass());
    Contract c = ctx.getContract(params.indicatorSymbol);

    // 1. 构造：Configuration 指定合约/周期/显示名，参数跟在构造器里
    this.fastLine = new EMAIndicator(Configuration.builder()
            .contract(c).indicatorName("EMA10").interval(Interval.M15).build(), 10);
    MACDIndicator macd = new MACDIndicator(c, Interval.M15, 12, 26, 9);
    this.macdDiff = macd.getDiffLine();
    this.macdDea = macd.getDeaLine();

    // 2. 注册（会递归注册 dependencies()，自动绑定行情更新）
    ctx.registerIndicator(fastLine);
    ctx.registerIndicator(macd);
}
```

**注意：`IModuleContext` 没有 `indicators()` 之类的工厂 helper，直接 `new` + `registerIndicator`。**

### 3.2 在事件中取值

```java
@Override
public void onMergedBar(Bar bar) {
    if (!fastLine.isReady()) return;          // 缓存未填满，必须先判断
    double current = fastLine.value(0);       // 0 = 当前值，-1 = 上一步，依此类推
    Num num = fastLine.get(0);                // 取完整 Num（含 timestamp、unstable 标记）
}
```

- `isReady()`：环形缓存填满才返回 true，未就绪时 `value()` 不可信。
- `Interval` 枚举：`M1, M3, M5, M10, M15, M30, H1, H2, H4, D1, WK1`。`onMergedBar` 按指标注册的周期触发；同一策略可监听多周期（用 `ctx.addMergedBarListener` 或按 `bar.interval()` 区分）。
- `Num`（`indicator/model/Num.java`）：`record Num(double value, long timestamp, boolean unstable)`。`unstable=true` 表示值来自未收盘 K 线，可能变化。
- `Configuration`（`indicator/model/Configuration.java`，`@Builder` record）常用项：`indicatorName`、`contract`、`interval`（默认 M1）、`valueType`（默认 CLOSE）、`cacheLength`（可回溯长度，默认 16）、`visible`（是否上图，默认 true）、`placement`（主/副图）、`ifPlotPerBar`。**`contract` 为 null 会抛 "未绑定计算合约"**；可见指标 `indicatorName` 不可重名。

### 3.3 内置指标清单（包 `org.dromara.northstar.indicator`）

| 分类 | 类 | 说明 |
|---|---|---|
| trend | `trend/MAIndicator` | 构造 `(Contract, Interval, int barCount)` 或 `(Configuration, int)` |
| trend | `trend/SMAIndicator`、`trend/EMAIndicator` | 加权均线 / EMA |
| trend | `trend/MACDIndicator` | `(Contract, Interval, fast, slow, signal)`，`getDiffLine()`/`getDeaLine()` |
| trend | `trend/PBXIndicator` | 瀑布线 |
| momentum | `momentum/RSIIndicator`、`momentum/KDIndicator` | RSI / KD |
| volatility | `volatility/ATRIndicator`、`volatility/BOLLIndicator` | ATR / 布林带（多线） |
| volume | `volume/VWAPIndicator`、`volume/VolumeBoostingIndicator` | 量加权均价 / 放量 |
| wave | `wave/MABasedWaveIndicator` | 均线波浪 |
| helper | `helper/SimpleValueIndicator` | 透传原值，适合包装自定义序列 |
| helper | `helper/{Diff,Sum,HHV,LLV,StandardDeviation,Normalize}Indicator` | 派生计算 |

### 3.4 创建自定义指标

继承 `AbstractIndicator`，**只需实现一个抽象方法 `protected abstract Num evaluate(Num num)`**。`update()` 已由基类实现（时间戳幂等去重后调用 evaluate，非 NaN 入环形缓存）。`dependencies()` 返回源指标列表即可被框架递归更新。

最简透传指标（`helper/SimpleValueIndicator.java:13`）：

```java
public class SimpleValueIndicator extends AbstractIndicator {
    public SimpleValueIndicator(Configuration cfg) { super(cfg); }
    @Override
    protected Num evaluate(Num num) { return num; }
}
```

带状态的典型写法（参照 `trend/MAIndicator.java:42-58`，内部用 `RingArray<Num>` 保存样本窗口）。

## 4. 事件驱动

### 4.1 回调总览（只有 4 个事件回调）

事件流：`DisruptorFastEventEngine` → `TradeModule.onEvent`（先按绑定合约过滤）→ `ModuleContext` → 策略回调：

| 回调 | 触发时机 | 典型用途 |
|---|---|---|
| `onTick(Tick tick)` | 每个 TICK 一次；**模组停用也触发** | 盘口、最新价、止盈止损即时判断 |
| `onMergedBar(Bar bar)` | 每根**已收盘** K 线一次；停用也触发 | 指标信号、开平仓决策主战场 |
| `onOrder(Order order)` | 本模组委托状态变动（按 `originOrderId` 过滤） | 跟踪挂单状态、超时撤单 |
| `onTrade(Trade trade)` | 成交回报（`AbstractStrategy` 默认实现已打印持仓/状态日志） | 成交确认、加仓计数 |

**没有 `onPosition` / `onAccount` / `onDisabled` 回调。** 持仓、账户信息要主动查询：

```java
ctx.getState();                                                    // 模组状态机
ctx.getModuleAccount().getPosition(PositionDirectionEnum.PD_Long, contract);  // 持仓（Optional）
ctx.getModuleAccount().accountBalance();                           // 模组权益
ctx.getAccount(contract).availableBalance();                       // 物理账户可用
ctx.getModuleAccount().getNonclosedNetPosition(contract);          // 净持仓，正=多 负=空
```

`ModuleState` 枚举（`constant/ModuleState.java`）：`EMPTY, HOLDING_LONG, HOLDING_SHORT, HOLDING_HEDGE, EMPTY_HEDGE, PLACING_ORDER, PENDING_ORDER, RETRIEVING_FOR_CANCEL, ERROR`，辅助方法 `isHolding()` / `isEmpty()` / `isOrdering()` / `isWaiting()`。信号策略的标准写法是 `switch (ctx.getState())`：

```java
switch (ctx.getState()) {
    case EMPTY        -> { if (shouldBuy())  helper.buyOpen(1); }
    case HOLDING_LONG -> { if (shouldSell()) helper.sellClose(1); }
    default -> {}
}
```

### 4.2 K 线周期机制

- 网关只产 M1 K 线（或合成 TICK），`BarMerger` 向上合成大周期；策略收到的 `onMergedBar` 周期 = 指标注册/监听的周期。
- 多合约模组：每个合约每个周期分别触发，用 `bar.contract()` 区分。
- 其他周期：`ctx.addMergedBarListener(contract, Interval.M15, listener)`（见 `MultiPeriodExampleStrategy.java:121`）。

### 4.3 核心模型关键字段（`common/model/core/`，均为 `@Builder` record）

- **Tick**：`lastPrice, bidPrice/bidVolume, askPrice/askVolume（List）, volumeDelta, openInterest, upperLimit/lowerLimit, preSettlePrice, contract, gatewayId, actionTimestamp` 等。
- **Bar**：`openPrice/highPrice/lowPrice/closePrice, volume, volumeDelta, interval（Interval）, unstable, contract, actionTimestamp, barEndDateTime()`。
- **Order**：`originOrderId（本地ID）, orderId（网关ID）, direction, offsetFlag, orderPriceType, orderStatus, price, totalVolume, tradedVolume, stopPrice`。
- **Trade**：`originOrderId, direction, offsetFlag, price, volume, contract, tradeTimestamp`。
- 方向/开平枚举来自 protobuf `xyz.redtorch.pb.CoreEnum`：`DirectionEnum.D_Buy/D_Sell`、`OffsetFlagEnum.OF_Open/OF_Close/OF_CloseToday`、`PositionDirectionEnum.PD_Long/PD_Short`、`OrderStatusEnum.OS_AllTraded` 等。

### 4.4 下单

唯一正确入口是 `ctx.submitOrderReq(...)`（不要直接调 `IAccount.submitOrder`，会绕过模组状态机与风控过滤——默认过滤器每合约每日 4000 单上限，超限自动停用模组）。

```java
// 方式一：直接构造。price 不传或为 0 = 市价，否则限价
ctx.submitOrderReq(SubmitOrderReq.builder()
        .contract(tick.contract())
        .direction(DirectionEnum.D_Buy)          // D_Buy / D_Sell
        .offsetFlag(OffsetFlagEnum.OF_Open)      // OF_Open / OF_Close / OF_CloseToday
        .volume(ctx.getDefaultVolume())
        // .price(3350)                          // 限价；省略 = 市价
        // .stopLoss(3300) .takeProfit(3400)     // 附带止损止盈
        .build());

// 方式二：TradeHelper（northstar-api/.../common/utils/TradeHelper.java），语义更清晰
TradeHelper helper = new TradeHelper(ctx, contract);
helper.buyOpen(1);            helper.sellOpen(1);          // 市价开
helper.buyClose(1);           helper.sellClose(1);         // 市价平
helper.buyOpen(price, 1);                                   // 限价开
helper.buyCloseAll();         helper.sellCloseAll();       // 全平
```

撤单：`ctx.cancelOrder(CancelOrderReq.builder().originOrderId(oid).build())`（记录下单返回的本地 ID）。回放环境下 `PlaybackModuleContext` 以 lastTick 最新价**立即全部成交**。

## 5. 回测验证

### 5.1 交互式回放（回放网关 + PLAYBACK 模组）——主流程

四步 REST 调用（也可在 Web 界面完成同样操作）：

**① 创建回放网关** — `POST /northstar/gateway/create`，body `GatewayDescription`：

```json
{
  "gatewayId": "PB_GATEWAY",
  "channelType": "PLAYBACK",
  "gatewayUsage": "MARKET_DATA",
  "autoConnect": true,
  "subscribedContracts": [{"unifiedSymbol": "rb2505@SHFE@FUTURES", "gatewayId": "PB_GATEWAY"}],
  "settings": {
    "preStartDate": "20250101",
    "startDate": "20250301",
    "endDate": "20250331",
    "precision": "MEDIUM",
    "speed": "NORMAL"
  }
}
```

settings 对应 `PlaybackGatewaySettings`（`northstar-gateway-playback/.../PlaybackGatewaySettings.java`，同样用 `@Setting` 声明）：`preStartDate`（预热起始日，可选）、`startDate`/`endDate`（YYYYMMDD，必填；结束日自动截到昨天）、`precision`（LOW/MEDIUM/HIGH = 每分钟 4/30/120 个 TICK）、`speed`（NORMAL/SPRINT/RUSH）。

**② 预热数据** — `GET /northstar/playback/preload`：把预热段历史 M1 K 线转成 dummy TICK 驱动指标与缓存，避免"指标未就绪错过信号"。

**③ 创建回测模组** — `POST /northstar/module`，`ModuleDescription`：`usage=PLAYBACK`、绑定 PLAYBACK 合约、`initBalance` = 回测初始资金、`bindedAccounts` 可为空、`strategySetting.className` = 策略全限定类名 + settings 参数值。

**④ 回放控制** — `GET /northstar/playback/start` 开始；`pause` 暂停；`PUT /northstar/playback/speed/{毫秒}` 调速；`GET /northstar/playback/time/{timestamp}` 跳时间；`GET /northstar/playback/reset` 复位（重建所有 PLAYBACK 模组）。回放状态经 SSE 事件 `PLAYBACK_STATUS` 推送。

### 5.2 批量回测（策略探索 Strategy Explorer）

适合参数批量验证，无需手工建网关和模组：

- `POST /northstar/explorer/task`，body `ExplorerTaskDescription{taskName, preloadFrom, startDate, endDate, contracts, initBalance, strategySetting}`。
- `PUT /northstar/explorer/task?taskName=xx` 运行；`GET /northstar/explorer/task/details` 看每个模组汇总（权益、手续费、平仓盈亏、**最大回撤、胜率、盈亏比、年化**）；`GET /northstar/explorer/trades?moduleName=xx` 看成交明细。

### 5.3 查看结果

- 成交记录：`GET /northstar/module/{name}/deals`；运行时：`GET /northstar/module/{name}/rt`；K 线+指标：`GET /northstar/module/{name}/chart`。
- 实时推送：SSE `/northstar/sse/stream`。
- 程序化核对权益：`ctx.getModuleAccount().accountBalance()`；模组累计盈亏 = 已实现 + 浮盈 - 手续费。

### 5.4 SIM 模拟盘 vs PLAYBACK 回测

| | SIM 模拟盘 | PLAYBACK 回放 |
|---|---|---|
| 网关 | `SimMarketGateway` + `SimTradeGateway`（行情/交易分开建） | `PlaybackGatewayAdapter`（仅行情） |
| 行情 | 实时合成 TICK | 历史 M1 按精度合成 TICK |
| 成交 | 模拟撮合（保证金/手续费/持仓） | lastTick 最新价立即全成 |
| 模组 | `usage=UAT`，绑 SIM 交易网关 | `usage=PLAYBACK`，不绑账户 |
| 初始资金 | `PUT /northstar/gateway/deposit/{gatewayId}?amount=` 入金 | 模组 `initBalance` |
| 时间 | 实时 | 可控速/暂停/逐 K 的回放区间 |

验证节奏建议：先在 PLAYBACK 上跑通信号与盈亏，再切 SIM（`usage=UAT`）验证下单链路，最后才考虑 PROD。

## 6. 常见错误清单

1. 把初始化写进构造器 → NPE（ctx 尚未注入）。放 `initIndicators()`。
2. `InitParams` 类名写错 / 不是 static → 平台反射找不到参数类，`resolveComponent` 失败。
3. 下单直接调 `IAccount.submitOrder()` → 绕过风控与状态机。必须走 `ctx.submitOrderReq()`。
4. 指标未 `isReady()` 就读值 → 得到 NaN 或错值。
5. `Configuration` 未设 `contract` → 抛 "未绑定计算合约"。
6. 期待 `onPosition`/`onAccount` 回调 → 不存在，改为主动查询 `ctx.getModuleAccount()` / `ctx.getState()`。
7. 策略类放错包（不在 `org.dromara.northstar` 下）→ 组件扫描发现不了。
8. 回测模组 `usage` 不是 `PLAYBACK` 或绑了非 PLAYBACK 合约 → 模组校验失败。

## 7. 参考示例（northstar-strategy-example 模块）

| 类 | 演示重点 |
|---|---|
| `BeginnerExampleStrategy` | 必要元素骨架、开平仓、超时撤单、ModuleState |
| `IndicatorExampleStrategy` | EMA + MACD 双指标金叉死叉策略（最佳入门范例） |
| `IndicatorDemoStrategy` | 指标各种用法 |
| `MultiPeriodExampleStrategy` | 多周期 + `addMergedBarListener` |
| `MultiContractExampleStrategy` | 多合约 |
| `SimpleSpreadStrategy` | 价差/套利 |
| `CustomVisibilityExampleStrategy` | `strategyInfos()` / `customIndicators()` 监控面板（高级） |

读完整范例：先读 `IndicatorExampleStrategy.java`（约 130 行，覆盖参数、指标、状态机、下单全链路），再按需求查其它示例。
