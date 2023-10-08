package org.dromara.northstar.module;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.lang.Assert;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public class ArbitrageModuleContext extends ModuleContext implements IModuleContext{
	
	private ExecutorService exec = Executors.newFixedThreadPool(2);

	public ArbitrageModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo,
			ModuleLoggerFactory loggerFactory, BarMergerRegistry barMergerRegistry) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, moduleRepo, loggerFactory, barMergerRegistry);
	}

	/**
	 * 套利模组上下文为了实现同时发单，利用了多线程处理下单过程
	 */
	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		exec.execute(() -> {
			getLogger().info("下单交由子线程处理");
			if(!module.isEnabled()) {
				if(isReady()) {
					getLogger().info("策略处于停用状态，忽略委托单");
				}
				return;
			}
			TickField tick = latestTickMap.get(tradeIntent.getContract().getUnifiedSymbol());
			if(Objects.isNull(tick)) {
				getLogger().warn("没有TICK行情数据时，忽略下单请求");
				return;
			}
			getLogger().info("收到下单意图：{}", tradeIntent);
			tradeIntentMap.put(tradeIntent.getContract().getUnifiedSymbol(), tradeIntent);
			tradeIntent.setContext(this);
			tradeIntent.onTick(tick);	
		});
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现同时下单
	 */
	@Override
	public Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price) {
		if(!module.isEnabled()) {
			if(isReady()) {
				getLogger().info("策略处于停用状态，忽略委托单");
			}
			return Optional.empty();
		}
		TickField tick = latestTickMap.get(contract.getUnifiedSymbol());
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(volume > 0, "下单手数应该为正数。当前为" + volume);
		
		double orderPrice = priceType.resolvePrice(tick, operation, price);
		getLogger().info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
				tick.getActionDay(), LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER),
				contract.getUnifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		
		String id = UUID.randomUUID().toString();
		String gatewayId = getAccount(contract).accountId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		int factor = FieldUtils.directionFactor(direction);
		double plusPrice = module.getModuleDescription().getOrderPlusTick() * contract.getPriceTick(); // 超价设置
		PositionField pos = getAccount(contract).getPosition(OrderUtils.getClosingDirection(direction), contract.getUnifiedSymbol())
				.orElse(PositionField.newBuilder().setContract(contract).build());
		Tuple<OffsetFlagEnum, Integer> tuple = module.getModuleDescription().getClosingPolicy().resolve(operation, pos, volume);
		if(tuple.t1() == OffsetFlagEnum.OF_CloseToday) {
			PositionField updatePos = pos.toBuilder().setTdFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		} else if(tuple.t1() == OffsetFlagEnum.OF_CloseYesterday) {
			PositionField updatePos = pos.toBuilder().setYdFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		}
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(direction)
				.setOffsetFlag(tuple.t1())
				.setVolume(tuple.t2())		
				.setPrice(orderPrice + factor * plusPrice)	// 自动加上超价
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setActionTimestamp(System.currentTimeMillis())
				.setMinVolume(1)
				.build();
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
		} catch (Exception e) {
			getLogger().error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(orderReq.getContract().getUnifiedSymbol());
			return Optional.empty();
		}
		getLogger().info("发单：{}，{}", orderReq.getOriginOrderId(), LocalDateTime.now());
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
			getLogger().debug("找不到订单：{}", originOrderId);
			return;
		}
		getLogger().info("撤单：{}", originOrderId);
		ContractField contract = orderReqMap.get(originOrderId).getContract();
		Contract c = contractMgr.getContract(Identifier.of(contract.getContractId()));
		CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder()
				.setGatewayId(contract.getGatewayId())
				.setOriginOrderId(originOrderId)
				.build();
		module.getAccount(c).cancelOrder(cancelReq);
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		ModuleRuntimeDescription mrd = super.getRuntimeDescription(fullDescription);
		if(fullDescription) {
			if(contractMap.size() == 2) {
				Iterator<ContractField> it = contractMap.values().iterator();
				ContractField c1 = it.next();
				ContractField c2 = it.next();
				ContractField nearMonth = Long.valueOf(c1.getLastTradeDateOrContractMonth()) < Long.valueOf(c2.getLastTradeDateOrContractMonth()) ? c1 : c2;
				ContractField farMonth = c1 == nearMonth ? c2 : c1;
				String combName = String.format("%s-%s", nearMonth.getName(), farMonth.getName());
				Map<Long, BarField> timeBarMap = barBufQMap.get(nearMonth.getUnifiedSymbol())
						.stream()
						.collect(Collectors.toMap(BarField::getActionTimestamp, bar -> bar));
				JSONArray combBarArr = new JSONArray();
				barBufQMap.get(farMonth.getUnifiedSymbol()).forEach(bar -> {
					BarField nearBar = timeBarMap.get(bar.getActionTimestamp());
					if(Objects.nonNull(nearBar)) {
						combBarArr.add(compute(nearBar, bar));
					}
				});
				mrd.getDataMap().put(combName, combBarArr);
			} else {
				getLogger().warn("多于两个合约时，无法进行价差处理");
			}
		}
		return mrd;
	}
	
	private JSONObject compute(BarField bar1, BarField bar2) {
		JSONObject json = new JSONObject();
		json.put("open", (bar1.getOpenPrice() - bar2.getOpenPrice()) / bar2.getOpenPrice() * 100);
		json.put("low", (bar1.getLowPrice() - bar2.getLowPrice()) / bar2.getLowPrice() * 100);
		json.put("high", (bar1.getHighPrice() - bar2.getHighPrice()) / bar2.getHighPrice() * 100);
		json.put("close", (bar1.getClosePrice() - bar2.getClosePrice()) / bar2.getClosePrice() * 100);
		json.put("timestamp", bar1.getActionTimestamp());
		return json;
	}
}
