package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public interface IModuleStrategyContext {
	/**
	 * 获取模组名称
	 * @return
	 */
	String getModuleName();
	/**
	 * 获取模组缓存数据
	 * @return
	 */
	List<BarField> getModuleBufBars(String unifiedSymbol);
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
	 * 获取模组状态
	 * @return
	 */
	ModuleState getState();
}
