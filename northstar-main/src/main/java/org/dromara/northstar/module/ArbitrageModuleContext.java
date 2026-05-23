package org.dromara.northstar.module;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;

public class ArbitrageModuleContext extends ModuleContext implements IModuleContext{
	
	private ExecutorService exec = Executors.newFixedThreadPool(2);
	
	private final Logger logger;

	public ArbitrageModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, moduleRepo);
		logger = getLogger(getClass());
	}

	/**
	 * 套利模组上下文为了实现同时发单，利用了多线程处理下单过程
	 */
	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		exec.execute(() -> {
			logger.info("下单交由子线程处理");
			if(!isEnabled()) {
				logger.info("策略处于停用状态，忽略委托单");
				return;
			}
			mktCenter.lastTick(tradeIntent.getContract()).ifPresentOrElse(tick -> {
				logger.info("收到下单意图：{}", tradeIntent);
				tradeIntentMap.put(tradeIntent.getContract(), tradeIntent);
				tradeIntent.setContext(this);
				tradeIntent.onTick(tick);	
			}, () -> logger.warn("没有TICK行情数据时，忽略下单请求"));
		});
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现同时下单
	 */
	@Override
	public Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, int volume, double price) {
		if(!isEnabled()) {
			logger.info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		Assert.isTrue(volume > 0, "下单交易量应该为正数。当前为" + volume);
		Tick tick = mktCenter.lastTick(contract).orElseThrow(() -> new IllegalStateException("没有行情时不应该发送订单"));
		
		double orderPrice = priceType.resolvePrice(tick, operation, price);
		logger.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，交易量【{}】，类型【{}】", 
				tick.actionDay(), tick.actionTime(),
				contract.unifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		
		String id = UUID.randomUUID().toString();
		String gatewayId = getAccount(contract).accountId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		int factor = priceType == PriceType.ANY_PRICE ? 0 : FieldUtils.directionFactor(direction);	// 市价时没有超价处理
		double plusPrice = getModuleDescription().getOrderPlusTick() * contract.priceTick(); // 超价设置
		Position pos = getAccount(contract).getPosition(OrderUtils.getClosingDirection(direction), contract)
				.orElse(Position.builder().contract(contract).build());
		Tuple<OffsetFlagEnum, Integer> tuple = getModuleDescription().getClosingPolicy().resolve(operation, pos, volume);
		if(tuple.t1() == OffsetFlagEnum.OF_CloseToday) {
			Position updatePos = pos.toBuilder().tdFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		} else if(tuple.t1() == OffsetFlagEnum.OF_CloseYesterday) {
			Position updatePos = pos.toBuilder().ydFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		}
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.originOrderId(id)
				.contract(contract)
				.gatewayId(gatewayId)
				.direction(direction)
				.offsetFlag(tuple.t1())
				.volume(tuple.t2())
				.price(orderPrice + factor * plusPrice)	// 自动加上超价
				.timeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.orderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.actionTimestamp(System.currentTimeMillis())
				.minVolume(1)
				.build();
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
		} catch (Exception e) {
			logger.error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(orderReq.contract());
			return Optional.empty();
		}
		logger.info("发单：{}，{}", orderReq.originOrderId(), LocalDateTime.now());
		String originOrderId = getAccount(contract).submitOrder(orderReq);
		orderReqMap.put(originOrderId, orderReq);
		return Optional.of(originOrderId);
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现及时撤单
	 */
	@Override
	public void cancelOrder(String originOrderId) {
		if(!orderReqMap.containsKey(originOrderId)) {
			logger.debug("找不到订单：{}", originOrderId);
			return;
		}
		logger.info("撤单：{}", originOrderId);
		Contract contract = orderReqMap.get(originOrderId).contract();
		getAccount(contract).cancelOrder(originOrderId);
	}

}
