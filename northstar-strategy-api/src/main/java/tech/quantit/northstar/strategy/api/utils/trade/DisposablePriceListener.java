package tech.quantit.northstar.strategy.api.utils.trade;

import java.util.Objects;
import java.util.function.Predicate;

import lombok.Builder;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.IDisposablePriceListener;
import tech.quantit.northstar.strategy.api.IModuleStrategyContext;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

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
	
	private Predicate<TickField> testFunc;
	
	private Runnable callback;
	
	public static DisposablePriceListener create(IModuleStrategyContext ctx, ContractField contract, DirectionEnum openDir, double basePrice, int numOfPriceTickToTrigger, int volume) {
		if(numOfPriceTickToTrigger == 0) {
			throw new IllegalArgumentException("无效的止盈止损位");
		}
		int factor = FieldUtils.directionFactor(openDir);
		double actionPrice = basePrice + factor * numOfPriceTickToTrigger * contract.getPriceTick();
		SignalOperation closeOpr = factor > 0 ? SignalOperation.SELL_CLOSE : SignalOperation.BUY_CLOSE;
		String desc = String.format("%s，%s", numOfPriceTickToTrigger < 0 ? "止损" : "止盈", actionPrice);
		Predicate<TickField> testFunc;
		if(numOfPriceTickToTrigger > 0) {
			testFunc = t -> (int)(factor * (t.getLastPrice() - basePrice) / contract.getPriceTick()) >= numOfPriceTickToTrigger;
		} else {
			testFunc = t -> (int)(factor * (t.getLastPrice() - basePrice) / contract.getPriceTick()) <= numOfPriceTickToTrigger;
		} 
			
		return DisposablePriceListener.builder()
				.desc(desc)
				.testFunc(testFunc) 	
				.action(() -> {
					if(ctx.availablePosition(openDir, contract.getUnifiedSymbol()) > 0) {
						ctx.submitOrderReq(contract, closeOpr, PriceType.ANY_PRICE, volume, 0);
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
	public boolean shouldBeTriggered(TickField t){
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
