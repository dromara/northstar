package tech.xuanwu.northstar.persistance.po;

import java.io.Serializable;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;

@Data
public class Position implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2766862232763568631L;

	private String positionId;
	private String accountId;
	private PositionDirectionEnum positionDirection;
	private int position;
	private int frozen;
	private int ydPosition;
	private int ydFrozen;
	private int tdPosition;
	private int tdFrozen;
	private double lastPrice;
	private double price;
	private double priceDiff;
	private double openPrice;
	private double openPriceDiff;
	private double positionProfit;
	private double positionProfitRatio;
	private double openPositionProfit;
	private double openPositionProfitRatio;
	private double useMargin;
	private double exchangeMargin;
	private double contractValue;
	private HedgeFlagEnum hedgeFlag;
	private String contractUnifiedSymbol;
	private String gatewayId;
	
	public static Position convertFrom(PositionField position) {
		Position po = new Position();
		BeanUtils.copyProperties(position.toBuilder(), po);
		po.setContractUnifiedSymbol(position.getContract().getUnifiedSymbol());
		return po;
	}
	
	public PositionField convertTo(ContractField contract) {
		PositionField.Builder pb = PositionField.newBuilder();
		BeanUtils.copyProperties(this, pb);
		pb.setContract(contract);
		return pb.build();
	}
}
