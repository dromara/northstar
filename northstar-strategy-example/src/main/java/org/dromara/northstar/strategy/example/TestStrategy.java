package org.dromara.northstar.strategy.example;

import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.LineStyle;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ModuleIndicatorDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.RingArray;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.indicator.trend.MACDIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;

/**
 * 本策略仅用于系统测试
 * 除基础的交易信号外，还提供两类指标用于验证前端图表在不同周期间切换时的指标缓存恢复逻辑：
 * 1. 周期特有指标：M5 周期的 EMA5_5m 与 M15 周期的 EMA10_15m / EMA20_15m / MACD_15m，
 *    用于验证“不同名指标只需添加一次，切回对应周期时自动恢复显示”；
 * 2. 全周期同名指标：customIndicators() 提供的【全周期曲线】（timeFrame 为 null，所有周期可见且同名），
 *    用于验证“同名指标在切换周期时无需重复添加”。
 *
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent(TestStrategy.NAME)
public class TestStrategy extends AbstractStrategy	// 为了简化代码，引入一个通用的基础抽象类
		implements TradeStrategy{

	protected static final String NAME = "测试策略";

	private Indicator fastLine;

	private Indicator slowLine;

	private Indicator macdDiff;

	private Indicator macdDea;
	
	private Indicator m5Line;					// M5周期指标，用于验证周期特有指标的缓存恢复
	
	private RingArray<Bar> barBuff;		// 缓存M1周期K线，用于生成全周期可见的自定义指标
	
	private Logger logger;
	
	private TradeHelper helper;
	
	@Override
	public void onMergedBar(Bar bar) {
		if(bar.unstable()) {
			return;
		}
		logger.info("{} K线数据：时 [{}] 级别 [{}] 开 [{}], 高 [{}], 低 [{}], 收 [{}]",
				bar.contract().unifiedSymbol(), CommonUtils.millsToLocalDateTime(bar.actionTimestamp()), bar.interval(),
				bar.openPrice(), bar.highPrice(), bar.lowPrice(), bar.closePrice());
		// 缓存M1周期K线，用于生成全周期可见的自定义指标
		if(bar.interval() == Interval.M1) {
			barBuff.update(bar, bar.unstable());
		}
		// 确保指标已经准备好再开始交易
		if(!fastLine.isReady() || !slowLine.isReady()) {
			logger.debug("指标未准备就绪");
			return;
		}
		switch (ctx.getState()) {
			case EMPTY -> {
				// 快线在慢线之上开多，快线在慢线之下开空
				if(shouldBuy()) {
					helper.buyOpen(1);
					logger.info("多开");
				}
				if(shouldSell()) {
					helper.sellOpen(1);
					logger.info("空开");
				}

			}
			case HOLDING_LONG -> {
				if(fastLine.value(0) < slowLine.value(0)) {
					helper.sellClose(1);
					logger.info("平多");
				}
			}
			case HOLDING_SHORT -> {
				if(fastLine.value(0) > slowLine.value(0)) {
					helper.buyClose(1);
					logger.info("平空");
				}
			}
			default -> { /* 其他情况不处理 */}
		}
	}

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
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		// 忽略参数
	}

	@Override
	protected void initIndicators() {
		logger = ctx.getLogger(getClass());
		Contract c = ctx.bindedContracts().getFirst();
		// 指标的创建
		this.fastLine = new EMAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("EMA10")
				.interval(Interval.M15).build(), 10);
		this.slowLine = new EMAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("EMA20")
				.interval(Interval.M15).build(), 20);
		MACDIndicator macd = new MACDIndicator(c, Interval.M15, 12, 26, 9);
		this.macdDiff = macd.getDiffLine();
		this.macdDea = macd.getDeaLine();
		// M5周期指标，使M5周期图表也有周期特有指标可供验证
		this.m5Line = new EMAIndicator(Configuration.builder()
				.contract(c)
				.indicatorName("EMA5")
				.interval(Interval.M5).build(), 5);

		// 指标的注册
		ctx.registerIndicator(fastLine);
		ctx.registerIndicator(slowLine);
		ctx.registerIndicator(macd);
		ctx.registerIndicator(m5Line);

		barBuff = new RingArray<>(1000);
		helper = new TradeHelper(ctx, c);
	}

	/**
	 * 提供一个在所有周期下同名可见的自定义指标（timeFrame 为 null 表示所有周期可见），
	 * 用于验证前端在不同周期间切换时，同名指标无需重复添加即可自动显示。
	 * 指标值取M1周期K线的典型价（最高价 + 最低价 + 收盘价）/ 3，由后端按各周期自动归集。
	 */
	@Override
	public List<ModuleIndicatorDescription> customIndicators() {
		if(barBuff == null) {
			return List.of();
		}
		Contract c = ctx.bindedContracts().getFirst();
		List<TimeSeriesValue> values = barBuff.toList().stream()
				.map(bar -> new TimeSeriesValue((bar.highPrice() + bar.lowPrice() + bar.closePrice()) / 3, bar.actionTimestamp()))
				.toList();
		return List.of(ModuleIndicatorDescription.builder()
				.contract(ContractSimpleInfo.from(c))
				.name("全周期曲线")
				.style(LineStyle.LINES)
				.values(values)
				.build());
	}

	public static class InitParams extends DynamicParams {}

	@Override
	public String name() {
		return NAME;
	}

}
