package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.gateway.api.Gateway;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.ModuleAccount;
import tech.xuanwu.northstar.strategy.common.ModuleOrder;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class StrategyModule {
	/**
	 * unifiedSymbol -> barData
	 */
	private Map<String,BarData> barDataMap;
	
	private ModuleAccount mAccount;
	
	private ModulePosition mPosition;
	
	private ModuleOrder mOrder;
	
	private ModuleTrade mTrade;
	
	private SignalPolicy signalPolicy;
	
	private List<RiskControlRule> riskControlRules;
	
	private Dealer dealer;
	
	private ModuleState state;
	
	private Gateway gateway;
	
	private String name;
	
	private String accountGatewayId;
	
	private boolean enabled;
	
	/**
	 * 用于记录模组关联的合约名称
	 */
	private Set<String> symbolSet = new HashSet<>();
	/**
	 * 用于记录模组发出的订单
	 */
	private Set<String> orderIdSet = new HashSet<>();
	
	public StrategyModule(String name, String accountGatewayId, Map<String, BarData> barDataMap, ModuleAccount mAccount, ModulePosition mPosition,
			ModuleOrder mOrder, ModuleTrade mTrade, SignalPolicy signalPolicy, List<RiskControlRule> riskControlRules,
			Dealer dealer, ModuleState state, Gateway gateway, boolean enabled) {
		this.barDataMap = barDataMap;
		this.mAccount = mAccount;
		this.mPosition = mPosition;
		this.mOrder = mOrder;
		this.mTrade = mTrade;
		this.signalPolicy = signalPolicy;
		this.riskControlRules = riskControlRules;
		this.dealer = dealer;
		this.state = state;
		this.gateway = gateway;
		this.name = name;
		this.accountGatewayId = accountGatewayId;
		this.enabled = enabled;
		symbolSet.addAll(dealer.bindedUnifiedSymbols());
		symbolSet.addAll(signalPolicy.bindedUnifiedSymbols());
	}
	
	public void onTick(TickField tick) {
		if(!symbolSet.contains(tick.getUnifiedSymbol())) {
			return;
		}
		mPosition.onTick(tick);
		barDataMap.get(tick.getUnifiedSymbol()).update(tick);
		if(!enabled) {
			return;
		}
		Optional<Signal> signalOpt = signalPolicy.updateTick(tick, barDataMap);
		if(signalOpt.isPresent()) {
			//TODO 未完成
		}
		dealer.onTick(tick, riskControlRules, gateway);
	}
	
	public void onBar(BarField bar) {
		if(!symbolSet.contains(bar.getUnifiedSymbol())) {
			return;
		}
		barDataMap.get(bar.getUnifiedSymbol()).update(bar);
	}
	
	public void onOrder(OrderField order) {
		if(!orderIdSet.contains(order.getOrderId())) {
			return;
		}
		mOrder.updateOrder(order);
		dealer.onOrder(order);
	}
	
	public void onTrade(TradeField trade) {
		if(!orderIdSet.contains(trade.getOrderId())) {
			return;
		}
		mTrade.updateTrade(trade);
		mPosition.onTrade(trade);
		dealer.onTrade(trade);
	}
	
	public void onAccount(AccountField account) {
		if(!StringUtils.equals(account.getGatewayId(), accountGatewayId)) {
			return;
		}
		mAccount.updateAccount(account);
		dealer.onAccount(account);
	}
	
	public String getName() {
		return name;
	}
	
	public ModuleState getState() {
		return state;
	}
	
	public void toggleRunningState() {
		enabled = !enabled;
	}
	
	public ModuleStatus getModuleStatus() {
		ModuleStatus status = new ModuleStatus();
		status.setModuleName(name);
		status.setState(state);
		List<TradeField> trades = mPosition.getOpenningTrade();
		if(trades.size() > 0) {
			status.setLastOpenTrade(trades.stream().map(trade -> trade.toByteArray()).collect(Collectors.toList()));
		}
		return status;
	}
	
	public ModulePerformance getPerformance() {
		ModulePerformance mp = new ModulePerformance();
		mp.setModuleName(name);
		Map<String, List<byte[]>> byteMap = new HashMap<>();
		for(Entry<String, BarData> e : barDataMap.entrySet()) {
			byteMap.put(e.getKey(), e.getValue().getRefBarList().stream().map(bar -> bar.toByteArray()).collect(Collectors.toList()));
		}
		mp.setRefBarDataMap(byteMap);
		AccountField account = mAccount.getAccount();
		if(account != null) {			
			mp.setAccount(account.toByteArray());
		}
		mp.setAccountShare(mAccount.getAccountShareInPercentage());
		mp.setModuleState(state);
		mp.setTotalPositionProfit(mPosition.getPositionProfit());
		mp.setTotalCloseProfit(mTrade.getTotalCloseProfit());
		mp.setDealRecords(mTrade.getDealRecords());
		return mp;
	}

}
