package tech.xuanwu.northstar.strategy.common.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.ExternalSignalPolicy;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleDataRef;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleDealRecord;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleRealTimeInfo;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
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
	
	private final ModuleStatus status;
	
	private final SignalPolicy signalPolicy;
	
	private final RiskController riskController;
	
	private final Dealer dealer;
	
	private final String mktGatewayId;
	
	private final TradeGateway gateway;
	
	private boolean disabled;
	
	private long lastWarningTime;
	
	private String tradingDay;
	
	private ContractManager contractMgr;
	
	@Builder.Default
	private Map<String, OrderField> originOrderIdMap = new HashMap<>();
	
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
			tradingDay = tick.getTradingDay();
			status.updateHoldingProfit(tick);
			
			if(status.at(ModuleState.EMPTY) 
					|| status.at(ModuleState.HOLDING_LONG)
					|| status.at(ModuleState.HOLDING_SHORT)) {				
				Optional<Signal> signal = signalPolicy.onTick(tick);
				if(signal.isPresent()) {
					OffsetFlagEnum closingOffset = status.isSameDay(tradingDay) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_Close;
					status.transform(signal.get().isOpening() ? ModuleEventType.OPENING_SIGNAL_CREATED : ModuleEventType.CLOSING_SIGNAL_CREATED);
					dealer.onSignal(signal.get(), signal.get().isOpening() ? OffsetFlagEnum.OF_Open : closingOffset);
				}
			}
		}
		if(dealer.bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			Optional<SubmitOrderReqField> stopLossReq = status.triggerStopLoss(tick, contractMgr.getContract(tick.getUnifiedSymbol()));
			if(stopLossReq.isPresent()) {
				status.transform(ModuleEventType.STOP_LOSS);
				originOrderIdMap.put(stopLossReq.get().getOriginOrderId(), OrderField.newBuilder().build()); // 用空的订单对象占位
				gateway.submitOrder(stopLossReq.get());
				return this;
			}
			
			if(status.at(ModuleState.PLACING_ORDER)) {
				Optional<SubmitOrderReqField> submitOrder = dealer.onTick(tick);
				if(submitOrder.isEmpty()) {
					return this;
				}
				if(submitOrder.get().getOffsetFlag() == OffsetFlagEnum.OF_Unknown) {
					throw new IllegalStateException("未定义开平操作");
				}
				boolean isRisky = riskController.testReject(tick, status, submitOrder.get());
				if(isRisky && submitOrder.get().getOffsetFlag() == OffsetFlagEnum.OF_Open) {
					status.transform(ModuleEventType.SIGNAL_RETAINED);
					return this;
				}
				originOrderIdMap.put(submitOrder.get().getOriginOrderId(), OrderField.newBuilder().build());	// 用空的订单对象占位
				gateway.submitOrder(submitOrder.get());
			}
			
			if(status.at(ModuleState.PENDING_ORDER)) {
				short riskCode = riskController.onTick(tick, status);
				if(riskCode == RiskAuditResult.ACCEPTED) {
					return this;
				}
				
				status.transform((riskCode & RiskAuditResult.REJECTED) > 0 
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
				status.transform(ModuleEventType.ORDER_CANCELLED);
				originOrderIdMap.remove(order.getOriginOrderId());
				break;
			default:
				status.transform(ModuleEventType.ORDER_SUBMITTED);
			}
		}
		return this;
	}
	
	public Optional<ModuleStatus> onTrade(TradeField trade) {
		if(originOrderIdMap.containsKey(trade.getOriginOrderId())) {
			OrderField order = originOrderIdMap.remove(trade.getOriginOrderId());
			// 考虑一个order分多次成交的情况
			if(trade.getVolume() < order.getTradedVolume()) {
				log.info("订单[{}]分可能多次成交", order.getOriginOrderId());
				OrderField restOrder = OrderField.newBuilder(order)
						.setTradedVolume(order.getTradedVolume() - trade.getVolume())
						.build();
				originOrderIdMap.put(restOrder.getOriginOrderId(), restOrder);
			} else {				
				status.transform(trade.getDirection() == DirectionEnum.D_Buy ? ModuleEventType.BUY_TRADED : ModuleEventType.SELL_TRADED);
				dealer.doneTrade(trade);
			}
			return Optional.of(status.onTrade(trade, order));
		}
		return Optional.empty();
	}
	
	public Optional<ModuleDealRecord> consumeDealRecord() {
		return status.consumeDealRecord();
	}
	
	public void onExternalMessage(String text) {
		if(signalPolicy instanceof ExternalSignalPolicy) {
			((ExternalSignalPolicy)signalPolicy).onExtMsg(text);
		}
	}
	
	public StrategyModule onAccount(AccountField account) {
		if(StringUtils.equals(account.getGatewayId(), gateway.getGatewaySetting().getGatewayId())) {
			status.setAccountAvailable(account.getAvailable());
		}
		return this;
	}
	
	public boolean isEnabled() {
		return !disabled;
	}
	
	public String getName() {
		return status.getModuleName();
	}
	
	public void toggleRunningState() {
		disabled = !disabled;
	}
	
	public String getTradingDay() {
		return tradingDay;
	}
	
	public TradeGateway getGateway() {
		return gateway;
	}
	
	public ModuleRealTimeInfo getRealTimeInfo() {
		ModuleRealTimeInfo mp = new ModuleRealTimeInfo();
		mp.setModuleName(status.getModuleName());
		mp.setAccountId(gateway.getGatewaySetting().getGatewayId());
		mp.setModuleAvailable((int)status.getAccountAvailable());
		mp.setModuleState(status.getCurrentState());
		mp.setTotalPositionProfit(status.getHoldingProfit());
		mp.setLongPositions(status.getLongPositions());
		mp.setShortPositions(status.getShortPositions());
		return mp;
	}
	
	public ModuleDataRef getDataRef() {
		Map<String, List<byte[]>> byteMap = new HashMap<>();
		for(String unifiedSymbol : signalPolicy.bindedUnifiedSymbols()) {
			byteMap.put(unifiedSymbol, 
				signalPolicy.getRefBarData(unifiedSymbol)
					.getRefBarList()
					.stream()
					.map(BarField::toByteArray)
					.collect(Collectors.toList()));
		}
		return ModuleDataRef.builder()
				.refBarDataMap(byteMap)
				.build();
	}
	
	public ModuleStatus updatePosition(ModulePosition position) {
		if(!dealer.bindedUnifiedSymbols().contains(position.getUnifiedSymbol())) {
			throw new IllegalArgumentException("手工更新的合约与交易策略绑定合约不一致");
		}
		status.manuallyUpdatePosition(position);
		return status;
	}
	
	public ModuleStatus removePosition(String unifiedSymbol, PositionDirectionEnum dir) {
		status.manuallyRemovePosition(unifiedSymbol, dir);
		return status;
	}
}
