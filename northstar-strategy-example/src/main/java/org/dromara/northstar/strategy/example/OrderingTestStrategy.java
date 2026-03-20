package org.dromara.northstar.strategy.example;

import java.util.List;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.springframework.util.Assert;

/**
 * 实盘下单测试策略
 * 本策略仅用于在实盘环境下发单是否成功，并不期望成交
 * @auth KevinHuangwl
 */
@StrategicComponent(OrderingTestStrategy.NAME)
public class OrderingTestStrategy extends AbstractStrategy implements TradeStrategy{
	
	protected static final String NAME = "示例-实盘下单测试";
	
	private Contract c;
	
	private boolean triggered;
	
	
	@Override
	protected void initIndicators() {
		List<Contract> contracts = ctx.bindedContracts();
		Assert.isTrue(contracts.size() == 1, "只能绑定一个合约");
		c = contracts.get(0);
	}

	@Override
	public void onTick(Tick tick) {
		if(!isEnabled()) {
			return;
		}
		if(!triggered) {
			triggered = true;
			ctx.submitOrderReq(TradeIntent.builder()
					.contract(c)
					.price(tick.lowerLimit())
					.priceType(PriceType.LIMIT_PRICE)
					.operation(SignalOperation.BUY_OPEN)
					.volume(ctx.getDefaultVolume())
					.timeout(10000)
					.build());
		}
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		// 不需要任何参数
	}
	
	public static class InitParams extends DynamicParams{}	
}
