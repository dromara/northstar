package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.ModuleCalculatedDataFrame;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreField.ContractField;

public interface IModuleContext extends TickDataAware, BarDataAware, TransactionAware {
	/**
	 * 获取模组名称
	 * @return
	 */
	String getModuleName();
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getTradeStrategy();
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleRuntimeDescription getRuntimeDescription();
	/**
	 * 绑定网关与合约
	 * @param gateway
	 * @param contracts
	 */
	void bindGatewayContracts(TradeGateway gateway, List<ContractField> contracts);
	/**
	 * 获取合约
	 * @param unifiedSymbol
	 * @return
	 */
	ContractField getContract(String unifiedSymbol);
	/**
	 * 委托下单（精简接口）
	 * @param gatewayId
	 * @param contract
	 * @param operation
	 * @param priceType
	 * @param volume
	 * @param price
	 * @return
	 */
	String submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price);
	/**
	 * 撤单
	 * @param cancelReq
	 */
	void cancelOrder(String originOrderId);
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
	/**
	 * 获取模组数据
	 * @return
	 */
	List<ModuleCalculatedDataFrame> getModuleData();
	
}
