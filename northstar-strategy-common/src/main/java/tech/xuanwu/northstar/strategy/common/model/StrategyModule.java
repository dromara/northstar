package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
// FIXME 未考虑模组发出的订单分多次成交的情况
@Slf4j
@Builder
public class StrategyModule {
	
	protected ModulePosition mPosition;
	
	protected ModuleTrade mTrade;
	
	protected SignalPolicy signalPolicy;
	
	protected RiskController riskController;
	
	protected Dealer dealer;
	
	protected ModuleStateMachine stateMachine;
	
	protected String mktGatewayId;
	
	protected TradeGateway gateway;
	
	protected String name;

	protected AccountField account;
	
	protected boolean disabled;
	
	protected String tradingDay;
	
	private long lastWarningTime;
	
	@Builder.Default
	private Map<String, OrderField> originOrderIdMap = new HashMap<>();
	//FIXME 未考虑重启程序可能出现的止损丢失问题
	@Builder.Default
	private LinkedList<StopLossItem> stopLossItemRegistry = new LinkedList<>();
	
	public StrategyModule onTick(TickField tick) {
		if(!StringUtils.equals(mktGatewayId, tick.getGatewayId())) {
			return this;
		}
		signalPolicy.updateTick(tick);
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
				Optional<Signal> signal = signalPolicy.onTick(tick);
				if(signal.isPresent()) {
					stateMachine.transformForm(signal.get().isOpening() ? ModuleEventType.OPENING_SIGNAL_CREATED : ModuleEventType.CLOSING_SIGNAL_CREATED);
					dealer.onSignal(signal.get(), this);
				}
			}
		}
		if(dealer.bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			//遍历止损记录，检查止损触发
			Iterator<StopLossItem> itItem = stopLossItemRegistry.iterator();
			while(itItem.hasNext()) {
				StopLossItem item = itItem.next();
				Optional<SubmitOrderReqField> stopLossOrder = item.onTick(tick);
				if(stopLossOrder.isPresent()) {
					gateway.submitOrder(stopLossOrder.get());
					itItem.remove();
				}
			}
			
			if(stateMachine.getState() == ModuleState.PLACING_ORDER) {
				Optional<SubmitOrderReqField> submitOrder = dealer.onTick(tick);
				if(submitOrder.isEmpty()) {
					return this;
				}
				if(submitOrder.get().getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
					throw new IllegalStateException("未定义开平操作");
				}
				boolean isRisky = riskController.testReject(tick, this, submitOrder.get());
				if(submitOrder.get().getOffsetFlag() == OffsetFlagEnum.OF_Open) {
					if(isRisky) {
						stateMachine.transformForm(ModuleEventType.SIGNAL_RETAINED);
						return this;
					}
				}
				originOrderIdMap.put(submitOrder.get().getOriginOrderId(), OrderField.newBuilder().build());	// 用空的订单对象占位
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
				for(Entry<String, OrderField> e : originOrderIdMap.entrySet()) {
					String originOrderId = e.getKey();
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
		if(!StringUtils.equals(mktGatewayId, bar.getGatewayId())) {
			return this;
		}
		signalPolicy.updateBar(bar);
		return this;
	}
	
	public StrategyModule onOrder(OrderField order) {
		if(originOrderIdMap.containsKey(order.getOriginOrderId())) {
			originOrderIdMap.put(order.getOriginOrderId(), order); //更新
			switch(order.getOrderStatus()) {
			case OS_AllTraded:
				// DO NOTHING
				break;
			case OS_Rejected:
			case OS_Canceled:
				stateMachine.transformForm(ModuleEventType.ORDER_CANCELLED);
				originOrderIdMap.remove(order.getOriginOrderId());
				break;
			default:
				stateMachine.transformForm(ModuleEventType.ORDER_SUBMITTED);
			}
		}
		return this;
	}
	
	public boolean onTrade(TradeField trade) {
		if(originOrderIdMap.containsKey(trade.getOriginOrderId())) {
			OrderField order = originOrderIdMap.remove(trade.getOriginOrderId());
			Optional<StopLossItem> stopLossItem = StopLossItem.generateFrom(trade, order);
			if(stopLossItem.isPresent()) {
				stopLossItemRegistry.add(stopLossItem.get());
			}
			// 考虑一个order分多次成交的情况
			if(trade.getVolume() < order.getTradedVolume()) {
				log.info("订单{}分可能多次成交", order.getOriginOrderId());
				OrderField restOrder = OrderField.newBuilder(order)
						.setTradedVolume(order.getTradedVolume() - trade.getVolume())
						.build();
				originOrderIdMap.put(restOrder.getOriginOrderId(), restOrder);
			} else {				
				stateMachine.transformForm(trade.getDirection() == DirectionEnum.D_Buy ? ModuleEventType.BUY_TRADED : ModuleEventType.SELL_TRADED);
			}
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
