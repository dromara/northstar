package tech.xuanwu.northstar.strategy.common.model;

import org.apache.commons.lang3.StringUtils;

import tech.xuanwu.northstar.strategy.common.model.entity.ModulePositionEntity;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TickField;

public class ModulePosition {

	private String unifiedSymbol;

	private PositionDirectionEnum positionDir;
	
	private double openPrice;
	
	private double stopLossPrice;
	
	private int volume;
	
	private int multiplier;
	
	private double holdingProfit;
	
	public ModulePosition(ModulePositionEntity e) {
		this.unifiedSymbol = e.getUnifiedSymbol();
		this.positionDir = e.getPositionDir();
		this.openPrice = e.getOpenPrice();
		this.stopLossPrice = e.getStopLossPrice();
		this.volume = e.getVolume();
		this.multiplier = e.getMultiplier();
	}
	
	public double onUpdate(TickField tick) {
		checkValidTick(tick);
		int factor = positionDir == PositionDirectionEnum.PD_Long ? 1 : -1;
		holdingProfit = factor * (tick.getLastPrice() - openPrice) * volume * multiplier;
		return holdingProfit;
	}
	
	public boolean triggerStopLoss(TickField tick) {
		checkValidTick(tick);
		if(stopLossPrice == 0) {
			return false;
		}
		return positionDir == PositionDirectionEnum.PD_Long && tick.getLastPrice() <= stopLossPrice
				|| positionDir == PositionDirectionEnum.PD_Short && tick.getLastPrice() >= stopLossPrice;
	}
	
	private void checkValidTick(TickField tick) {
		if(!StringUtils.equals(unifiedSymbol, tick.getUnifiedSymbol())) {
			throw new IllegalStateException(unifiedSymbol + "与不匹配的数据更新：" + tick.getUnifiedSymbol());
		}
	}
	
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
