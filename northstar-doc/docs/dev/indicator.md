# 指标与可视化

> 「扩展 · 程序化交易」系列之一。前置阅读：[程序化交易](/features/auto-trading)（了解模组与运行时页面）。

在 [模组运行时](/features/auto-trading#模组运行时) 页面，除了 K 线本身，我们还可以把策略计算的技术指标（均线、MACD、布林线……）直接画在图表上，与行情对照观察。本文用尽量少的术语讲清楚：

- **原理**：一条"指标线"在平台里是如何定义、计算、传输和绘制的；
- **案例**：不写代码体验内置指标；以及在策略代码里注册单线指标、自定义多线指标组。

::: tip 本文适合谁
前半部分（原理与界面操作）适合所有用户，不需要编程基础；后半部分（策略代码）适合准备自己开发策略的读者，需要一点 Java 基础。
:::

## 从界面说起

打开 **交易自动化 → 模组管理**，对任意模组点 **运行时状态**，页面顶部是一排图表工具栏：

```
[1h ▾]  [氧化铝指数 ▾]  [选择技术指标 ▾]  [＋] [－] [🗑] [📊] [⬇]
 周期     合约            指标多选框        增  删  清空 持仓线 下载
```

- **周期**：K 线周期（1m ~ 1wk），切换后主图与所有副图一起重算；
- **合约**：模组绑定的合约，多合约模组可切换；
- **指标多选框**：列出该合约在当前周期下可显示的指标，**按"指标组"聚合显示**（这是重点，下文详解）；
- **＋ / － / 🗑**：把选中的指标组添加到图表 / 从图表移除 / 清空全部指标；
- **📊**：显示或隐藏历史持仓线段；**⬇**：把当前图表数据导出为 CSV。

图表区由 **1 个主图 + 最多 5 个副图窗格** 组成。主图绘制 K 线（价格坐标）；副图每个窗格绘制一类副图指标（如 MACD、KD），各自有独立的纵坐标。

## 核心原理

### 一个指标对象 = 一条指标线的值序列

平台里最基本的概念是 **指标（Indicator）对象**：它对应**一条指标线的值序列**——每来一根 K 线，它产出一个数值，把连续的数值连起来，就是我们熟悉的指标线。

```
K线:    ┃ 第1根 ┃ 第2根 ┃ 第3根 ┃ 第4根 ┃ ...
MA5值:  100.2   100.5   100.1   100.8         ← 一个 Indicator 对象负责产出这一行数
         └────────────── 连成一条线 ──────────────┘
```

以均线为例，`MA5` 就是一个 Indicator 对象，它的值是最近 5 根 K 线收盘价的平均值。**一个对象维护一条线**，这让每条线都能独立更新、独立缓存，非常好维护。

### 多线指标天然是多个对象的组合

有些指标在图表上天生是几条线的组合。最典型的是 **MACD**，它在任何行情软件里都是"红绿柱 + DIF + DEA"同图显示：

```
        MACD（副图窗格）
  DIF  ╱╲    ╱╲
      ╱  ╲  ╱  ╲      DEA
     ╱    ╲╱    ╲╱
    ███▲▲▲▼▼▼▲▲▲▼▼▼    ← 红绿柱（MACD 值本身）
   ──────────────── 零轴
```

在平台里，MACD 由 **3 个 Indicator 对象** 协作完成：

| 对象 | 角色 | 是否在图上显示 |
|---|---|---|
| `MACD` | 红绿柱（核心值 = (DIF − DEA) × 2） | ✅ 按柱状渲染 |
| `DIFF` | DIF 线（快慢 EMA 之差） | ✅ 折线 |
| `DEA` | DEA 线（DIF 的 EMA） | ✅ 折线 |
| `MACD_fast` / `MACD_slow` | 两条中间计算用的 EMA | ❌ 仅内部使用 |

### 指标组（group）与主副图（placement）

如果前端把 3 个对象平铺成 3 个列表项，用户就得自己搞清"这三条是一伙的、要加到同一个窗格"——这正是老版本的做法，繁琐且容易出错。

现在每个指标对象在定义时用两个配置项声明自己的"身份"：

- **`group`（指标组）**：同组的指标线在前端**聚合成一个列表项**。比如 MACD 的三个对象都登记 `group = "MACD"`，指标框里就只出现一项"MACD"；
- **`placement`（主副图归属）**：声明该指标应该画在**主图**（`MAIN`，与 K 线共用价格坐标，如均线、布林轨）还是**副图**（`SUB`，独立窗格，如 MACD、KD）。

`group` 留空表示"自成一组"，单线指标无需关心分组；`placement` 缺省为 `MAIN`（主图），与业界习惯一致： trend 类指标叠在主图上，摆动类指标放副图。

平台内置的多线指标已经预先登记好了组名与归属：

| 指标 | 组名 | 图上的成员线 | 主/副图 | 策略中的注册方式 |
|---|---|---|---|---|
| MACD | `MACD` | DIFF、DEA、红绿柱 | 副图 | `new MACDIndicator(合约, 周期, 12, 26, 9)` |
| 布林带 BOLL | `BOLL` | MID（中轨）、UPPER（上轨）、LOWER（下轨） | 主图 | `new BOLLIndicator(合约, 周期, 20, 2)` |
| KD | `KD` | K、D | 副图 | `new KDIndicator(合约, 周期, 9, 3, 3)` |
| 瀑布线 PBX | `PBX` | PB1 ~ PB6 六条线 | 主图 | `new PBXIndicator(合约, 周期)` |

::: info 内部辅助线不上图
MACD 的 `MACD_fast`/`MACD_slow`、BOLL 的 `STD`（标准差）、KD 的 `RSV`，都是算出线来的"脚手架"。它们被标记为 `visible(false)`：照常参与计算，但不会出现在指标列表里，也不会画到图上。
:::

### 数据是怎么流到图表上的

```
策略代码                          后端（模组运行时）                    前端浏览器
┌────────────────┐   每根K线     ┌──────────────────────────┐  轮询  ┌──────────────────────┐
│ ctx.register   │ ───────────→ │ 按周期计算并缓存各指标值  │ ─────→ │ 指标框按 group 聚合   │
│ Indicator(…)   │   驱动计算    │ /module/{模组名}/chart   │  JSON  │ 列表显示              │
└────────────────┘              └──────────────────────────┘        └──────────────────────┘
                                                                          │ 用户点"＋"
                                                                          ▼
                                                            MAIN 组 → 画到主图（K线窗格）
                                                            SUB  组 → 找第一个空闲副图窗格放入
```

要点：

1. **注册**：策略通过 `ctx.registerIndicator(...)` 把指标交给模组，模组会递归登记指标的依赖（比如 MACD 会自动带上 DIFF/DEA 乃至内部的 EMA）；
2. **计算与缓存**：每来一根 K 线，模组按周期驱动所有已注册指标更新一次，结果缓存在内存；
3. **下发**：页面打开或自动刷新时，前端调用图表接口，后端把每个可见指标的名称、线型、分组、归属连同 K 线数据一起返回；
4. **绘制**：前端把同组的多条线**堆叠进同一个窗格**——点一次"＋"，整组指标一次到位，不需要也不允许手动选窗格；
5. **记忆**：添加了哪些指标组、各自在哪个窗格，按"**模组 + 合约**"记在浏览器本地，切换周期或下次打开自动恢复。

副图窗格最多 5 个，占满后再添加副图指标会提示"副图窗格已满，无法添加副图指标"——先移除不用的指标组再试。

## 动手案例

### 案例一：不写代码，体验分组效果

平台自带的 **示例-指标演示** 策略（`IndicatorDemoStrategy`）专门用于演示指标功能，它注册了从基础到精通各种难度的指标。

1. **创建模组**：交易自动化 → 模组管理 → 新建，勾选 **显示示例策略**，绑定策略 **示例-指标演示**，参数"指标合约"填一个模组已绑定合约的合约编码（如 `rb0000@SHFE@FUTURES`），绑定账户与合约后保存；
2. **产生行情**：回测用途的模组按 [历史回放](/features/playback) 流程预加载并启动回放；模拟盘用途的模组启用后连接行情网关；
3. **打开运行时状态**，把周期切到 **15m**（演示策略的指标都注册在 15 分钟周期）；
4. 点开**指标多选框**，可以看到列表按组聚合：

   ```
   ☑ EMA5            ← 单线指标，自成一组
   ☐ KD              ← 一组（K 线 + D 线）
   ☑ MA5
   ☐ MACD            ← 一组（DIFF + DEA + 红绿柱），只有一项！
   ☐ BOLL            ← 一组（MID + UPPER + LOWER）
   ...
   ```

5. 勾选 **MACD** 点 **＋**：自动出现一个副图窗格，红绿柱与 DIFF、DEA 三条线同图显示；勾选 **EMA5** 点 **＋**：EMA5 直接叠加在主图 K 线上；
6. 切换周期再切回来，或刷新页面，已添加的指标组自动恢复。

::: warning 看不见 K 线？
图表的前提是"有数据"：行情/回放正在产生数据、且数据合约与图表选择的合约一致。这两个条件任一不满足，图都是空的——这不是指标的问题。
:::

### 案例二：在策略里注册一个单线指标

打开 `northstar-strategy-example` 模块的 `IndicatorDemoStrategy`，最基础的注册写法只有两行：

```java
Contract c = ctx.getContract(params.indicatorSymbol);   // 取合约对象

// 方式一：用 Configuration 逐项配置（最通用）
Configuration cfg = Configuration.builder()
        .contract(c)                // 绑定合约（必填）
        .indicatorName("MA5")       // 显示名称，也是唯一标识，不能重名
        .interval(Interval.M15)     // K线周期
        .build();
ctx.registerIndicator(new MAIndicator(cfg, 5));         // MA5

// 方式二：内置单线指标同样支持便捷构造
ctx.registerIndicator(new EMAIndicator(cfg, 5));        // EMA5
```

注册后无需其他操作：模组会自动计算，前端指标框自动出现"MA5"、"EMA5"两项。

`Configuration` 的常用字段速查：

| 字段 | 默认值 | 说明 |
|---|---|---|
| `indicatorName` | `""` | 显示名称，**同一合约同一周期下不能重名** |
| `contract` | 必填 | 指标计算绑定的合约 |
| `interval` | `M1` | K 线周期（M1/M5/M15/H1/D1…） |
| `valueType` | `CLOSE` | 从 K 线取哪个字段（开/高/低/收/量/仓…） |
| `lineStyle` | `LINES` | 线型：`LINES` 折线 / `BARS` 柱 / `ZERO_BASED_BARS` 零轴柱 / `DOTS` 圆点 |
| `cacheLength` | `16` | 指标内部回溯缓存长度 |
| `ifPlotPerBar` | `false` | 跨周期指标是否映射到每根 K 线 |
| `visible` | `true` | 是否在模组图表中显示（`false` ＝ 纯计算用的辅助线） |
| `group` | `""` | 指标组名；同组在前端聚合成一项 |
| `placement` | `MAIN` | `MAIN` 主图 / `SUB` 副图 |

### 案例三：自定义一个多线指标组

假设我们想做一个"双均线通道"：`DEMA_FAST`（快 EMA5）与 `DEMA_SLOW`（慢 EMA20）总是一起看。做法是写一个指标类，把两条线登记为同一组：

```java
@Getter
public class DoubleEMAIndicator extends AbstractIndicator implements Indicator {

    private Indicator fastLine;
    private Indicator slowLine;

    public DoubleEMAIndicator(Contract c, Interval interval) {
        // ① 基座配置：声明组名与主副图归属
        Configuration base = Configuration.builder()
                .contract(c)
                .interval(interval)
                .group("DEMA")                       // 组名：前端列表里只显示一项 "DEMA"
                .placement(IndicatorPlacement.MAIN)  // 主图指标：叠加在 K 线上
                .build();

        // ② 本指标没有单独的值，不把自己画出来
        super(base.toBuilder().visible(false).build());

        // ③ 两条子线都从基座派生，自动继承 group 与 placement
        fastLine = new EMAIndicator(base.toBuilder().indicatorName("DEMA_FAST").build(), 5);
        slowLine = new EMAIndicator(base.toBuilder().indicatorName("DEMA_SLOW").build(), 20);
    }

    @Override
    public List<Indicator> dependencies() {
        // ④ 声明依赖，框架会递归登记并驱动计算
        return List.of(fastLine, slowLine);
    }

    @Override
    protected Num evaluate(Num num) {
        return Num.NaN();   // 本体不产出值，只负责组织两条线
    }

    @Override
    public Num get(int step) {
        return Num.NaN();   // 多值指标不能直接取值，请取具体的线
    }
}
```

策略里注册它：

```java
ctx.registerIndicator(new DoubleEMAIndicator(c, Interval.M15));
```

打开模组运行时页面，指标框里会出现一项 **DEMA**，点"＋"后快慢两条均线一起叠加到主图。四个关键点：**① 基座打组**、**② 本体隐藏**、**③ 子线派生**、**④ 声明依赖**。平台内置的 MACD / BOLL / KD / PBX 都是按这个模式实现的（见 `northstar-api` 模块 `org.dromara.northstar.indicator` 包下的源码），可直接对照阅读。

## 常见问题

**指标框里没有我注册的指标？**
- 检查策略是否真的调用了 `ctx.registerIndicator(...)` 且模组已重新启用（策略在模组启用时加载）；
- 指标注册在哪个周期，就切到哪个周期看（如案例一的指标都在 15m）；
- 辅助线（`visible(false)`）本来就不显示。

**添加了指标，但图上看不到线？**
- 图上先有 K 线才有指标线。确认模组已启用、行情/回放正在产生数据、且合约选择正确（参见案例一的警告框）。

**同一个组的指标被画到了不同副图？**
- 确认访问的是新前端（dev 端口或重新打包后的静态资源），旧版本没有分组概念；
- 浏览器在平台升级期间一直开着的页面，请强刷（Ctrl+F5）后再试。

**副图加到 5 个以后加不上指标了？**
- 副图窗格上限为 5 个，用"－"移除不用的指标组，或"🗑"清空后重新添加。

**想让某条线以柱子/圆点显示？**
- 注册时给该线指定 `lineStyle`（`LINES` / `BARS` / `ZERO_BASED_BARS` / `DOTS`），例如 MACD 柱就是 `ZERO_BASED_BARS`（零轴之上红柱、之下绿柱）。

## 小结

- **一个 Indicator 对象维护一条指标线**；多线指标 = 多个对象的组合；
- **`group` 把多条线聚合成一个指标项，`placement` 决定主图还是副图**，在指标定义时一次说清，前端零配置；
- 前端把同组线堆叠进同一窗格，主图指标叠 K 线、副图指标依次占用空闲副图，选择结果按"模组+合约"本地记忆；
- 想深入：读 `IndicatorDemoStrategy`（用法）与 `org.dromara.northstar.indicator` 包（实现）。
