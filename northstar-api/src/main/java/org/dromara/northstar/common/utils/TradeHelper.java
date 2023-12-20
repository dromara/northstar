package org.dromara.northstar.common.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 交易助手，简化开平仓代码
 * @auth KevinHuangwl
 */
@Builder
public class TradeHelper {

	private IModuleContext context;
	
	private Contract tradeContract;
	
	@Builder.Default
	private long timeout = 5000;
	
	public int getCloseVolume(DirectionEnum holdingDirection) {
		PositionDirectionEnum posDir = switch (holdingDirection) {
		case D_Buy -> PositionDirectionEnum.PD_Long;
		case D_Sell -> PositionDirectionEnum.PD_Short;
		default -> throw new IllegalArgumentException("Unexpected value: " + holdingDirection);
		};
		AtomicInteger actualVol = new AtomicInteger();
		context.getAccount(tradeContract).getPosition(posDir, tradeContract).ifPresent(pos -> actualVol.set(pos.position() - pos.frozen()));
		int logicalVol = context.getModuleAccount().getNonclosedPosition(tradeContract, holdingDirection);
		return Math.max(Math.min(actualVol.get(), logicalVol), 1);
	} 
	
	/**
	 * 求价差率
	 * @param numerator
	 * @param denominator
	 * @param buyNumerator
	 * @return
	 */
	public static double spreadRateInPercentage(Tick numerator, Tick denominator, boolean buyNumerator) {
		double numeratorPrice = buyNumerator ? numerator.askPrice().get(0) : numerator.bidPrice().get(0);
		double denominatorPrice = buyNumerator ? denominator.bidPrice().get(0) : denominator.askPrice().get(0);
		return (numeratorPrice / denominatorPrice - 1) * 100;
	}
	
	/**
	 * 买开
	 * @param price
	 * @param vol
	 * @param timeout
	 * @param priceDiffConditionToAbort
	 */
	public void doBuyOpen(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort) {
		doAction(SignalOperation.BUY_OPEN, price, vol, timeout, priceDiffConditionToAbort);
	}
	/**
	 * 买开
	 * @param vol
	 */
	public void doBuyOpen(int vol) {
		doAction(SignalOperation.BUY_OPEN, vol);
	}
	/**
	 * 卖开
	 * @param price
	 * @param vol
	 * @param timeout
	 * @param priceDiffConditionToAbort
	 */
	public void doSellOpen(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort) {
		doAction(SignalOperation.SELL_OPEN, price, vol, timeout, priceDiffConditionToAbort);
	}
	/**
	 * 卖开
	 * @param vol
	 */
	public void doSellOpen(int vol) {
		doAction(SignalOperation.SELL_OPEN, vol);
	}
	/**
	 * 买平
	 * @param price
	 * @param vol
	 * @param timeout
	 * @param priceDiffConditionToAbort
	 */
	public void doBuyClose(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort) {
		doAction(SignalOperation.BUY_CLOSE, price, vol, timeout, priceDiffConditionToAbort);
	}
	/**
	 * 买平
	 * @param vol
	 */
	public void doBuyClose(int vol) {
		doAction(SignalOperation.BUY_CLOSE, vol);
	}
	/**
	 * 卖平
	 * @param price
	 * @param vol
	 * @param timeout
	 * @param priceDiffConditionToAbort
	 */
	public void doSellClose(double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort) {
		doAction(SignalOperation.SELL_CLOSE, price, vol, timeout, priceDiffConditionToAbort);
	}
	/**
	 * 卖平
	 * @param vol
	 */
	public void doSellClose(int vol) {
		doAction(SignalOperation.SELL_CLOSE, vol);
	}
	
	private void doAction(SignalOperation operation, double price, int vol, long timeout, Predicate<Double> priceDiffConditionToAbort) {
		context.submitOrderReq(TradeIntent.builder()
				.contract(tradeContract)
				.operation(operation)
				.price(price)
				.priceType(PriceType.LIMIT_PRICE)
				.timeout(timeout)
				.volume(vol)
				.priceDiffConditionToAbort(priceDiffConditionToAbort)
				.build());
	}
	
	private void doAction(SignalOperation operation, int vol) {
		context.submitOrderReq(TradeIntent.builder()
				.contract(tradeContract)
				.operation(operation)
				.priceType(PriceType.OPP_PRICE)
				.timeout(timeout)
				.volume(vol)
				.build());
	}
}
