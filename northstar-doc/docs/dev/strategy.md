# 策略开发

> 「扩展 · 程序化交易」系列之一。前置阅读：[程序化交易](/features/auto-trading)（了解模组、账户与合约的关系）。

平台的策略是一段 Java 代码：告诉模组"每来一根 K 线该看什么、满足什么条件时下什么单"。本文从最小例子出发，讲清一个策略的**必要元素**、**回调时机**、**下单方式**与**参数机制**，最后给出常用 API 速查。

::: tip 不想写代码？
平台自带约 11 个示例策略（名称含"示例"，模组表单勾选"显示示例策略"可见），覆盖双均线、多周期、多合约、套利等主题，可直接创建模组体验，也可打开源码学习。本文示例均取自 `northstar-strategy-example` 模块的真实代码。
:::

## 一个策略的最小骨架

下面是一个完整可运行的双均线策略（改编自示例 `IndicatorExampleStrategy`），只有约 50 行：

```java
package org.dromara.northstar.strategy.example;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

@StrategicComponent(DoubleMAStrategy.NAME)          // ① 注册为策略组件， value 就是策略名
public class DoubleMAStrategy extends AbstractStrategy implements TradeStrategy {

    protected static final String NAME = "双均线示例";

    private Indicator fastLine, slowLine;           // 两条均线
    private TradeHelper helper;                     // 下单助手

    // ② 参数定义类：类名必须是 InitParams、必须 static、必须继承 DynamicParams
    public static class InitParams extends DynamicParams {}

    @Override
    public DynamicParams getDynamicParams() { return new InitParams(); }

    @Override
    public void initWithParams(DynamicParams params) { /* 保存参数（本文无参数） */ }

    // ③ 模组上下文注入后调用：初始化指标、下单助手
    @Override
    protected void initIndicators() {
        Contract c = ctx.bindedContracts().getFirst();
        fastLine = new EMAIndicator(Configuration.builder().contract(c)
                .indicatorName("EMA10").interval(Interval.M15).build(), 10);
        slowLine = new EMAIndicator(Configuration.builder().contract(c)
                .indicatorName("EMA20").interval(Interval.M15).build(), 20);
        ctx.registerIndicator(fastLine);
        ctx.registerIndicator(slowLine);
        helper = new TradeHelper(ctx, c);
    }

    // ④ 每根K线收盘时触发：策略的核心逻辑都在这里
    @Override
    public void onMergedBar(Bar bar) {
        if (bar.unstable()) return;                         // 忽略未收盘的K线
        if (!fastLine.isReady() || !slowLine.isReady()) return;  // 指标预热未完成不交易
        switch (ctx.getState()) {
            case EMPTY -> {
                if (fastLine.value(0) > slowLine.value(0)) helper.buyOpen(1);   // 金叉开多
                if (fastLine.value(0) < slowLine.value(0)) helper.sellOpen(1);  // 死叉开空
            }
            case HOLDING_LONG  -> { if (fastLine.value(0) < slowLine.value(0)) helper.sellClose(1); }
            case HOLDING_SHORT -> { if (fastLine.value(0) > slowLine.value(0)) helper.buyClose(1); }
            default -> {}
        }
    }

    @Override
    public String name() { return NAME; }
}
```

四个必备元素：**① 注解声明**、**② 参数类**（无参也要写空类）、**③ 初始化钩子**、**④ 策略名**。逻辑核心通常全部写在 `onMergedBar` 里。

## 策略是如何被调用的

实现 `TradeStrategy` 接口（推荐继承 `AbstractStrategy`）后，框架会在合适的时机回调以下方法：

| 回调 | 触发时机 | 典型用途 |
|---|---|---|
| `initWithParams(params)` | 创建模组时，**在拿到上下文之前** | 保存前端表单填写的参数（此时不能访问 `ctx`） |
| `initIndicators()` | 模组加载、`ctx` 注入后 | 创建并注册指标、初始化下单助手（`AbstractStrategy` 的固定钩子） |
| `onMergedBar(bar)` | **每根 K 线收盘时** | 策略主逻辑：算信号、下单（含模组停用期间，自行用 `ctx.isEnabled()` 判断） |
| `onTick(tick)` | 每个 TICK 行情到达时 | 需要盘中即时反应的策略（如盘口策略） |
| `onOrder(order)` | 委托单状态变化时 | 跟踪报单反馈（不关心可不重写） |
| `onTrade(trade)` | 每次成交时 | `AbstractStrategy` 已默认打印成交与持仓，一般不用重写 |
| `strategyInfos()` / `customIndicators()` | 监控台渲染时（高级） | 自定义信息面板 / 自定义指标线，见示例 `CustomVisibilityExampleStrategy` |

生命周期要点：

- **策略实例化不走 Spring 容器**：模组创建时按类名反射 `newInstance()`，因此策略类里**不能注入其他 Bean**，保持纯逻辑；
- **停用不等于停止回调**：`onTick`/`onMergedBar` 在模组停用期间仍会被调用，但 **`ctx.submitOrderReq` 会被忽略**——发单被安全地"短路"了；
- **下单异常自动停用**：发单异常时框架会自动停用模组，避免错误逻辑无限下单；
- **状态机**：`ctx.getState()` 返回模组当前状态（`EMPTY` / `HOLDING_LONG` / `HOLDING_SHORT` / `PLACING_ORDER` / `PENDING_ORDER` / `ERROR` 等），策略据此决定该开还是该平；
- **状态可持久化**：`getStoreObject()` / `setStoreObject()` 用于保存自定义计算状态，重启模组后恢复。

## 下单：TradeHelper 与底层接口

日常开发用 **`TradeHelper`**（`new TradeHelper(ctx, 合约)`）即可：

| 方法 | 说明 |
|---|---|
| `buyOpen(vol)` / `sellOpen(vol)` | 市价买开 / 卖开 |
| `buyOpen(price, vol)` / `sellOpen(price, vol)` | 限价重载 |
| `buyClose(vol)` / `sellClose(vol)` | 市价平空 / 平多（注意方向：买入平空仓、卖出平多仓） |
| `buyCloseAll()` / `sellCloseAll()` | 按当前持仓量一次性全平 |

它内部组装的是底层记录 `SubmitOrderReq`：`price = 0` 表示市价，否则限价；还可以携带止盈价 `takeProfit`、止损价 `stopLoss`。需要精细控制（如自定义本地订单号、查询物理账户）时再直接使用 `ctx.submitOrderReq(...)`。

::: warning 一个常见误区
`buyClose` 是**买入平仓**（平掉空头持仓），`sellClose` 是**卖出平仓**（平掉多头持仓）——方向与开仓正好相反。开平仓方向配错是最常见的下单错误。
:::

## 策略参数（前端表单自动生成）

策略的参数不用写前端代码：定义一个 **`InitParams` 内部类**（必须 `static`、必须继承 `DynamicParams`），用 **`@Setting` 注解**标注字段，平台就会自动渲染表单并把用户填写值回传给 `initWithParams`：

```java
public static class InitParams extends DynamicParams {
    @Setting(label="快线周期", type=FieldType.NUMBER, order=10, unit="根")
    private int fastPeriod = 10;     // 字段默认值 = 表单默认值

    @Setting(label="慢线周期", type=FieldType.NUMBER, order=20, unit="根")
    private int slowPeriod = 20;

    @Setting(label="下单量", type=FieldType.NUMBER, order=30, unit="手")
    private int volume = 1;
}

@Override
public void initWithParams(DynamicParams params) {
    InitParams p = (InitParams) params;
    this.fastPeriod = p.fastPeriod;   // 或直接用强转后的对象
}
```

`@Setting` 常用属性：`label`（显示名）、`type`（`TEXT` / `NUMBER` / `SELECT` 等控件类型）、`order`（显示顺序）、`unit`（单位）、`options`（下拉选项）、`required`（是否必填）。

## ctx 常用 API 速查

策略里通过 `ctx`（模组上下文）与平台交互：

| 分组 | 方法 | 说明 |
|---|---|---|
| 行情/合约 | `getContract(unifiedSymbol)` | 按统一编码取合约对象（可获取模组未绑定的合约，用于跨合约指标） |
| | `bindedContracts()` | 模组绑定的全部合约 |
| 指标 | `registerIndicator(indicator)` | 注册指标（详见 [指标与可视化](/dev/indicator)） |
| | `addMergedBarListener(合约, 周期, 监听者)` | 额外订阅某合约某周期的 K 线（多周期策略，见示例 `MultiPeriodExampleStrategy`） |
| 下单 | `submitOrderReq(req)` / `cancelOrderReq(req)` | 底层委托 / 撤单 |
| | `setOrderRequestFilter(filter)` | 自定义下单风控过滤器（下单前拦截） |
| 账户 | `getModuleAccount()` | 模组账户（持仓、权益、未完成订单） |
| | `getAccount(合约)` | 合约绑定的物理账户 |
| 状态 | `getState()` / `isEnabled()` | 模组状态机状态 / 启用标志 |
| | `disabledModule()` | 策略主动停用模组 |
| 其他 | `getLogger(Class)` | 模组独立日志（每模组一个文件，便于排查） |
| | `getDefaultVolume()` | 模组默认下单手数 |

## 策略如何被平台发现

1. **内置示例**：`northstar-strategy-example` 模块随主程序一起编译，示例策略通过组件扫描自动注册；
2. **外部 jar 动态加载**：把自己写的策略打成 jar，放到主程序 jar 的**同级目录**，按启动脚本（`startup.sh` 中的 `-Dloader.path=$(pwd)`）启动，重启后即出现在模组表单的"绑定策略"下拉中。

## 下一步

- 想给策略配上图表指标 → [指标与可视化](/dev/indicator)
- 写完想验证效果 → [回测与策略探索](/dev/backtest)
- 想用强化学习替代手工规则 → [强化学习训练](/dev/rl-training)
