package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.StringUtils;
import org.dromara.northstar.common.exception.NoSuchElementException;
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
	}

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
			contractReqCounterMap.clear();
			prepareCounters();
		}
	}
	
	private void prepareCounters() {
		module.getModuleDescription().getModuleAccountSettingsDescription().stream().flatMap(mard -> mard.getBindedContracts().stream())
			.forEach(contract -> contractReqCounterMap.put(contract.getUnifiedSymbol(), new AtomicInteger()));
	}

	@Override
	public void doFilter(SubmitOrderReqField orderReq) {
		if(!contractReqCounterMap.containsKey(orderReq.getContract().getUnifiedSymbol())) {
			throw new NoSuchElementException(orderReq.getContract().getUnifiedSymbol());
		}
		if(contractReqCounterMap.get(orderReq.getContract().getUnifiedSymbol()).getAndIncrement() > MAX_ORDER_REQ_PER_DAY) {
			module.getModuleContext().getLogger().warn("模组 [{}] 触发 [{}] 合约的日内免费申报上限。自动停用模组。", module.getName(), orderReq.getContract().getName());
			module.setEnabled(false);
		}
		module.getAccount(orderReq.getContract()).submitOrder(orderReq);
	}

}
