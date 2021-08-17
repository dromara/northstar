package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.ExternalSignalPolicy;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 主要负责组装各部件，并控制模组状态流程
 * @author KevinHuangwl
 *
 */
@Slf4j
@Builder
public class StrategyModule {
	
	protected ModulePosition mPosition;
	
	protected ModuleTrade mTrade;
	
	protected SignalPolicy signalPolicy;
	
	protected RiskController riskController;
	
	protected Dealer dealer;
	
	protected ModuleStateMachine stateMachine;
	
	protected TradeGateway gateway;
	
	protected String name;

	protected AccountField account;
	
	protected boolean disabled;
	
	protected String tradingDay;
	
	private long lastWarningTime;
	
	@Builder.Default
	private Set<String> originOrderIdSet = new HashSet<>();
	
	public StrategyModule onTick(TickField tick) {
		BarData barData = signalPolicy.getRefBarData(tick.getUnifiedSymbol());
		if(barData != null) {
			signalPolicy.getRefBarData(tick.getUnifiedSymbol()).update(tick);
		}
		if(disabled) {
			//停用期间忽略数据更新
			return this;
		}
		if(!gateway.isConnected()) {
			long now = System.currentTimeMillis();
			if(now - lastWarningTime > 60000) {
				log.warn("网关[{}]未连接，无法执行策略", gateway.getGatewaySetting().getGatewayId());
				lastWarningTime = now;
			}
			return this;
		}
		if(signalPolicy.bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {	
			mPosition.onTick(tick);
			tradingDay = tick.getTradingDay();
			if(stateMachine.getState() == ModuleState.EMPTY 
					|| stateMachine.getState() == ModuleState.HOLDING_LONG
					|| stateMachine.getState() == ModuleState.HOLDING_SHORT) {				
				Optional<Signal> signal = signalPolicy.updateTick(tick);
				if(signal.isPresent()) {
					stateMachine.transformForm(signal.get().isOpening() ? ModuleEventType.OPENING_SIGNAL_CREATED : ModuleEventType.CLOSING_SIGNAL_CREATED);
					dealer.onSignal(signal.get(), this);
				}
			}
		}
		if(dealer.bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			if(stateMachine.getState() == ModuleState.PLACING_ORDER) {
				Optional<SubmitOrderReqField> submitOrder = dealer.onTick(tick);
				if(submitOrder.isEmpty()) {
					return this;
				}
				if(submitOrder.get().getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
					throw new IllegalStateException("未定义开平操作");
				}
				boolean testFlag = riskController.testReject(tick, this, submitOrder.get());
				if(submitOrder.get().getOffsetFlag() == OffsetFlagEnum.OF_Open) {
					if(testFlag) {
						stateMachine.transformForm(ModuleEventType.SIGNAL_RETAINED);
						return this;
					}
				}
				originOrderIdSet.add(submitOrder.get().getOriginOrderId());
				gateway.submitOrder(submitOrder.get());
			}
			
			if(stateMachine.getState() == ModuleState.PENDING_ORDER) {
				short riskCode = riskController.onTick(tick, this);
				if(riskCode == RiskAuditResult.ACCEPTED) {
					return this;
				}
				
				stateMachine.transformForm((riskCode & RiskAuditResult.REJECTED) > 0 
						? ModuleEventType.REJECT_RISK_ALERTED 
						: ModuleEventType.RETRY_RISK_ALERTED);
				Iterator<String> itOrder = originOrderIdSet.iterator();
				while(itOrder.hasNext()) {
					String originOrderId = itOrder.next();
					CancelOrderReqField cancelOrder = CancelOrderReqField.newBuilder()
							.setGatewayId(gateway.getGatewaySetting().getGatewayId())
							.setOriginOrderId(originOrderId)
							.build();
					gateway.cancelOrder(cancelOrder);
				}
			}
		}
		return this;
	}
	
	public StrategyModule onBar(BarField bar) {
		BarData barData = signalPolicy.getRefBarData(bar.getUnifiedSymbol());
		if(barData != null) {
			signalPolicy.getRefBarData(bar.getUnifiedSymbol()).update(bar);
		}
		if(disabled) {
			//停用期间忽略数据更新
			return this;
		}
		if(signalPolicy.bindedUnifiedSymbols().contains(bar.getUnifiedSymbol())) {			
			if(stateMachine.getState() == ModuleState.EMPTY 
					|| stateMachine.getState() == ModuleState.HOLDING_LONG
					|| stateMachine.getState() == ModuleState.HOLDING_SHORT) {				
				Optional<Signal> signal = signalPolicy.updateBar(bar);
				if(signal.isPresent()) {
					stateMachine.transformForm(signal.get().isOpening() ? ModuleEventType.OPENING_SIGNAL_CREATED : ModuleEventType.CLOSING_SIGNAL_CREATED);
					dealer.onSignal(signal.get(), this);
				}
			}
		}
		return this;
	}
	
	public StrategyModule onOrder(OrderField order) {
		
		if(originOrderIdSet.contains(order.getOriginOrderId())) {
			switch(order.getOrderStatus()) {
			case OS_AllTraded:
				// DO NOTHING
				break;
			case OS_Rejected:
			case OS_Canceled:
				stateMachine.transformForm(ModuleEventType.ORDER_CANCELLED);
				originOrderIdSet.remove(order.getOriginOrderId());
				break;
			default:
				stateMachine.transformForm(ModuleEventType.ORDER_SUBMITTED);
			}
		}
		return this;
	}
	
	public boolean onTrade(TradeField trade) {
		if(originOrderIdSet.contains(trade.getOriginOrderId())) {
			originOrderIdSet.remove(trade.getOriginOrderId());
			stateMachine.transformForm(trade.getDirection() == DirectionEnum.D_Buy ? ModuleEventType.BUY_TRADED : ModuleEventType.SELL_TRADED);
			mTrade.updateTrade(TradeDescription.convertFrom(getName(), trade));
			mPosition.onTrade(trade);
			return true;
		}
		return false;
	}
	
	public void onExternalMessage(String text) {
		if(signalPolicy instanceof ExternalSignalPolicy) {
			((ExternalSignalPolicy)signalPolicy).onExtMsg(text);
		}
	}
	
	public StrategyModule onAccount(AccountField account) {
		if(StringUtils.equals(account.getGatewayId(), gateway.getGatewaySetting().getGatewayId())) {
			this.account = account;
		}
		return this;
	}
	
	public AccountField getAccount() {
		return account;
	}
	
	public String getName() {
		return name;
	}
	
	public ModuleState getState() {
		return stateMachine.getState();
	}
	
	public boolean isEnabled() {
		return !disabled;
	}
	
	public void toggleRunningState() {
		disabled = !disabled;
	}
	
	public String getTradingDay() {
		return tradingDay;
	}
	
	public ModuleTrade getModuleTrade() {
		return mTrade;
	}
	
	public ModulePosition getModulePosition() {
		return mPosition;
	}
	
	public TradeGateway getGateway() {
		return gateway;
	}
	
	public ModuleStatus getModuleStatus() {
		ModuleStatus status = new ModuleStatus();
		status.setModuleName(name);
		status.setState(getState());
		List<TradeField> trades = mPosition.getOpenningTrade();
		if(trades.size() > 0) {
			status.setLastOpenTrade(trades.stream().map(trade -> trade.toByteArray()).collect(Collectors.toList()));
			status.setTradeDescrptions(trades.stream().map(trade -> TradeDescription.convertFrom(getName(), trade)).collect(Collectors.toList()));
		}
		return status;
	}
	
	public ModulePerformance getPerformance() {
		ModulePerformance mp = new ModulePerformance();
		mp.setModuleName(getName());
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
		mp.setAccountId(gateway.getGatewaySetting().getGatewayId());
		mp.setAccountBalance(account == null ? 0 : (int)account.getBalance());
		mp.setModuleState(getState());
		mp.setTotalPositionProfit(mPosition.getPositionProfit());
		mp.setTotalCloseProfit(mTrade.getTotalCloseProfit());
		mp.setDealRecords(mTrade.getDealRecords());
		return mp;
	}

}
