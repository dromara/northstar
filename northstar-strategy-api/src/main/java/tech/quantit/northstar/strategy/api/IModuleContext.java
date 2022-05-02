package tech.quantit.northstar.strategy.api;

import java.util.Set;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.TransactionAware;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public interface IModuleContext extends TickDataAware, BarDataAware, TransactionAware {
	/**
	 * 获取模组名称
	 * @return
	 */
	String getModuleName();
	/**
	 * 绑定的合约集
	 * @return
	 */
	Set<ContractField> bindedContracts();
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getTradeStrategy();
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleDescription getModuleDescription();
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
	String submitOrderReq(String gatewayId, ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price);
	/**
	 * 委托下单
	 * @param orderReq
	 * @return
	 */
	String submitOrderReq(SubmitOrderReqField orderReq);
	/**
	 * 撤单
	 * @param cancelReq
	 */
	void cancelOrderReq(CancelOrderReqField cancelReq);
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
