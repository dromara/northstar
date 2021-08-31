package tech.xuanwu.northstar.strategy.common.model;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.common.EntityAware;
import tech.xuanwu.northstar.strategy.common.model.entity.ModulePositionEntity;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModulePosition implements EntityAware<ModulePositionEntity>{

	protected String unifiedSymbol;

	protected PositionDirectionEnum positionDir;
	
	protected double openPrice;
	
	protected double stopLossPrice;
	
	protected int volume;
	
	protected double multiplier;
	
	protected double holdingProfit;
	
	public ModulePosition(ModulePositionEntity e) {
		this.unifiedSymbol = e.getUnifiedSymbol();
		this.positionDir = e.getPositionDir();
		this.openPrice = e.getOpenPrice();
		this.stopLossPrice = e.getStopLossPrice();
		this.volume = e.getVolume();
		this.multiplier = e.getMultiplier();
	}
	
	public double updateProfit(TickField tick) {
		checkMatch(tick.getUnifiedSymbol());
		int factor = positionDir == PositionDirectionEnum.PD_Long ? 1 : -1;
		holdingProfit = factor * (tick.getLastPrice() - openPrice) * volume * multiplier;
		return holdingProfit;
	}
	
	public boolean triggerStopLoss(TickField tick) {
		checkMatch(tick.getUnifiedSymbol());
		if(stopLossPrice == 0) {
			return false;
		}
		return positionDir == PositionDirectionEnum.PD_Long && tick.getLastPrice() <= stopLossPrice
				|| positionDir == PositionDirectionEnum.PD_Short && tick.getLastPrice() >= stopLossPrice;
	}
	
	public boolean onTrade(TradeField trade) {
		checkMatch(trade.getContract().getUnifiedSymbol());
		if(OffsetFlagEnum.OF_Open == trade.getOffsetFlag()) {
			// 开仓成交
			if(positionDir == PositionDirectionEnum.PD_Long && trade.getDirection() == DirectionEnum.D_Buy
					|| positionDir == PositionDirectionEnum.PD_Short && trade.getDirection() == DirectionEnum.D_Sell) {
				double originCost = openPrice * volume;
				double newCost = trade.getPrice() * trade.getVolume();
				openPrice = (originCost + newCost) / (volume + trade.getVolume());
				volume += trade.getVolume();
				return true;
			}
		} else {
			// 平仓成交
			if(positionDir == PositionDirectionEnum.PD_Long && trade.getDirection() == DirectionEnum.D_Sell
					|| positionDir == PositionDirectionEnum.PD_Short && trade.getDirection() == DirectionEnum.D_Buy) {
				if(volume < trade.getVolume()) {
					throw new IllegalStateException("成交数量大于持仓数量");
				}
				volume -= trade.getVolume();
				return true;
			}
		}
		return false;
	}
	
	public boolean isEmpty() {
		return volume == 0;
	}
	
	public boolean isMatch(String unifiedSymbol) {
		return StringUtils.equals(unifiedSymbol, this.unifiedSymbol);
	}
	
	private void checkMatch(String unifiedSymbol) {
		if(!StringUtils.equals(this.unifiedSymbol, unifiedSymbol)) {
			throw new IllegalStateException(this.unifiedSymbol + "与不匹配的数据更新：" + unifiedSymbol);
		}
	}
	
	@Override
	public ModulePositionEntity convertToEntity() {
		return ModulePositionEntity.builder()
				.unifiedSymbol(unifiedSymbol)
				.positionDir(positionDir)
				.openPrice(openPrice)
				.stopLossPrice(stopLossPrice)
				.volume(volume)
				.multiplier(multiplier)
				.build();
	}
}
