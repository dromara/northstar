package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.StringUtils;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.OrderRequestFilter;

import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;


public class DefaultOrderFilter implements OrderRequestFilter {

	private static final int MAX_ORDER_REQ_PER_DAY = 4000; //按CTP日内免申报费的上限设置
	
	private IModule module;
	
	private String tradingDay;
	
	private Map<String, AtomicInteger> contractReqCounterMap = new HashMap<>();
	
	public DefaultOrderFilter(IModule module) {
		this.module = module;
		prepareCounters();
	}

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
			prepareCounters();
		}
	}
	
	private void prepareCounters() {
		contractReqCounterMap.clear();
		module.getModuleDescription().getModuleAccountSettingsDescription().stream().flatMap(mad -> mad.getBindedContracts().stream())
			.forEach(contract -> contractReqCounterMap.put(contract.getUnifiedSymbol(), new AtomicInteger()));
	}

	@Override
	public void doFilter(SubmitOrderReqField orderReq) {
		module.getModuleContext().getLogger().info("默认订单过滤器正进行风控过滤");
		if(!contractReqCounterMap.containsKey(orderReq.getContract().getUnifiedSymbol())) {
			throw new NoSuchElementException(String.format("模组没包含合约：%s。 可选合约：%s", orderReq.getContract().getUnifiedSymbol(), contractReqCounterMap.keySet()));
		}
		String unifiedSymbol = orderReq.getContract().getUnifiedSymbol();
		if(module.getModuleContext().getLogger().isDebugEnabled()) {
			module.getModuleContext().getLogger().debug("[{}] 当天 [{}] 合约的剩余发单次数为：{}", tradingDay, unifiedSymbol, contractReqCounterMap.get(unifiedSymbol).get());
		}
		if(contractReqCounterMap.get(unifiedSymbol).getAndIncrement() > MAX_ORDER_REQ_PER_DAY) {
			module.getModuleContext().getLogger().warn("模组 [{}] 触发 [{}] 合约的日内免费申报上限。自动停用模组。", module.getName(), orderReq.getContract().getName());
			module.setEnabled(false);
			throw new TradeException("中止委托发单");
		}
	}

}
