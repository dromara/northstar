# 如何编写Northstar的策略代码

首先要了解策略层代码可用的接口：

```java
public interface IModuleStrategyContext {
	/**
	 * 委托下单（根据配置自动处理撤单追单）
	 * @param tradeIntent		交易意图
	 */
	void submitOrderReq(TradeIntent tradeIntent);
	/**
	 * 委托下单（精简接口）
	 * @param contract			交易合约			
	 * @param operation			操作信号
	 * @param priceType			价格类型
	 * @param volume			手数
	 * @param price				委托价（市价为0）
	 * @return	originOrderId	订单ID
	 */
	Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, int volume, double price);
	/**
	 * 判断订单是否已经超时
	 * 该方法用于撤单场景
	 * @param originOrderId		订单ID
	 * @param timeout			超时毫秒数
	 * @return
	 */
	boolean isOrderWaitTimeout(String originOrderId, long timeout);
	/**
	 * 撤单
	 * @param originOrderId		订单ID
	 */
	void cancelOrder(String originOrderId);
	/**
	 * 获取合约绑定的物理账户对象
	 * @param contract
	 * @return
	 */
	IAccount getAccount(Contract contract);
	/**
	 * 获取模组账户
	 * @param contract
	 * @return
	 */
	IModuleAccount getModuleAccount();
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleState getState();
	/**
	 * 获取合约
	 * @param unifiedSymbol		合约编码
	 * @return					返回合约信息
	 */
	Contract getContract(String unifiedSymbol);
	/**
	 * 获取模组绑定的全部合约
	 * @return
	 */
	List<Contract> bindedContracts();
	/**
	 * 获取日志对象
	 * @return
	 */
	Logger getLogger(Class<?> clz);
	/**
	 * 获取模组周期设置
	 * @return
	 */
	int numOfMinPerMergedBar();
	/**
	 * 获取默认交易手数
	 * @return
	 */
	int getDefaultVolume();
	/**
	 * 停用模组策略
	 * @param enabled
	 */
	void disabledModule();
	/**
	 * 设置自定义的风控策略
	 * @param filter
	 */
	void setOrderRequestFilter(OrderRequestFilter filter);
	/**
	 * 注册指标
	 * @param indicator
	 */
	void registerIndicator(Indicator indicator);
	/**
	 * 注册周期监听器
	 * @param listener
	 */
	void addMergedBarListener(Contract contract, int numOfUnit, PeriodUnit unit, MergedBarListener listener);
}
```

其次，Northstar框架本身提供了一个通用的抽象类，所有的策略代码都应该继承这个抽象类去扩展：

```java
public abstract class AbstractStrategy implements TradeStrategy{
	
	// 模组计算状态
	@Getter
	@Setter
	protected JSONObject storeObject;
	// 模组上下文
	protected IModuleStrategyContext ctx;
	// 处理器，contract -> handler
	protected Map<Contract, TickHandler> tickHandlerMap = new HashMap<>();
	// 处理器，contract -> handler
	protected Map<Contract, BarHandler> barHandlerMap = new HashMap<>();
	// 日志对象
	@Setter
	protected Logger logger;
	// 预热K线数据量（该预热数据量与模组的设置并不相等，该属性用于策略内部判断接收了多少数据，而模组的预热设置用于外部投喂了多少数据）
	protected int numOfBarsToPrepare;
	
	private Queue<Bar> barCache = new LinkedList<>();
	
	@Override
	public void onOrder(Order order) {
		// 如果策略不关心订单反馈，可以不重写
	}

	@Override
	public void onTrade(Trade trade) {
		// 如果策略不关心成交反馈，可以不重写
		if(logger.isInfoEnabled()) {
			logger.info("模组成交 [{} {} {} 操作：{}{} {}手 {}]", trade.contract().unifiedSymbol(),
					trade.tradeDate(), trade.tradeTime(), FieldUtils.chn(trade.direction()), FieldUtils.chn(trade.offsetFlag()), 
					trade.volume(), trade.price());
			logger.info("当前模组净持仓：[{}]", ctx.getModuleAccount().getNonclosedNetPosition(trade.contract()));
			logger.info("当前模组状态：{}", ctx.getState());
		}
	}

	/* 该方法不应该被重写，但可以扩展 */
	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		logger = ctx.getLogger(getClass());
		initIndicators();
		initMultiContractHandler();
	}
	
	public IModuleContext getContext() {
		return (IModuleContext) ctx;
	}
	
	/**
	 * 是否启用
	 * @return
	 */
	public boolean isEnabled() {
		return getContext().isEnabled();
	}
	
	/**
	 * 指标初始化
	 */
	protected void initIndicators() {}
	
	/**
	 * 多合约处理器初始化
	 */
	protected void initMultiContractHandler() {}

	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个TICK触发一次
	 * 如果订阅了多个合约，则会有多个TICK，因此每个TICK时刻会触发多次
	 */
	@Override
	public void onTick(Tick tick) {
		if(!canProceed()) {
			return;
		}
		if(tickHandlerMap.containsKey(tick.contract())) {
			tickHandlerMap.get(tick.contract()).onTick(tick);
		}
	}
	
	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param contract
	 * @param handler
	 */
	protected void addTickHandler(Contract contract, TickHandler handler) {
		tickHandlerMap.put(contract, handler);
	}

	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个K线触发一次
	 * 如果订阅了多个合约，则会有多个K线，因此每个K线时刻会触发多次
	 */
	@Override
	public void onMergedBar(Bar bar) {
		if(!canProceed(bar)) {
			return;
		}
		if(barHandlerMap.containsKey(bar.contract())) {
			barHandlerMap.get(bar.contract()).onMergedBar(bar);
		}
	}
	
	protected boolean canProceed() {
		return barCache.isEmpty() && numOfBarsToPrepare == 0;
	}
	
	protected boolean canProceed(Bar bar) {
		if(barCache.size() < numOfBarsToPrepare) {
			if(barCache.isEmpty() || barCache.peek().contract().equals(bar.contract())) {
				barCache.offer(bar);
			}
			return false;
		}
		
		numOfBarsToPrepare = 0;
		barCache.clear();
		return true;
	}
	
	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param contract
	 * @param handler
	 */
	protected void addBarHandler(Contract contract, BarHandler handler) {
		barHandlerMap.put(contract, handler);
	}
	
	protected static interface TickHandler extends TickDataAware {}
	
	protected static interface BarHandler extends MergedBarListener {}
}
```

Northstar的策略框架是基于事件驱动的，事件来源是交易所。
以下是策略代码的基本框架：

```java
/**
 * 本示例用于展示一个带指标的策略
 * 采用的是简单的均线策略：快线在慢线之上做多，快线在慢线之下做空
 */
@StrategicComponent(IndicatorExampleStrategy.NAME)	// 每个策略都要有这个注解，才能被Northstar所加载
public class IndicatorExampleStrategy extends AbstractStrategy	implements TradeStrategy{

	protected static final String NAME = "示例-指标策略";
	private InitParams params;	// 策略的参数配置信息
	private Indicator fastLine;
	private Indicator slowLine;
	private Indicator macdDiff;
	private Indicator macdDea;
	private Logger logger;
	private TradeHelper helper;
	
	/* 合并Bar事件会在每个K线走完时触发此方法调用 */
	@Override
	public void onMergedBar(Bar bar) {
		logger.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]",
				bar.contract().unifiedSymbol(), bar.openPrice(), bar.highPrice(), bar.lowPrice(), bar.closePrice());
		// 确保指标已经准备好再开始交易
		if(!fastLine.isReady() || !slowLine.isReady()) {
			logger.debug("指标未准备就绪");
			return;
		}
		switch (ctx.getState()) {
			case EMPTY -> {
				// 快线在慢线之上开多，快线在慢线之下开空
				if(shouldBuy()) {
					helper.doBuyOpen(1);
					logger.info("多开");
				}
				if(shouldSell()) {
					helper.doSellOpen(1);
					logger.info("空开");
				}

			}
			case HOLDING_LONG -> {
				if(fastLine.value(0) < slowLine.value(0)) {
					helper.doSellClose(1);
					logger.info("平多");
				}
			}
			case HOLDING_SHORT -> {
				if(fastLine.value(0) > slowLine.value(0)) {
					helper.doBuyClose(1);
					logger.info("平空");
				}
			}
			default -> { /* 其他情况不处理 */}
		}
	}

	/* 交易所的TICK事件会触发此方法的调用，大概每秒2-3次 */
	@Override
	public void onTick(Tick tick) {
		logger.info("时间：{} {} 价格：{} 指标值：{}", tick.actionDay(), tick.actionTime(), tick.lastPrice(), fastLine.value(0));
	}

	private boolean shouldBuy() {
		return fastLine.value(0) > slowLine.value(0) && this.macdDiff.value(0) > this.macdDea.value(0);
	}

	private boolean shouldSell() {
		return fastLine.value(0) < slowLine.value(0) && this.macdDiff.value(0) < this.macdDea.value(0);
	}

	@Override
	protected void initIndicators() {
		logger = ctx.getLogger(getClass());	//每个策略都要实例化一个自己的logger以实现日志按模组隔离
		Contract c = ctx.getContract(params.indicatorSymbol);
		// 指标的创建
		this.fastLine = new EMAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("EMA10")
				.numOfUnits(ctx.numOfMinPerMergedBar()).build(), 10);
		this.slowLine = new EMAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("EMA20")
				.numOfUnits(ctx.numOfMinPerMergedBar()).build(), 20);
		MACDIndicator macd = new MACDIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("MACD")
				.numOfUnits(ctx.numOfMinPerMergedBar())
				.build(), 12, 26, 9);
		this.macdDiff = macd.getDiffLine();
		this.macdDea = macd.getDeaLine();

		// 指标的注册
		ctx.registerIndicator(fastLine);
		ctx.registerIndicator(slowLine);
		ctx.registerIndicator(macd);
		
		helper = TradeHelper.builder().context(getContext()).tradeContract(c).build();
	}
	
	/* 交易所的委托单状态变动会触发该方法调用 */
	@Override
	public void onOrder(Order order) { }

	/* 交易所的成交回报会触发该方法调用 */
	@Override
	public void onTrade(Trade trade) { }
	
	/* 定义策略参数 */
	public static class InitParams extends DynamicParams {			
		
		@Setting(label="指标合约", order=0)	// 每个属性都要加这个注解来定义前端使用哪种类型控件来输入这个参数
		private String indicatorSymbol;
		
		@Setting(label="快线周期", type = FieldType.NUMBER, order=1)		
		private int fast = 5;						
		
		@Setting(label="慢线周期", type = FieldType.NUMBER, order=2)		
		private int slow = 10;

	}

	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();	// 该方法用于生成一个配置对象
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;	// 该方法用于把配置信息放入策略对象
	}
	
	@Override
	public String name() {
		return NAME;	// 策略名称
	}

}
```

下单所用到的工具类TradeHelper的接口文档代码如下：

```md
# TradeHelper 接口文档

## 1. 类概述
`TradeHelper` 是一个交易助手类，旨在简化开平仓操作的代码实现。它封装了交易逻辑，提供了多种交易操作方法，支持限价和对手价交易，并允许用户自定义价格类型、超时时间以及价格差异条件。

## 2. 类成员变量
| 成员变量 | 类型 | 描述 |
|----------|------|------|
| `context` | `IModuleStrategyContext` | 模组上下文，用于与交易系统交互。 |
| `tradeContract` | `Contract` | 当前交易的合约信息。 |
| `defaultTimeout` | `long` | 默认的订单超时时间（单位：毫秒）。 |
| `logger` | `Logger` | 日志记录器，用于记录交易操作的日志信息。 |
| `priceType` | `PriceType` | 当前价格类型，默认为对手价（`PriceType.OPP_PRICE`）。 |

## 3. 构造方法
### 3.1 TradeHelper(IModuleStrategyContext context, Contract tradeContract)
### 3.2 TradeHelper(IModuleStrategyContext context, Contract tradeContract, long defaultTimeout)

## 4. 方法
### 4.1 设置价格类型
#### setPriceType(PriceType type)

### 4.2 获取可平仓数量
#### getCloseVolume(DirectionEnum holdingDirection)

### 4.3 计算两个 Tick 的价差率
#### static double spreadRateInPercentage(Tick numerator, Tick denominator, boolean buyNumerator)

### 4.4 交易操作
#### doBuyOpen(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort)
- **描述**：限价买开仓。

#### doBuyOpen(int vol)
- **描述**：对手价买开仓。

#### doSellOpen(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort)
- **描述**：限价卖开仓。

#### doSellOpen(int vol)
- **描述**：对手价卖开仓。

#### doBuyClose(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort)
- **描述**：限价买平仓。

#### doBuyClose(int vol)
- **描述**：对手价买平仓。

#### doSellClose(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort)
- **描述**：限价卖平仓。

#### doSellClose(int vol)
- **描述**：对手价卖平仓。

#### doBuyReverse(int vol)
- **描述**：反手买。

#### doSellReverse(int vol)
- **描述**：反手卖。

## 6. PriceType 枚举
`PriceType` 枚举定义了不同的价格类型及其行为。以下是支持的价格类型及其特点：

| 枚举值 | 描述 | 价格计算逻辑 |
|--------|------|--------------|
| `ANY_PRICE` | 市价 | 买入时使用涨停价，卖出时使用跌停价。 |
| `OPP_PRICE` | 对手价 | 买入时使用卖一价，卖出时使用买一价。 |
| `LAST_PRICE` | 最新价 | 使用最新的成交价。 |
| `WAITING_PRICE` | 排队价 | 买入时使用买一价，卖出时使用卖一价。 |
| `LIMIT_PRICE` | 限价 | 使用用户指定的价格。 |

```

编写策略时，通常会需要实现指标计算。Northstar有自己的指标框架，以下是实现一个MACD指标的示例：

```java
public class MACDIndicator extends AbstractIndicator implements Indicator{
  
  protected Indicator diffLine;
  protected Indicator deaLine;
​
  /**
   * 求默认的MACD值
   * @param cfg
   * @param shortBarCount
   * @param longBarCount
   * @param signalBarCount
   */
  public MACDIndicator(Configuration cfg, int shortBarCount, int longBarCount, int signalBarCount) {
    super(cfg);
    Assert.isTrue(shortBarCount < longBarCount, "长短周期设置不正确");
    Indicator shortTermLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_fast").visible(false).build(), shortBarCount);
    Indicator longTermLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_slow").visible(false).build(), longBarCount);
    
    diffLine = new DiffIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_diff").build(), shortTermLine, longTermLine);
    deaLine = new EMAIndicator(cfg.toBuilder().indicatorName(cfg.indicatorName() + "_dea").build(), diffLine, signalBarCount);
  }
  
  @Override
  public List<Indicator> dependencies() {
    // 该指标所依赖到的其他指标，需要通过这个数组暴露出去
    return List.of(deaLine);  
  }
  
  /**
   * 由于MACD属于多值指标，但可以把MACD红绿柱本身看作核心值，dea与diff看作辅助值，
   * 所以此处的核心运算是MACD红绿柱值的运算
   */
  @Override
  protected Num evaluate(Num num) {
    double val = (diffLine.value(0) - deaLine.value(0)) * 2;  //依赖的指标值会确保被优先更新，因此这里能直接使用
    return Num.of(val, num.timestamp(), num.unstable());
  }
​
}
```
现在我们定义了指标与指标值，在访问指标值时，用的是一般的数组集合访问方式：get(i)和value(i)，i 的取值范围为(-size,0]。0代表当前最新的指标值，-1则代表上一个K线的指标值，如此类推。

以下是Configuration的接口文档：

```md
# Configuration 接口文档

## 概述
`Configuration` 是一个用于配置指标的记录类，用于定义指标的显示名称、绑定合约、周期设置、值类型、指标线类型等属性。该类通过构造函数和默认值提供灵活的配置选项，并通过 `indicatorID` 方法生成唯一的指标标识符。

## 类定义
@Builder(toBuilder = true)
public record Configuration(
    String indicatorName,
    Contract contract,
    Integer numOfUnits,
    PeriodUnit period,
    ValueType valueType,
    LineStyle lineStyle,
    Integer cacheLength,
    Boolean ifPlotPerBar,
    Boolean visible
) {
    ...
}

## 属性
| 属性名称          | 类型          | 默认值       | 描述                                                                 |
|-------------------|---------------|--------------|----------------------------------------------------------------------|
| `indicatorName`   | `String`      | `""`         | 指标显示名称。如果未指定，则默认为空字符串。                           |
| `contract`        | `Contract`    | -            | 绑定的合约对象。如果未指定，则抛出 `IllegalArgumentException` 异常。 |
| `numOfUnits`      | `Integer`     | `1`          | 周期数量，默认为 1。                                                 |
| `period`          | `PeriodUnit`  | `PeriodUnit.MINUTE` | 周期单位，默认为分钟 (`MINUTE`)。                                      |
| `valueType`       | `ValueType`   | `ValueType.CLOSE` | 指标取值类型，默认为收盘价 (`CLOSE`)。                                 |
| `lineStyle`       | `LineStyle`   | `LineStyle.LINES` | 指标线类型，默认为折线 (`LINES`)。                                     |
| `cacheLength`     | `Integer`     | `16`         | 可回溯长度，默认为 16。                                               |
| `ifPlotPerBar`    | `Boolean`     | `false`      | 是否将跨周期指标映射到每根 K 线，默认为 `false`。                       |
| `visible`         | `Boolean`     | `true`       | 是否在模组图表中显示，默认为 `true`。                                   |

## 枚举类型

### `PeriodUnit`
周期单位枚举，定义了时间周期的类型及其符号。

#### 枚举值
| 枚举值    | 符号 | 描述       |
|-----------|------|------------|
| `MINUTE`  | `"m"` | 分钟       |
| `HOUR`    | `"hr"` | 小时       |
| `DAY`     | `"d"` | 天         |

### `ValueType`
指标取值类型枚举，定义了从 K 线数据中提取的值类型。

#### 析取值
| 析取值    | 描述                                                                 | 方法返回值                                                                 |
|-----------|----------------------------------------------------------------------|----------------------------------------------------------------------------|
| `HIGH`    | 最高价                                                               | `bar.highPrice()`                                                          |
| `LOW`     | 最低价                                                               | `bar.lowPrice()`                                                           |
| `OPEN`    | 开盘价                                                               | `bar.openPrice()`                                                          |
| `CLOSE`   | 收盘价                                                               | `bar.closePrice()`                                                         |
| `BARYCENTER` | 重心价，计算公式为 `(high + low + 2 * close) / 4`                     | `(bar.highPrice() + bar.lowPrice() + bar.closePrice() * 2) / 4`            |
| `VOL`     | 当日累计成交量                                                       | `bar.volume()`                                                             |
| `VOL_DELTA` | K 线成交量差值                                                     | `bar.volumeDelta()`                                                        |
| `OI`      | 实际持仓量                                                           | `bar.openInterest()`                                                       |
| `OI_DELTA` | 持仓量变化                                                           | `bar.openInterestDelta()`                                                  |
| `TRADE_DATE` | 交易日，返回格式化后的交易日期（整数格式）                           | `Double.parseDouble(bar.tradingDay().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))` |

### `LineStyle`
指标图例类型枚举，定义了指标的显示样式。

#### 析取值
| 析取值    | 描述       |
|-----------|------------|
| `LINES`   | 折线       |
| `BARS`    | 柱线       |
| `DOTS`    | 点状线     |
```