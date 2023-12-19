package org.dromara.northstar.module;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Bar;
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
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.lang.Assert;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;

public class ArbitrageModuleContext extends ModuleContext implements IModuleContext{
	
	private ExecutorService exec = Executors.newFixedThreadPool(2);
	
	private final Logger logger;

	public ArbitrageModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo, BarMergerRegistry barMergerRegistry) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, moduleRepo, barMergerRegistry);
		logger = getLogger(getClass());
	}

	/**
	 * 套利模组上下文为了实现同时发单，利用了多线程处理下单过程
	 */
	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		exec.execute(() -> {
			logger.info("下单交由子线程处理");
			if(!module.isEnabled()) {
				if(isReady()) {
					logger.info("策略处于停用状态，忽略委托单");
				}
				return;
			}
			Tick tick = latestTickMap.get(tradeIntent.getContract());
			if(Objects.isNull(tick)) {
				logger.warn("没有TICK行情数据时，忽略下单请求");
				return;
			}
			logger.info("收到下单意图：{}", tradeIntent);
			tradeIntentMap.put(tradeIntent.getContract(), tradeIntent);
			tradeIntent.setContext(this);
			tradeIntent.onTick(tick);	
		});
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现同时下单
	 */
	@Override
	public Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, int volume, double price) {
		if(!module.isEnabled()) {
			if(isReady()) {
				logger.info("策略处于停用状态，忽略委托单");
			}
			return Optional.empty();
		}
		Tick tick = latestTickMap.get(contract);
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(volume > 0, "下单手数应该为正数。当前为" + volume);
		
		double orderPrice = priceType.resolvePrice(tick, operation, price);
		logger.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
				tick.actionDay(), tick.actionTime(),
				contract.unifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		
		String id = UUID.randomUUID().toString();
		String gatewayId = getAccount(contract).accountId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		int factor = FieldUtils.directionFactor(direction);
		double plusPrice = module.getModuleDescription().getOrderPlusTick() * contract.priceTick(); // 超价设置
		Position pos = getAccount(contract).getPosition(OrderUtils.getClosingDirection(direction), contract)
				.orElse(Position.builder().contract(contract).build());
		Tuple<OffsetFlagEnum, Integer> tuple = module.getModuleDescription().getClosingPolicy().resolve(operation, pos, volume);
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
		String originOrderId = module.getAccount(contract).submitOrder(orderReq);
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
		module.getAccount(contract).cancelOrder(originOrderId);
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		ModuleRuntimeDescription mrd = super.getRuntimeDescription(fullDescription);
		if(fullDescription) {
			if(contractMap.size() == 2) {
				Iterator<Contract> it = contractMap.values().iterator();
				Contract c1 = it.next();
				Contract c2 = it.next();
				Contract nearMonth = c1.lastTradeDate().isBefore(c2.lastTradeDate()) ? c1 : c2;
				Contract farMonth = c1 == nearMonth ? c2 : c1;
				String combName = String.format("%s-%s", nearMonth.name(), farMonth.name());
				Map<Long, Bar> timeBarMap = barBufQMap.get(nearMonth)
						.stream()
						.collect(Collectors.toMap(Bar::actionTimestamp, bar -> bar));
				JSONArray combBarArr = new JSONArray();
				barBufQMap.get(farMonth).forEach(bar -> {
					Bar nearBar = timeBarMap.get(bar.actionTimestamp());
					if(Objects.nonNull(nearBar)) {
						combBarArr.add(compute(nearBar, bar));
					}
				});
				mrd.getDataMap().put(combName, combBarArr);
			} else if (contractMap.size() == 3) {
				List<Contract> contracts = contractMap.values().stream()
						.sorted((a,b) -> a.unifiedSymbol().compareTo(b.unifiedSymbol()))
						.toList();
				Contract near = contracts.get(0);
				Contract mid = contracts.get(1);
				Contract far = contracts.get(2);
				Map<Long, Bar> midTimeBarMap = barBufQMap.get(mid)
						.stream()
						.collect(Collectors.toMap(Bar::actionTimestamp, bar -> bar));
				Map<Long, Bar> farTimeBarMap = barBufQMap.get(far)
						.stream()
						.collect(Collectors.toMap(Bar::actionTimestamp, bar -> bar));
				JSONArray combBarArr = new JSONArray();
				barBufQMap.get(near).forEach(nearBar -> {
					Bar midBar = midTimeBarMap.get(nearBar.actionTimestamp());
					Bar farBar = farTimeBarMap.get(nearBar.actionTimestamp());
					if(Objects.nonNull(midBar) && Objects.nonNull(farBar)) {
						combBarArr.add(compute(nearBar, midBar, farBar));
					}
				});
				mrd.getDataMap().put("蝶式价差率", combBarArr);
			}
		}
		return mrd;
	}
	
	private JSONObject compute(Bar bar1, Bar bar2) {
		JSONObject json = new JSONObject();
		json.put("open", (bar1.openPrice() - bar2.openPrice()) / bar2.openPrice() * 100);
		json.put("low", (bar1.lowPrice() - bar2.lowPrice()) / bar2.lowPrice() * 100);
		json.put("high", (bar1.highPrice() - bar2.highPrice()) / bar2.highPrice() * 100);
		json.put("close", (bar1.closePrice() - bar2.closePrice()) / bar2.closePrice() * 100);
		json.put("timestamp", bar1.actionTimestamp());
		return json;
	}
	
	private JSONObject compute(Bar near, Bar mid, Bar far) {
		JSONObject json = new JSONObject();
		json.put("open", (near.openPrice() / mid.openPrice() - mid.openPrice() / far.openPrice()) * 100);
		json.put("low", (near.lowPrice() / mid.lowPrice() - mid.lowPrice() / far.lowPrice()) * 100);
		json.put("high", (near.highPrice() / mid.highPrice() - mid.highPrice() / far.highPrice()) * 100);
		json.put("close", (near.closePrice() / mid.closePrice() - mid.closePrice() / far.closePrice()) * 100);
		json.put("timestamp", near.actionTimestamp());
		return json;
	}
}
