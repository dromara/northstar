package tech.xuanwu.northstar.strategy.common.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.EntityAware;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModulePositionPO;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class ModulePosition implements EntityAware<ModulePositionPO>{

	protected String unifiedSymbol;

	protected PositionDirectionEnum positionDir;
	
	protected double openPrice;
	
	protected double stopLossPrice;
	
	protected int volume;
	
	protected double multiplier;
	
	protected double holdingProfit;
	
	protected String openTradingDay;
	
	protected ContractManager contractMgr;
	
	public ModulePosition(ModulePositionPO e, ContractManager contractMgr) {
		this.unifiedSymbol = e.getUnifiedSymbol();
		this.positionDir = e.getPositionDir();
		this.openPrice = e.getOpenPrice();
		this.stopLossPrice = e.getStopLossPrice();
		this.volume = e.getVolume();
		this.multiplier = e.getMultiplier();
		this.openTradingDay = e.getOpenTradingDay();
		this.contractMgr = contractMgr;
	}
	
	public double updateProfit(TickField tick) {
		checkMatch(tick.getUnifiedSymbol());
		int factor = positionDir == PositionDirectionEnum.PD_Long ? 1 : -1;
		holdingProfit = factor * (tick.getLastPrice() - openPrice) * volume * multiplier;
		return holdingProfit;
	}
	
	public Optional<SubmitOrderReqField> triggerStopLoss(TickField tick) {
		checkMatch(tick.getUnifiedSymbol());
		if(stopLossPrice == 0) {
			return Optional.empty();
		}
		if(triggeredStopLoss(tick)) {
			SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(contractMgr.getContract(unifiedSymbol))
					.setDirection(getClosingDirection())
					.setVolume(volume)
					.setPrice(0) 											//市价专用
					.setOrderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)	//市价专用
					.setTimeCondition(TimeConditionEnum.TC_IOC)				//市价专用
					.setOffsetFlag(StringUtils.equals(tick.getTradingDay(), openTradingDay) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.build();
			log.info("生成止损单：{}，{}，{}，{}手", orderReq.getContract().getSymbol(), orderReq.getDirection(), orderReq.getOffsetFlag(), orderReq.getVolume());
			return Optional.of(orderReq);
		}
		return Optional.empty();
	}
	
	private DirectionEnum getClosingDirection() {
		return positionDir == PositionDirectionEnum.PD_Long ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
	}
	
	private boolean triggeredStopLoss(TickField tick) {
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
		if(!isMatch(unifiedSymbol)) {
			throw new IllegalStateException(this.unifiedSymbol + "与不匹配的数据更新：" + unifiedSymbol);
		}
	}
	
	@Override
	public ModulePositionPO convertToEntity() {
		return ModulePositionPO.builder()
				.unifiedSymbol(unifiedSymbol)
				.positionDir(positionDir)
				.openPrice(openPrice)
				.stopLossPrice(stopLossPrice)
				.volume(volume)
				.multiplier(multiplier)
				.build();
	}
}
