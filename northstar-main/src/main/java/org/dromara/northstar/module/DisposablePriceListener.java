package org.dromara.northstar.module;

import java.util.Objects;
import java.util.function.Predicate;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.strategy.IDisposablePriceListener;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;

/**
 * 一次性价格监听器
 * @author KevinHuangwl
 *
 */
@Builder
public final class DisposablePriceListener implements IDisposablePriceListener {

	@Builder.Default
	protected boolean valid = true;
	
	protected Runnable action;
	
	protected String desc;
	
	private Predicate<Tick> testFunc;
	
	private Runnable callback;
	
	public static DisposablePriceListener create(IModuleContext ctx, Contract contract, DirectionEnum openDir, double basePrice, int numOfPriceTickToTrigger, int volume) {
		if(numOfPriceTickToTrigger == 0) {
			throw new IllegalArgumentException("无效的止盈止损位");
		}
		int factor = FieldUtils.directionFactor(openDir);
		double actionPrice = basePrice + factor * numOfPriceTickToTrigger * contract.priceTick();
		SignalOperation closeOpr = factor > 0 ? SignalOperation.SELL_CLOSE : SignalOperation.BUY_CLOSE;
		String desc = String.format("%s，%s", numOfPriceTickToTrigger < 0 ? "止损" : "止盈", actionPrice);
		Predicate<Tick> testFunc;
		if(numOfPriceTickToTrigger > 0) {
			testFunc = t -> (int)(factor * (t.lastPrice() - basePrice) / contract.priceTick()) >= numOfPriceTickToTrigger;
		} else {
			testFunc = t -> (int)(factor * (t.lastPrice() - basePrice) / contract.priceTick()) <= numOfPriceTickToTrigger;
		} 
			
		return DisposablePriceListener.builder()
				.desc(desc)
				.testFunc(testFunc) 	
				.action(() -> {
					if(ctx.getModuleAccount().getNonclosedPosition(contract, openDir) > 0) {
						ctx.submitOrderReq(TradeIntent.builder()
								.contract(contract)
								.operation(closeOpr)
								.priceType(PriceType.OPP_PRICE)
								.volume(volume)
								.timeout(3000)	// 由于止损要求尽快成交，因此若3秒无成交则撤单重试
								.build());
					}
				})
				.build();
	}
	
	/**
	 * 监听器描述，用于记录日志，分辨不同的监听器的作用
	 * @return
	 */
	public String description() {
		return desc;
	}
	
	/**
	 * 是否有效，失效的监听器会从模组上下文自动移除
	 * @return
	 */
	public boolean isValid() {
		return valid;
	}
	
	/**
	 * 令监听器失效
	 */
	@Override
	public void invalidate() {
		valid = false;
	}
	
	/**
	 * 触发条件
	 * @return
	 */
	public boolean shouldBeTriggered(Tick t){
		return testFunc.test(t);
	}
	
	/**
	 * 执行逻辑
	 */
	public void execute() {
		action.run();
		valid = false;
		if(Objects.nonNull(callback)) {
			callback.run();
		}
	}

	/**
	 * 设置监听器触发后回调
	 */
	@Override
	public void setCallback(Runnable callback) {
		this.callback = callback;
	}
	
}
