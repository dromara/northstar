package tech.xuanwu.northstar.service;

import java.util.List;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreField.GatewayField;

/**
 * 交易服务
 * @author kevinhuangwl
 *
 */
public interface ITradeService {

	/**
	 * 委托下单
	 * @param gatewayName
	 * @param symbol
	 * @param price
	 * @param volume
	 * @param dir
	 * @param dealType
	 * @return
	 */
	String submitOrder(String gatewayName, String symbol, double price, int volume, DirectionEnum dir,
			OffsetFlagEnum dealType, OrderPriceTypeEnum priceType, TimeConditionEnum timeCondition);
	
	/**
	 * 撤单
	 * @param gatewayName
	 * @param orderId
	 */
	boolean cancelOrder(String gatewayName, String orderId);
	
	/**
	 * 获取可交易账户列表
	 * @return
	 */
	List<GatewayField> getTradableAccountList();
	
	/**
	 * 获取
	 * @return
	 */
	List<byte[]> getContracts();
}
