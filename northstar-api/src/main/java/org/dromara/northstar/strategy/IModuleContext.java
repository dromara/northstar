package org.dromara.northstar.strategy;

import java.util.List;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.TransactionAware;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.TradeGateway;

import xyz.redtorch.pb.CoreField.ContractField;

public interface IModuleContext extends IModuleStrategyContext, TickDataAware, BarDataAware, TransactionAware {
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getTradeStrategy();
	/**
	 * 获取交易网关
	 * @return
	 */
	TradeGateway getTradeGateway(ContractField contract);
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription);
	/**
	 * 绑定网关与合约
	 * @param gateway
	 * @param contracts
	 */
	void bindGatewayContracts(TradeGateway gateway, List<Contract> contracts);
	/**
	 * 设置模组
	 * @param module
	 */
	void setModule(IModule module);
	
}
