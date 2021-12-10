package tech.quantit.northstar.gateway.sim.trade;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Transient;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
@NoArgsConstructor
class SimPosition implements TickDataAware{
	
	@Transient
	private Runnable savingCallback;
	@Transient
	private CloseTradeRequest closeReq;
	
	private PositionDirectionEnum direction;
	
	private double openPrice;
	
	private double lastPrice;
	
	private String contractSymbol;
	
	private String contractName;
	
	private String unifiedSymbol;
	
	private double multipler;
	
	private int volume;
	
	private double marginRatio;
	
	private double settlePrice;
	
	private String gatewayId;
	
	private byte[] contractSrc;
	
	public SimPosition(TradeField trade) {
		if(FieldUtils.isClose(trade.getOffsetFlag())) {
			throw new IllegalStateException("平仓合约不能构造持仓");
		}
		openPrice = trade.getPrice();
		lastPrice = trade.getPrice();
		settlePrice = trade.getPrice();
		direction = FieldUtils.isBuy(trade.getDirection()) ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		contractSymbol = trade.getContract().getSymbol();
		contractName = trade.getContract().getFullName();
		multipler = trade.getContract().getMultiplier();
		marginRatio = FieldUtils.isBuy(trade.getDirection()) ? trade.getContract().getLongMarginRatio() : trade.getContract().getShortMarginRatio();
		volume = trade.getVolume();
		gatewayId = trade.getGatewayId();
		unifiedSymbol = trade.getContract().getUnifiedSymbol();
		contractSrc = trade.getContract().toByteArray();
	}
	
	@Override
	public void onTick(TickField tick) {
		lastPrice = tick.getLastPrice();
		settlePrice = tick.getSettlePrice();
	}

	public int availableVol() {
		return closeReq == null ? volume : volume - closeReq.frozenVol();
	}
	
	public double frozenMargin() {
		return settlePrice * volume * multipler * marginRatio;
	}
	
	public double profit() {
		int factor = FieldUtils.isLong(direction) ? 1 : -1;
		return factor * (lastPrice - openPrice) * volume * multipler;
	}
	
	public void merge(TradeField trade) {
		if(!StringUtils.equals(trade.getContract().getUnifiedSymbol(), unifiedSymbol)) {
			throw new IllegalArgumentException("不是相同合约不能合并持仓");
		}
		if(FieldUtils.isOpen(trade.getOffsetFlag())) {
			increasePosition(trade);
		} else if(FieldUtils.isClose(trade.getOffsetFlag())) {
			decreasePosition(trade);
		}
	}
	
	private void increasePosition(TradeField trade) {
		if(FieldUtils.isLong(direction) && FieldUtils.isBuy(trade.getDirection())
				|| FieldUtils.isShort(direction) && FieldUtils.isSell(trade.getDirection())) {
			openPrice = (volume * openPrice + trade.getVolume() * trade.getPrice()) / (volume + trade.getVolume());
			volume += trade.getVolume(); 
		}
	}
	
	private void decreasePosition(TradeField trade) {
		if(FieldUtils.isLong(direction) && FieldUtils.isSell(trade.getDirection())
				|| FieldUtils.isShort(direction) && FieldUtils.isBuy(trade.getDirection())) {
			volume -= trade.getVolume();
		}
	}
	
	public PositionField positionField() {
		ContractField contract = ContractField.newBuilder().build();
		try {
			contract = ContractField.parseFrom(contractSrc);
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalStateException("转译合约数据异常");
		}
		return PositionField.newBuilder()
				.setAccountId(gatewayId)
				.setGatewayId(gatewayId)
				.setPositionId(contractSymbol+"_"+direction+"_"+gatewayId)
				.setFrozen(volume - availableVol())
				.setLastPrice(lastPrice)
				.setPosition(volume)
				.setExchangeMargin(frozenMargin())
				.setOpenPositionProfit(profit())
				.setPositionProfit(profit())
				.setOpenPrice(openPrice)
				.setPositionDirection(direction)
				.setContract(contract)
				.build();
	}
}
