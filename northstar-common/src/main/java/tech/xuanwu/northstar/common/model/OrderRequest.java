package tech.xuanwu.northstar.common.model;

import lombok.Data;

/**
 * 下单委托
 * @author KevinHuangwl
 *
 */
@Data
public class OrderRequest {

	/**
	 * 合约名称
	 */
	private String contractSymbol;
	/**
	 * 委托价
	 */
	private String price;
	/**
	 * 止损价
	 */
	private String stopPrice;
	/**
	 * 委托数量
	 */
	private int volume;
	/**
	 * 买卖开平仓
	 */
	private TradeOperation tradeOpr;
	/**
	 * 账户ID
	 */
	private String accountId;
	
	public static enum TradeOperation {
		BK,	//买开
		BP,	//买平
		SK,	//卖开
		SP;	//卖平
	}
}
