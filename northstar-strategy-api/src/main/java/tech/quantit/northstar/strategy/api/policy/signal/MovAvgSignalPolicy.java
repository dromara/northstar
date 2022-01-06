package tech.quantit.northstar.strategy.api.policy.signal;

import java.util.Map;

import com.google.common.eventbus.EventBus;

import tech.quantit.northstar.strategy.api.AbstractSignalPolicy;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.indicator.ExpMovingAverage;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.Signal;
import tech.quantit.northstar.strategy.api.model.TimeSeriesValue;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 本示例用于展示一个带指标的策略要怎么写
 * 注意：依赖指标计算的策略需要先确保指标计算的准确性，过分复杂的指标并不建议自行实现，因为这个不是本程序的优势所在。本程序的优势在于高频交易、套利交易，而非指标交易。
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent("均线示例信号策略")	
public class MovAvgSignalPolicy extends AbstractSignalPolicy
	implements SignalPolicy{
	
	private Indicator maFast;
	private Indicator maSlow;
	
	private double entryPrice;

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		this.numOfRefData = initParams.numOfRefData;
		this.periodMins = initParams.periodMins;
		maFast = new ExpMovingAverage(bindedUnifiedSymbol, initParams.fastline, ValueType.CLOSE);
		maSlow = new ExpMovingAverage(bindedUnifiedSymbol, initParams.slowline, ValueType.CLOSE);
	}
	
	@Override
	public void setEventBus(EventBus moduleEventBus) {
		super.setEventBus(moduleEventBus);
		// 指标都要注册模组的事件总线，以便自动订阅行情更新
		moduleEventBus.register(maFast);	
		moduleEventBus.register(maSlow);
	}

	public static class InitParams extends DynamicParams{
		
		@Setting(value="绑定合约", order=10)	// Label注解用于定义属性的元信息
		private String bindedUnifiedSymbol;		// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
		@Setting(value="周期时长", order=11, unit="分钟")
		private int periodMins;
		
		@Setting(value="回溯周期数", order=12)
		private int numOfRefData;
		
		@Setting(value="快均线周期", order=20)
		private int fastline;
		
		@Setting(value="慢均线周期", order=21)
		private int slowline;
	}

	@Override
	public String name() {
		return "均线示例信号策略";
	}

	@Override
	protected void handleTick(TickField tick) {
		// 挣20个TICK就离场
		if(currentState == ModuleState.HOLDING_LONG && tick.getLastPrice() - entryPrice > 20 * bindedContract.getPriceTick()) {
			emit(Signal.builder().signalOperation(SignalOperation.SELL_CLOSE).build());
		}
		
		if(currentState == ModuleState.HOLDING_SHORT && entryPrice - tick.getLastPrice() > 20 * bindedContract.getPriceTick()) {
			emit(Signal.builder().signalOperation(SignalOperation.BUY_CLOSE).build());
		}
	}

	@Override
	protected void handleBar(BarField bar) {
		log.debug("周期响应：{}", bar.getActionTime());
		log.debug("当前Bar: {} {}", bar.getUnifiedSymbol(), bar.getClosePrice());
		log.debug("当前指标：快线 [{} -> {}]，慢线 [{} -> {}]", maFast.value(1), maFast.value(0), maSlow.value(1), maSlow.value(0));
		// 快线上穿慢线，入场做多
		if(currentState == ModuleState.EMPTY && maFast.value(1) < maSlow.value(1) && maFast.value(0) > maSlow.value(0)) {
			log.debug("上一周期的指标：快线 [{}]，慢线 [{}]", maFast.value(1), maSlow.value(1));
			entryPrice = bar.getClosePrice();
			emit(Signal.builder().signalOperation(SignalOperation.BUY_OPEN).signalPrice(entryPrice).ticksToStop(5).build());
		}
		
		// 快线下穿慢线，入场做空
		if(currentState == ModuleState.EMPTY && maFast.value(1) > maSlow.value(1) && maFast.value(0) < maSlow.value(0)) {
			log.debug("上一周期的指标：快线 [{}]，慢线 [{}]", maFast.value(1), maSlow.value(1));
			entryPrice = bar.getClosePrice();
			emit(Signal.builder().signalOperation(SignalOperation.SELL_OPEN).signalPrice(entryPrice).ticksToStop(5).build());
		}
	}

	@Override
	public Map<String, TimeSeriesValue[]> inspectRefData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initByTick(Iterable<TickField> ticks) {
		// 不用处理
	}

	@Override
	public void initByBar(Iterable<BarField> bars) {
		for(BarField bar : bars) {
			maFast.onBar(bar);
			maSlow.onBar(bar);
		}
	}

}
