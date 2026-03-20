package org.dromara.northstar.common.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 交易助手，简化开平仓代码
 * @auth KevinHuangwl
 */
public class TradeHelper {

	private final IModuleContext context;
	private final Contract tradeContract;

	@Setter
	private Predicate<Double> abortTradeIfPriceExceeds = priceDiff -> false; // 默认不放弃交易
	
	@Setter
	private long defaultTimeout = 5000;	// 默认5秒后自动撤单追单
	
	
	@Builder
	public TradeHelper(IModuleStrategyContext context, Contract tradeContract) {
		Assert.notNull(tradeContract, "交易合约不能为空");
		Assert.notNull(context, "模组上下文不能为空");
		this.context = (IModuleContext) context;
		this.tradeContract = tradeContract;
	}
	
	public int getCloseVolume(DirectionEnum holdingDirection) {
		PositionDirectionEnum posDir = switch (holdingDirection) {
		case D_Buy -> PositionDirectionEnum.PD_Long;
		case D_Sell -> PositionDirectionEnum.PD_Short;
		default -> throw new IllegalArgumentException("Unexpected value: " + holdingDirection);
		};
		AtomicInteger actualVol = new AtomicInteger();
		context.getAccount(tradeContract).getPosition(posDir, tradeContract).ifPresent(pos -> actualVol.set(pos.position() - pos.frozen()));
		int logicalVol = context.getModuleAccount().getNonclosedPosition(tradeContract, holdingDirection);
		return Math.clamp(actualVol.get(), 1, logicalVol);
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
	public void doBuyOpen(double price, int vol) {
		doAction(SignalOperation.BUY_OPEN, price, vol);
	}
	/**
	 * 对手价买开
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
	public void doSellOpen(double price, int vol) {
		doAction(SignalOperation.SELL_OPEN, price, vol);
	}
	/**
	 * 对手价卖开
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
	public void doBuyClose(double price, int vol) {
		doAction(SignalOperation.BUY_CLOSE, price, vol);
	}
	/**
	 * 对手价买平
	 * @param vol
	 */
	public void doBuyClose(int vol) {
		doAction(SignalOperation.BUY_CLOSE, vol);
	}
	/**
	 * 对手价买平所有持仓
	 */
	public void doBuyCloseAll() {
		int vol = getCloseVolume(DirectionEnum.D_Sell);
		if(vol > 0) {
			doBuyClose(vol);
		}
	}
	/**
	 * 卖平
	 * @param price
	 * @param vol
	 * @param timeout
	 * @param priceDiffConditionToAbort
	 */
	public void doSellClose(double price, int vol) {
		doAction(SignalOperation.SELL_CLOSE, price, vol);
	}
	/**
	 * 对手价卖平
	 * @param vol
	 */
	public void doSellClose(int vol) {
		doAction(SignalOperation.SELL_CLOSE, vol);
	}
	/**
	 * 对手价卖平所有持仓
	 */
	public void doSellCloseAll() {
		int vol = getCloseVolume(DirectionEnum.D_Buy);
		if(vol > 0) {
			doSellClose(vol);
		}
	}
	/**
	 * 反手买
	 * @param vol
	 */
	public void doBuyReverse(int vol) {
		doAction(SignalOperation.BUY_REVERSE, vol);
	}
	/**
	 * 反手卖
	 * @param vol
	 */
	public void doSellReverse(int vol) {
		doAction(SignalOperation.SELL_REVERSE, vol);
	}
	
	private void doAction(SignalOperation operation, double price, int vol) {
		context.submitOrderReq(TradeIntent.builder()
				.contract(tradeContract)
				.operation(operation)
				.price(price)
				.priceType(PriceType.LIMIT_PRICE)
				.timeout(defaultTimeout)
				.volume(vol)
				.priceDiffConditionToAbort(abortTradeIfPriceExceeds)
				.build());
	}
	
	private void doAction(SignalOperation operation, int vol) {
		context.submitOrderReq(TradeIntent.builder()
				.contract(tradeContract)
				.operation(operation)
				.priceType(PriceType.OPP_PRICE)
				.timeout(defaultTimeout)
				.volume(vol)
				.priceDiffConditionToAbort(abortTradeIfPriceExceeds)
				.build());
	}
}
