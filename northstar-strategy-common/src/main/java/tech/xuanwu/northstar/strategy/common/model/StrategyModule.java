package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class StrategyModule {
	
	protected ModulePosition mPosition;
	
	protected ModuleTrade mTrade;
	
	protected SignalPolicy signalPolicy;
	
	protected RiskController riskController;
	
	protected Dealer dealer;
	
	protected ModuleAgent agent;
	
	private ModuleEventBus moduleEventBus = new ModuleEventBus();
	
	/**
	 * 用于记录模组关联的合约名称
	 */
	protected Set<String> symbolSet = new HashSet<>();
	
	public StrategyModule(ModuleAgent agent, SignalPolicy signalPolicy, RiskController riskController, Dealer dealer, 
			ModulePosition mPosition, ModuleTrade mTrade) {
		this.agent = agent;
		this.mPosition = mPosition;
		this.mTrade = mTrade;
		this.signalPolicy = signalPolicy;
		this.riskController = riskController;
		this.dealer = dealer;
		symbolSet.addAll(dealer.bindedUnifiedSymbols());
		symbolSet.addAll(signalPolicy.bindedUnifiedSymbols());
		
		moduleEventBus.register(signalPolicy);
		moduleEventBus.register(riskController);
		moduleEventBus.register(dealer);
		moduleEventBus.register(agent);
		
		riskController.setModuleAgent(agent);
		riskController.setEventBus(moduleEventBus);
		dealer.setModuleAgent(agent);
		dealer.setEventBus(moduleEventBus);
		signalPolicy.setModuleAgent(agent);
		signalPolicy.setEventBus(moduleEventBus);
		
		
	}
	
	public StrategyModule onTick(TickField tick) {
		if(!agent.isEnabled()) {
			//停用期间忽略TICK数据更新
			return this;
		}
		if(signalPolicy.bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {			
			agent.updateTradingDay(tick.getTradingDay());
			signalPolicy.updateTick(tick);
		}
		if(dealer.bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			dealer.onTick(tick);
			riskController.onTick(tick);
		}
		return this;
	}
	
	public StrategyModule onBar(BarField bar) {
		if(!agent.isEnabled()) {
			//停用期间忽略TICK数据更新
			return this;
		}
		signalPolicy.updateBar(bar);
		return this;
	}
	
	public StrategyModule onOrder(OrderField order) {
		if(agent.hasOrderRecord(order.getOriginOrderId())) {
			agent.onOrder(order);
		}
		return this;
	}
	
	public StrategyModule onTrade(TradeField trade) {
		if(agent.hasOrderRecord(trade.getOrderId())) {
			mTrade.updateTrade(trade);
			mPosition.onTrade(trade);
			agent.onTrade(trade);
		}
		return this;
	}
	
	public StrategyModule onAccount(AccountField account) {
		if(StringUtils.equals(account.getGatewayId(), agent.getAccountGatewayId())) {
			agent.updateAccount(account);
		}
		return this;
	}
	
	public String getName() {
		return agent.getName();
	}
	
	public ModuleState getState() {
		return agent.getModuleState();
	}
	
	public boolean isEnabled() {
		return agent.isEnabled();
	}
	
	public void toggleRunningState() {
		agent.toggleRunningState();
	}
	
	public ModuleStatus getModuleStatus() {
		ModuleStatus status = new ModuleStatus();
		status.setModuleName(agent.getName());
		status.setState(agent.getModuleState());
		List<TradeField> trades = mPosition.getOpenningTrade();
		if(trades.size() > 0) {
			status.setLastOpenTrade(trades.stream().map(trade -> trade.toByteArray()).collect(Collectors.toList()));
		}
		return status;
	}
	
	public ModulePerformance getPerformance() {
		ModulePerformance mp = new ModulePerformance();
		mp.setModuleName(agent.getName());
		Map<String, List<byte[]>> byteMap = new HashMap<>();
		for(String unifiedSymbol : signalPolicy.bindedUnifiedSymbols()) {
			byteMap.put(unifiedSymbol, 
				signalPolicy.getRefBarData(unifiedSymbol)
					.getRefBarList()
					.stream()
					.map(bar -> bar.toByteArray())
					.collect(Collectors.toList()));
		}
		mp.setRefBarDataMap(byteMap);
		mp.setAccountId(agent.getAccountGatewayId());
		mp.setAccountBalance(agent.getAccountBalance());
		mp.setModuleState(agent.getModuleState());
		mp.setTotalPositionProfit(mPosition.getPositionProfit());
		mp.setTotalCloseProfit(mTrade.getTotalCloseProfit());
		mp.setDealRecords(mTrade.getDealRecords());
		return mp;
	}

}
