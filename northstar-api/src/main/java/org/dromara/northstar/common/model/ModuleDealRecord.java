package org.dromara.northstar.common.model;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易开平仓记录
 * @author KevinHuangwl
 *
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleDealRecord {
	/**
	 * 模组名称
	 */
	private String moduleName;
	/**
	 * 模组账户
	 */
	private String moduleAccountId;
	/**
	 * 平仓盈亏
	 */
	private double dealProfit;
	/**
	 * 手续费
	 */
	private double transactionFee;
	/**
	 * 开仓成交
	 */
	private byte[] openTrade;
	/**
	 * 平仓成交
	 */
	private byte[] closeTrade;
	
	public TradeField convertOpenTrade() {
		try {
			return TradeField.parseFrom(openTrade);
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public TradeField convertCloseTrade() {
		try {
			return TradeField.parseFrom(closeTrade);
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalStateException(e);
		}
	}
}
