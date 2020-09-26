package tech.xuanwu.northstar.persistance.po;

import java.io.Serializable;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
public class Trade implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2766862232763568631L;

	private String tradeId;
	private String accountId; // 账户ID
	private DirectionEnum direction;
	private OffsetFlagEnum offsetFlag;
	private HedgeFlagEnum hedgeFlag;
	private double price;
	private int volume;
	private TradeTypeEnum tradeType;
	private PriceSourceEnum priceSource;
	private String tradingDay;
	private String tradeDate;
	private String tradeTime;
	private long tradeTimestamp;
	private String contractUnifiedSymbol;
	private String gatewayId;
	
	public static Trade convertFrom(TradeField trade) {
		Trade po = new Trade();
		BeanUtils.copyProperties(trade.toBuilder(), po);
		po.setContractUnifiedSymbol(trade.getContract().getUnifiedSymbol());
		return po;
	}
	
	public TradeField convertTo(ContractField contract) {
		TradeField.Builder tb = TradeField.newBuilder();
		BeanUtils.copyProperties(this, tb);
		tb.setContract(contract);
		return tb.build();
	}
}
