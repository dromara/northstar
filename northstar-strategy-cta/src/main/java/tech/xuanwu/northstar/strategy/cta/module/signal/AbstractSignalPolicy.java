package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public abstract class AbstractSignalPolicy implements SignalPolicy {
	
	/**
	 * 绑定合约
	 */
	protected String bindedUnifiedSymbol; 
	/**
	 * 回溯数据引用周期长度（默认至少有1个周期）
	 */
	protected int refDataLength = 1;

	protected Map<String, BarData> barDataMap = new HashMap<>();
	
	protected ModuleStatus moduleStatus;
	
	protected TickField currentTick;
	
	protected ContractManager contractManager;
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		if(StringUtils.isEmpty(bindedUnifiedSymbol)) {
			return Set.of();
		}
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public void setContractManager(ContractManager contractMgr) {
		contractManager = contractMgr;
	}

	@Override
	public Optional<Signal> onTick(TickField tick){
		if(bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			BarData barData = barDataMap.get(tick.getUnifiedSymbol());
			long timestamp = tick.getActionTimestamp();
			// 整分钟时不触发，避免onTick与onMin逻辑重复，导致重复触发
			int secondOfMin = (int) (timestamp % 60000);
			if(secondOfMin == 0) {
				LocalTime time = LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
				return onMin(time, barData);
			}
			return onTick(secondOfMin, barData);
		}
		return Optional.empty();
	}
	
	@Override
	public void updateTick(TickField tick) {
		// 更新行情
		if(bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			currentTick = tick;
			BarData barData = barDataMap.get(tick.getUnifiedSymbol());
			barData.update(tick);
		}
	}
	
	@Override
	public void updateBar(BarField bar) {
		// 更新行情
		if(bindedUnifiedSymbols().contains(bar.getUnifiedSymbol())) {
			BarData barData = barDataMap.get(bar.getUnifiedSymbol());
			barData.update(bar);
		}
	}
	
	@Override
	public BarData getRefBarData(String unifiedSymbol) {
		if(!barDataMap.containsKey(unifiedSymbol)) {
			throw new IllegalStateException("没有找到[" + unifiedSymbol + "]相应的引用数据");
		}
		return barDataMap.get(unifiedSymbol);
	}
	
	@Override
	public int getBarDataMaxRefLength() {
		return refDataLength;
	}
	
	@Override
	public void setBarData(BarData barData) {
		barDataMap.put(barData.getUnifiedSymbol(), barData);
	}
	
	/**
	 * 每TICK刷新用来驱动逻辑计算，所有的数据均已刷新在barDataMap中
	 */
	protected Optional<Signal> onTick(int milliSecOfMin, BarData barData){
		return Optional.empty();
	}
	
	/**
	 * 每分钟刷新用来驱动逻辑计算，所有的数据均已刷新在barDataMap中
	 */
	protected Optional<Signal> onMin(LocalTime time, BarData barData){
		return Optional.empty();
	}

	@Override
	public void setModuleStatus(ModuleStatus status) {
		this.moduleStatus = status;
	}
	
	protected Signal genSignal(SignalOperation signalOperation, double price, double stopPrice) {
		return CtaSignal.builder()
			.id(UUID.randomUUID())
			.signalClass(this.getClass())
			.signalPrice(price)
			.state(signalOperation)
			.stopPrice(stopPrice)
			.sourceUnifiedSymbol(bindedUnifiedSymbol)
			.timestamp(System.currentTimeMillis())
			.build();
	}
	
	protected Signal genSignal(SignalOperation signalOperation, double price) {
		return genSignal(signalOperation, price, 0);
	}
}
