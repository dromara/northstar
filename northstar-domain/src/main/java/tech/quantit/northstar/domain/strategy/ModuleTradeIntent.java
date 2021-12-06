package tech.quantit.northstar.domain.strategy;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组交易意图，用于管理下单的整个过程
 * @author KevinHuangwl
 *
 */
public class ModuleTradeIntent {
	@Getter
	private SubmitOrderReqField submitOrderReq;
	
	private ModulePosition currentPosition;
	// 开仓回调
	private Consumer<ModulePosition> openCallback;
	// 平仓回调
	private Consumer<ModuleDealRecord> closeCallback; 
	// 撤单回调，返回是否部分成交标识 
	private Consumer<Boolean> fallback;
	
	private String moduleName;
	
	public ModuleTradeIntent(String moduleName, SubmitOrderReqField submitOrderReq, Consumer<ModulePosition> onDoneOpen,
			Consumer<ModuleDealRecord> onDoneClose, Consumer<Boolean> fallback) {
		if(!FieldUtils.isOpen(submitOrderReq.getOffsetFlag())) {
			throw new IllegalStateException("该构造方法仅适用于开仓操作");
		}
		this.submitOrderReq = submitOrderReq;
		this.openCallback = onDoneOpen;
		this.closeCallback = onDoneClose;
		this.moduleName = moduleName;
		this.fallback = fallback;
	}
	
	public ModuleTradeIntent(String moduleName, ModulePosition position, SubmitOrderReqField submitOrderReq, Consumer<ModuleDealRecord> onDoneClose, Consumer<Boolean> fallback) {
		if(!FieldUtils.isClose(submitOrderReq.getOffsetFlag())) {
			throw new IllegalStateException("该构造方法仅适用于平仓操作");
		}
		this.submitOrderReq = submitOrderReq;
		this.currentPosition = position;
		this.moduleName = moduleName;
		this.closeCallback = onDoneClose;
		this.fallback = fallback;
	}
	
	public String originOrderId() {
		return submitOrderReq.getOriginOrderId();
	}
	
	// Order反馈可能是多次
	// 情况一：一次性成交
	// 情况二：分多次成交
	// 情况三：未成交就撤单
	// 情况四：部分成交然后撤单
	// 情况五：废单
	public void onOrder(OrderField order) {
		// 校验是匹配的回报
		if(!StringUtils.equals(order.getOriginOrderId(), submitOrderReq.getOriginOrderId())) 
			return;
		
		boolean partiallyTraded = order.getOrderStatus() == OrderStatusEnum.OS_Canceled 
				&& order.getTotalVolume() == submitOrderReq.getVolume() 
				&& order.getTradedVolume() > 0;
		
		// 处理情况三、四、五
		if(partiallyTraded) {
			fallback.accept(true);
		} else if(order.getOrderStatus() == OrderStatusEnum.OS_Canceled || order.getOrderStatus() == OrderStatusEnum.OS_Rejected) {
			fallback.accept(false);
		}
	}
	
	// Trade反馈可能是多次
	public void onTrade(TradeField trade) {
		// 校验是匹配的回报
		if(!StringUtils.equals(trade.getOriginOrderId(), submitOrderReq.getOriginOrderId())) 
			return;
		
		// 处理情况一、二、四
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			openCallback.accept(new ModulePosition(moduleName, trade, submitOrderReq.getStopPrice(), closeCallback));
		} else if(FieldUtils.isClose(trade.getOffsetFlag())) {
			closeCallback.accept(genDealRecord(trade));
		}
	}
	
	public int volume() {
		return submitOrderReq.getVolume();
	}
	
	private ModuleDealRecord genDealRecord(TradeField latestTrade) {
		double openPrice = currentPosition.openPrice();
		double multiplier = latestTrade.getContract().getMultiplier();
		int factor = FieldUtils.isLong(currentPosition.getDirection()) ? 1 : -1;
		double closeProfit = factor * (latestTrade.getPrice() - openPrice) * latestTrade.getVolume() * multiplier;
		double occupiedMoney = Math.max(openPrice, latestTrade.getPrice()) * latestTrade.getVolume() * multiplier
				* (factor > 0 ? latestTrade.getContract().getLongMarginRatio() : latestTrade.getContract().getShortMarginRatio()) * Constants.ESTIMATED_FROZEN_FACTOR;
		return ModuleDealRecord.builder()
				.moduleName(moduleName)
				.contractName(latestTrade.getContract().getFullName())
				.direction(currentPosition.getDirection())
				.tradingDay(latestTrade.getTradingDay())
				.dealTimestamp(System.currentTimeMillis())
				.volume(latestTrade.getVolume())
				.openPrice(openPrice)
				.closePrice(latestTrade.getPrice())
				.closeProfit((int) closeProfit)
				.estimatedOccupiedMoney(occupiedMoney)
				.build();
	}
}
