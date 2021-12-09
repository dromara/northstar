package tech.quantit.northstar.gateway.sim.trade;

import org.springframework.data.annotation.Transient;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Data
@NoArgsConstructor
public class SimPosition implements TickDataAware{
	
	@Transient
	private Runnable savingCallback;
	@Transient
	private CloseTradeRequest closeReq;
	
	private PositionDirectionEnum direction;
	
	private double openPrice;
	
	private double lastPrice;
	
	private String contractSymbol;
	
	private String contractName;
	
	private double multipler;
	
	private int volume;
	
	private double marginRatio;
	
	private double settlePrice;
	
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
}
