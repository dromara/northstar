package tech.xuanwu.northstar.domain.strategy;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.api.model.ModuleDealRecord;
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

	private SubmitOrderReqField submitOrderReq;
	
	private ModulePosition currentPosition;
	
	private Consumer<Optional<ModulePosition>> openCallback;
	
	private Consumer<Optional<ModuleDealRecord>> closeCallback; 
	
	private boolean partiallyTraded;
	
	private TradeField latestTrade;
	
	private String moduleName;
	
	@Getter
	private boolean isDone;
	
	public ModuleTradeIntent(String moduleName, SubmitOrderReqField submitOrderReq, Consumer<Optional<ModulePosition>> onDoneOpen) {
		if(!FieldUtils.isOpen(submitOrderReq.getOffsetFlag())) {
			throw new IllegalStateException("该构造方法仅适用于开仓操作");
		}
		this.submitOrderReq = submitOrderReq;
		this.openCallback = onDoneOpen;
		this.moduleName = moduleName;
	}
	
	public ModuleTradeIntent(String moduleName, ModulePosition position, SubmitOrderReqField submitOrderReq, Consumer<Optional<ModuleDealRecord>> onDoneClose) {
		if(!FieldUtils.isClose(submitOrderReq.getOffsetFlag())) {
			throw new IllegalStateException("该构造方法仅适用于平仓操作");
		}
		this.submitOrderReq = submitOrderReq;
		this.currentPosition = position;
		this.moduleName = moduleName;
		this.closeCallback = onDoneClose;
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
		
		partiallyTraded = order.getOrderStatus() == OrderStatusEnum.OS_Canceled 
				&& order.getTotalVolume() == submitOrderReq.getVolume() 
				&& order.getTradedVolume() > 0;
		
		if(FieldUtils.isOpen(order.getOffsetFlag())) {
			onOpenOrder(order);
		} else if(FieldUtils.isClose(order.getOffsetFlag())) {
			onCloseOrder(order);
		}
	}
	
	// Trade反馈可能是多次
	public void onTrade(TradeField trade) {
		// 校验是匹配的回报
		if(!StringUtils.equals(trade.getOriginOrderId(), submitOrderReq.getOriginOrderId())) 
			return;
		
		latestTrade = trade;
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			onOpenTrade(trade);
		} else if(FieldUtils.isClose(trade.getOffsetFlag())) {
			onCloseTrade(trade);
		}
	}
	
	
	private void onOpenOrder(OrderField order) {
		// 处理情况四
		if(partiallyTraded && latestTrade != null) {
			openCallback.accept(Optional.of(new ModulePosition(moduleName, latestTrade, submitOrderReq.getStopPrice())));
			isDone = true;
		}
		// 处理情况三、情况五
		if(order.getOrderStatus() == OrderStatusEnum.OS_Rejected ||
				order.getOrderStatus() == OrderStatusEnum.OS_Canceled 
				&& order.getTotalVolume() == submitOrderReq.getVolume() 
				&& order.getTradedVolume() == 0) {
			openCallback.accept(Optional.empty());
			isDone = true;
		}
	}
	
	private void onCloseOrder(OrderField order) {
		// 处理情况四
		if(partiallyTraded && latestTrade != null) {
			closeCallback.accept(Optional.of(genDealRecord()));
			isDone = true;
		}
		// 处理情况三、情况五
		if(order.getOrderStatus() == OrderStatusEnum.OS_Rejected ||
				order.getOrderStatus() == OrderStatusEnum.OS_Canceled 
				&& order.getTotalVolume() == submitOrderReq.getVolume() 
				&& order.getTradedVolume() == 0) {
			closeCallback.accept(Optional.empty());
			isDone = true;
		}
	}
	
	
	private void onOpenTrade(TradeField trade) {
		// 处理情况四，不确定trade回报与order回报哪个先到达
		// 处理情况一、二。如果是多次成交，trade的成交数量可能会变，但originOrderId不会变，所以只要核对总量即可
		if(partiallyTraded || trade.getVolume() == submitOrderReq.getVolume()) {
			openCallback.accept(Optional.of(new ModulePosition(moduleName, trade, submitOrderReq.getStopPrice())));
			isDone = true;
		}
	}
	
	private void onCloseTrade(TradeField trade) {
		// 处理情况四，不确定trade回报与order回报哪个先到达
		// 处理情况一、二。如果是多次成交，trade的成交数量可能会变，但originOrderId不会变，所以只要核对总量即可
		if(partiallyTraded || trade.getVolume() == submitOrderReq.getVolume()) {
			closeCallback.accept(Optional.of(genDealRecord()));
			isDone = true;
		}
	}
	
	private ModuleDealRecord genDealRecord() {
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
