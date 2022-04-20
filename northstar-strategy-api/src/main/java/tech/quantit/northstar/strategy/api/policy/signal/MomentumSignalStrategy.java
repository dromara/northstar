package tech.quantit.northstar.strategy.api.policy.signal;

import java.time.LocalTime;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.AbstractSignalPolicy;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.Signal;
import tech.quantit.northstar.strategy.api.utils.collection.RingArray;
import tech.quantit.northstar.strategy.api.utils.time.GetTime;
import tech.quantit.northstar.strategy.api.utils.time.TimeRange;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;


/**
 * 概述：计算最近两小时内的TICK成交量波动值，通过成交量的波动偏差来捕获放量与缩量，从而抓住入场与离场机会
 * 适用范围：全市场合约
 * 规则说明：
 * 	- 通过计算最近120个1分钟周期内，五秒内TICK的成交量，来捕获放量与缩量事件
 * 	- 以上计算数据需要排除刚开盘的前15分钟数据，以免开盘的放量事件影响统计；（开盘15分钟的计算放在其他策略做，本策略在这15分钟窗口内只确保止损）
 * 	- 当放量事件触发时，用当时价减去上一个K线的收盘价，以求得多空方向；然后用市价入场
 * 	- 当入场后，一路持仓到缩量事件出现
 * 	- 当入场后，未出现缩量，触发止损价离场
 * 	- 缩量事件出现时，离场
 * 参数说明：
 * 	- 日夜盘	用来确认要排除的开盘前15分钟是在哪个时间段
 * 策略验证：
 * 	- 关键指标计算是否准确
 * 	- 放量缩量位置是否与期望位置一致
 * 	- 交易频率与期望是否一致
 * 	- 
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("动量示例策略")
public class MomentumSignalStrategy extends AbstractSignalPolicy implements SignalPolicy {
	
	private boolean isNightContract;			// 是否包含夜盘的合约
	
	private RingArray<Long> tickVolOfLast5Secs;		// 最近5秒内的TICK成交量原始数据
	
	private double sumVolOfLast5Secs;				// 5秒TICK总量
	
	private RingArray<BarField> barsOfLast120Mins;	// 过去的120个1分钟K线
	
	private double meanVolOfEvery5SecsIn120Mins;	// 120个分钟周期的每5秒成交量均值
	
	private static final int FREEZE_PERIOD = 1000;
	
	private boolean isVolWaving;		// 成交量放量标志
	
	private int leastNumOfDataForInit;	// 初始化所需的最少数据量
	
	private int countOfInitData;		// 初始化的数据量计数
	
	private double triggeredVolBaseLine;
	
	private int numOfVolAtBid;
	
	private int numOfVolAtAsk;
	
	@Override
	protected void handleBar(BarField bar) {
		if(!hasDoneInit()) {
			initByBar(bar);
			return;
		}
		barsOfLast120Mins.update(bar);
		// 每分钟计算一次均量
		numOfVolAtAsk = 0;
		numOfVolAtBid = 0;
		meanVolOfEvery5SecsIn120Mins = Stream.of(barsOfLast120Mins.toArray())
				.map(BarField.class::cast)
				.mapToDouble(i -> i == null ? 0D : i.getVolumeDelta())
				.sum() / (120 * 12);
		log.trace("[{} {}] 近120分钟的每5秒成交量均值为 [{}]", bar.getActionDay(), bar.getActionTime(), meanVolOfEvery5SecsIn120Mins);
		
	}

	@Override
	protected void handleTick(TickField tick) {
		lastTick = tick;
		if(!hasDoneInit()) {
			initByTick(tick);
			return;
		}
		if(tick.getLastPrice() == tick.getAskPrice(0)) {
			numOfVolAtAsk += tick.getVolumeDelta();
		}
		if(tick.getLastPrice() == tick.getBidPrice(0)) {
			numOfVolAtBid += tick.getVolumeDelta();
		}
		updateTick(tick, false);
	}
	
	@Override
	public void initByBar(BarField bar) {
		countOfInitData++;
		barsOfLast120Mins.update(bar);
		if(hasDoneInit()) {
			meanVolOfEvery5SecsIn120Mins = Stream.of(barsOfLast120Mins.toArray())
					.map(BarField.class::cast)
					.mapToDouble(i -> i == null ? 0D : i.getVolumeDelta())
					.sum() / (120 * 12);
		}
	}

	@Override
	public void initByTick(TickField tick) {
		updateTick(tick, true);
	}

	private final TimeRange dayOpenPeriod = new TimeRange(LocalTime.of(8, 59), LocalTime.of(9, 15));
	private final TimeRange nightOpenPeriod = new TimeRange(LocalTime.of(20, 59), LocalTime.of(21, 15));
	
	protected void updateTick(TickField tick, boolean isInitializing) {
		LocalTime timeNow = GetTime.from(tick);
		if(!isNightContract && dayOpenPeriod.isWithinPeriod(timeNow) || isNightContract && nightOpenPeriod.isWithinPeriod(timeNow)) {
			log.trace("跳过开盘期间的TICK处理: [{} {} {}]", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime());
			return;
		}
		// 更新TICK成交量样本集
		long newVal = tick.getVolumeDelta();
		long oldVal = tickVolOfLast5Secs.update(newVal).orElse(0L);
		// 更新5秒成交均量
		sumVolOfLast5Secs += (newVal - oldVal);
		
		if(!isInitializing) {
			log.trace("[{} {}] 价格：[{}], 成交量：[{}] 近5秒成交量：[{}]", tick.getActionDay(), tick.getActionTime(),
					tick.getLastPrice(), tick.getVolumeDelta(), sumVolOfLast5Secs);
			if(!isVolWaving && significantlyWavingInMeanVol() && !isFreezing(tick) && barsOfLast120Mins.get() != null) {
				triggeredVolBaseLine = meanVolOfEvery5SecsIn120Mins;
				onVolWave(tick);
			} else if(isVolShrink()) {
				onVolShrink(tick);
			}
		}
	}
	
	private boolean isFreezing(TickField tick) {
		return tick.getActionTimestamp() - lastActionTime < FREEZE_PERIOD;
	}
	
	private boolean significantlyWavingInMeanVol() {
		return hasDoneInit() && sumVolOfLast5Secs > 10 * meanVolOfEvery5SecsIn120Mins;
	}
	
	private boolean isVolShrink() {
		return hasDoneInit() && isVolWaving && sumVolOfLast5Secs < 2 * triggeredVolBaseLine;
	}
	
	/*
	 * 放量时，即时开仓
	 * @param tick
	 */
	protected void onVolWave(TickField tick) {
		log.debug("[{} {}] 检测到放量：近120分钟5秒均量 [{}], 近5秒成交量 [{}], {}倍于均值", tick.getActionDay(), tick.getActionTime(), 
				meanVolOfEvery5SecsIn120Mins, sumVolOfLast5Secs, sumVolOfLast5Secs / meanVolOfEvery5SecsIn120Mins);
		isVolWaving = true;
		if(currentState == ModuleState.EMPTY) {
			SignalOperation op = null;
			if(tick.getLastPrice() > barsOfLast120Mins.get().getClosePrice()) {
				op = SignalOperation.BUY_OPEN;
			} else if (tick.getLastPrice() < barsOfLast120Mins.get().getClosePrice()) {
				op = SignalOperation.SELL_OPEN;
			} else {
				op = numOfVolAtAsk > numOfVolAtBid ? SignalOperation.BUY_OPEN : SignalOperation.SELL_OPEN;
			}
			int tickToStop = (int) (Math.abs(tick.getLastPrice() - barsOfLast120Mins.get().getClosePrice()) / bindedContract.getPriceTick());
			emit(Signal.builder().signalOperation(op).ticksToStop(tickToStop).build(), tick.getActionTimestamp());
		}
	}
	
	/*
	 * 缩量时，记下当前价位， 
	 * @param tick
	 */
	protected void onVolShrink(TickField tick) {
		log.debug("[{} {}] 检测到缩量：近120分钟5秒均量 [{}], 近5秒成交量 [{}], {}倍于均值", tick.getActionDay(), tick.getActionTime(),
				meanVolOfEvery5SecsIn120Mins, sumVolOfLast5Secs, sumVolOfLast5Secs / meanVolOfEvery5SecsIn120Mins);
		isVolWaving = false;
		if(currentState == ModuleState.HOLDING_LONG) {
			log.debug("多头离场");
			emit(Signal.builder().signalOperation(SignalOperation.SELL_CLOSE).signalPrice(tick.getAskPrice(0)).build(), tick.getActionTimestamp());
		} else if(currentState == ModuleState.HOLDING_SHORT) {
			log.debug("空头离场");
			emit(Signal.builder().signalOperation(SignalOperation.BUY_CLOSE).signalPrice(tick.getBidPrice(0)).build(), tick.getActionTimestamp());
		}
	}
	
	
	@Override
	public void setBindedContract(ContractField contract) {
		super.setBindedContract(contract);
		int numOfTicksPerSec = 2;
		if(contract.getExchange() == ExchangeEnum.CZCE) {
			numOfTicksPerSec = 4;
		}
		tickVolOfLast5Secs = new RingArray<>(5 * numOfTicksPerSec);
		barsOfLast120Mins = new RingArray<>(120);
	}

	@Override
	public String name() {
		return "动量示例策略";
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	/**
	 * 策略的所有参数初始化逻辑
	 */
	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		this.isNightContract = initParams.contractType.equals("日夜盘");
		this.numOfRefData = 120;	//往前回溯120个周期
		this.periodMins = 1;	//关注1分钟周期K线
		this.leastNumOfDataForInit = 120;
	}
	
	/**
	 * 定义该策略的参数，类名必须为InitParams，必须继承DynamicParams，必须是个static类
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams{
		
		@Setting(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		private String bindedUnifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Setting(value="合约类型", order=20,  options = {"日盘","日夜盘"})	// 可以声明单位
		private String contractType;
		
	}

	@Override
	public boolean hasDoneInit() {
		return countOfInitData >= leastNumOfDataForInit;
	}

}
