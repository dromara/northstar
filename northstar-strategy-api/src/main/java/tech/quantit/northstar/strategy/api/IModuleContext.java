package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.ContractField;

public interface IModuleContext extends IModuleStrategyContext, TickDataAware, BarDataAware, TransactionAware {
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getTradeStrategy();
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
	void bindGatewayContracts(TradeGateway gateway, List<ContractField> contracts);
	/**
	 * 设置模组
	 * @param module
	 */
	void setModule(IModule module);
	/**
	 * 停用模组策略
	 * @param enabled
	 */
	void disabledModule();
	
}
