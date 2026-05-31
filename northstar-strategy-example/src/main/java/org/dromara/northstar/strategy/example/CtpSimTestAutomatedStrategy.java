package org.dromara.northstar.strategy.example;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;

@StrategicComponent(CtpSimTestAutomatedStrategy.NAME)
public class CtpSimTestAutomatedStrategy extends AbstractStrategy implements TradeStrategy {

	protected static final String NAME = "示例-CTP自动化穿透测试";

	private TradeHelper hlp;
	
	private Contract c;
	
	private TestState state = TestState.OrderCheckingTest;

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
	}
	
	@Override
	protected void initIndicators() {
		logger = ctx.getLogger(getClass());
		c = ctx.bindedContracts().getFirst();
		hlp = new TradeHelper(ctx, c);
		ctx.setOrderRequestFilter(new OrderRequestFilter() {
			
			private static final int MAX_ORDER_REQ_PER_DAY = 30; //假设这个是风控上限
			
			private LocalDate tradingDay;
			
			private Map<String, AtomicInteger> contractReqCounterMap = new HashMap<>();
			
			{
				contractReqCounterMap.put(c.unifiedSymbol(), new AtomicInteger());
			}
			
			@Override
			public void onTick(Tick tick) {
				if(!tradingDay.equals(tick.tradingDay())) {
					tradingDay = tick.tradingDay();
					contractReqCounterMap.values().forEach(cnt -> cnt.set(0));
				}
			}

			@Override
			public void doFilter(SubmitOrderReq orderReq) {
				logger.info("自定义示例订单过滤器正进行风控过滤");
				if(!contractReqCounterMap.containsKey(orderReq.contract().unifiedSymbol())) {
					throw new IllegalStateException(String.format("模组没包含合约：%s。 可选合约：%s", orderReq.contract().unifiedSymbol(), contractReqCounterMap.keySet()));
				}
				String unifiedSymbol = orderReq.contract().unifiedSymbol();
				logger.info("当天 [{}] 合约的剩余发单次数为：{}", unifiedSymbol, MAX_ORDER_REQ_PER_DAY - contractReqCounterMap.get(unifiedSymbol).get());
				if(contractReqCounterMap.get(unifiedSymbol).getAndIncrement() > MAX_ORDER_REQ_PER_DAY) {
					logger.warn("模组触发 [{}] 合约的日内免费申报上限。自动停用模组。", orderReq.contract().name());
					ctx.disabledModule();
					throw new IllegalStateException("触发风控规则，中止委托发单");
				}
			}
		});
	}
	
	private void tradeTest() {
		if(state == TestState.BuyOpenTest && ctx.getState().isEmpty()) {
			logger.info("============ 正在进行交易测试：买开 ============");
			hlp.buyOpen(1);
			state = TestState.SellCloseTest;
			return;
		}
		
		if(state == TestState.SellCloseTest && ctx.getState().isHolding()) {
			logger.info("============ 正在进行交易测试：卖平 ============");
			hlp.sellClose(1);
			state = TestState.SellOpenTest;
			return;
		}
		
		if(state == TestState.SellOpenTest && ctx.getState().isEmpty()) {
			logger.info("============ 正在进行交易测试：卖开 ============");
			hlp.sellOpen(1);
			state = TestState.BuyCloseTest;
			return;
		}
		
		if(state == TestState.BuyCloseTest && ctx.getState().isHolding()) {
			logger.info("============ 正在进行交易测试：买平 ============");
			hlp.buyClose(1);
			state = TestState.OrderAndWithdrawTest;
		}
		
	}
	
	private Optional<Order> orderOpt = Optional.empty();
	
	private void orderCounterTest(Tick t) {
		// 挂单，自动撤单
		if(state == TestState.OrderAndWithdrawTest) {
			if(ctx.getState().isEmpty()) {
				logger.info("============ 正在进行委托及撤单测试 ============");
				hlp.buyOpen(t.lastPrice() - 30 * t.contract().priceTick(), numOfBarsToPrepare);;
			} else if (ctx.getState().isOrdering() && orderOpt.isPresent()) {
				ctx.cancelOrder(CancelOrderReq.builder()
						.gatewayId(orderOpt.get().gatewayId())
						.originOrderId(orderOpt.get().originOrderId())
						.orderId(orderOpt.get().orderId())
						.build());
				orderOpt = Optional.empty();
			}
		}
	}
	
	private void orderCheckingTest(Tick t) {
		if(state == TestState.OrderCheckingTest) {
			try {
				logger.info("============ 正在进行无效挂单测试：无效委托量 ============");
//				ctx.submitOrderReq(c, SignalOperation.BUY_OPEN, PriceType.LIMIT_PRICE, -1, t.lastPrice());
			} catch (Exception e) {
				logger.error("捕获到无效订单异常，主动拦截委托单：{}", e.getMessage());
			}
			
			try {
				logger.info("============ 正在进行无效挂单测试：无效委托价 ============");
//				ctx.submitOrderReq(c, SignalOperation.BUY_OPEN, PriceType.LIMIT_PRICE, 1, t.lastPrice() + t.contract().priceTick() * 0.3);
			} catch (Exception e) {
				logger.info("捕获到无效订单异常，主动拦截委托单：{}", e.getMessage());
			}
			
			state = TestState.BuyOpenTest;
		}
	}
	
	@Override
	public void onTick(Tick tick) {
		if(!isEnabled()) {
			return;
		}
		
		// 指令检查测试
		orderCheckingTest(tick);
		
		// 基础交易测试
		tradeTest();
		
		// 报撤单笔数监测
		orderCounterTest(tick);
		
	}

	@Override
	public String name() {
		return NAME;
	}

	public static class InitParams extends DynamicParams {

	}
	
	private enum TestState {
		BuyOpenTest,
		SellCloseTest,
		SellOpenTest,
		BuyCloseTest,
		OrderAndWithdrawTest,
		OrderCheckingTest,
	}
}
