package org.dromara.northstar.strategy.example;

import java.util.List;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.springframework.util.Assert;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

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
	
	private long orderTime;

	@Override
	public void onTick(Tick tick) {
		if(!isEnabled()) {
			return;
		}
		if(!triggered) {
			triggered = true;
			orderTime = System.currentTimeMillis();
			ctx.submitOrderReq(SubmitOrderReq.builder()
					.gatewayId(ctx.getAccount(c).getId())
					.contract(c)
					.direction(DirectionEnum.D_Buy)
					.offsetFlag(OffsetFlagEnum.OF_Open)
					.price(tick.lowerLimit())
					.volume(ctx.getDefaultVolume())
					.build());
		} else if(System.currentTimeMillis() - orderTime > 60000 && OrderUtils.isPendingOrder(pendingOrder)) {
			ctx.cancelOrder(CancelOrderReq.builder()
					.gatewayId(pendingOrder.gatewayId())
					.originOrderId(pendingOrder.originOrderId())
					.orderId(pendingOrder.orderId())
					.build());
		}
	}
	
	private Order pendingOrder;

	@Override
	public void onOrder(Order order) {
		pendingOrder = order;
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
