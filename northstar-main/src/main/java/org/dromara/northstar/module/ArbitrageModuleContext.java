package org.dromara.northstar.module;

import java.util.Objects;

import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;

public class ArbitrageModuleContext extends ModuleContext implements IModuleContext{
	
	private final Logger logger;

	public ArbitrageModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleAccount moduleAccount, IContractManager contractMgr, IModuleRepository moduleRepo) {
		super(tradeStrategy, moduleDescription, moduleAccount, contractMgr, moduleRepo);
		logger = getLogger(getClass());
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现同时下单
	 */
	@Override
	public void submitOrderReq(SubmitOrderReq orderReq) {
		if(!isEnabled()) {
			logger.info("策略处于停用状态，忽略委托单");
			return;
		}

		logger.info("准备发单：{}，{} {} {}", orderReq.contract().name(), orderReq.originOrderId(), orderReq.direction(), orderReq.offsetFlag());
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
			moduleAccount.submitOrder(orderReq);
			orderReqMap.put(orderReq.originOrderId(), orderReq);
		} catch (Exception e) {
			logger.error("发单失败。原因：{}", e.getMessage());
		}
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现及时撤单
	 */
	@Override
	public void cancelOrder(CancelOrderReq cancelReq) {
		if(!orderReqMap.containsKey(cancelReq.originOrderId())) {
			logger.warn("找不到订单：{}", cancelReq.originOrderId());
			return;
		}
		logger.info("撤单：{}", cancelReq.originOrderId());
		if(moduleAccount.cancelOrder(cancelReq)) {
			orderReqMap.remove(cancelReq.originOrderId());
		}
	}

}
