package tech.xuanwu.northstar.strategy.common.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.EntityAware;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.model.entity.DealRecordEntity;
import tech.xuanwu.northstar.strategy.common.model.entity.ModulePositionEntity;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class ModulePosition implements EntityAware<ModulePositionEntity>{

	private final String unifiedSymbol;

	private final PositionDirectionEnum positionDir;
	
	private final String openTradingDay;
	
	private final long openTime;
	
	private final double multiplier;
	
	private final ContractManager contractMgr;
	
	private double openPrice;
	
	private double stopLossPrice;
	
	private int volume;
	
	private double holdingProfit;
	
	public ModulePosition(TradeField trade, OrderField order, ContractManager contractMgr) {
		if(!StringUtils.equals(trade.getOriginOrderId(), order.getOriginOrderId())) {
			log.warn("委托明细：{}", order.toString());
			log.warn("成交明细：{}", trade.toString());
			throw new IllegalStateException("成交与委托不匹配");
		}
		this.unifiedSymbol = trade.getContract().getUnifiedSymbol();
		this.positionDir = trade.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		this.openPrice = trade.getPrice();
		this.stopLossPrice = order.getStopPrice();
		this.volume = trade.getVolume();
		this.multiplier = trade.getContract().getMultiplier();
		this.openTradingDay = trade.getTradingDay();
		this.openTime = trade.getTradeTimestamp();
		this.contractMgr = contractMgr;
	}
	
	public ModulePosition(ModulePositionEntity e, ContractManager contractMgr) {
		this.unifiedSymbol = e.getUnifiedSymbol();
		this.positionDir = e.getPositionDir();
		this.openPrice = e.getOpenPrice();
		this.stopLossPrice = e.getStopLossPrice();
		this.volume = e.getVolume();
		this.multiplier = e.getMultiplier();
		this.openTradingDay = e.getOpenTradingDay();
		this.openTime = e.getOpenTime();
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
	
	public Optional<ModulePositionEntity> onOpenTrade(TradeField trade) {
		checkMatch(trade.getContract().getUnifiedSymbol());
		if(OffsetFlagEnum.OF_Open != trade.getOffsetFlag()) {
			throw new IllegalStateException("传入了非开仓成交：" + trade.toString());
		}
		// 确保是同向开仓成交，忽略反向锁仓成交
		if(positionDir == PositionDirectionEnum.PD_Long && trade.getDirection() == DirectionEnum.D_Buy
				|| positionDir == PositionDirectionEnum.PD_Short && trade.getDirection() == DirectionEnum.D_Sell) {
			double originCost = openPrice * volume;
			double newCost = trade.getPrice() * trade.getVolume();
			openPrice = (originCost + newCost) / (volume + trade.getVolume());
			volume += trade.getVolume();
			return Optional.of(convertToEntity());
		}
		return Optional.empty();
	}
	
	public Optional<DealRecordEntity> onCloseTrade(TradeField trade){
		checkMatch(trade.getContract().getUnifiedSymbol());
		if(OffsetFlagEnum.OF_Open == trade.getOffsetFlag() || OffsetFlagEnum.OF_Unknown == trade.getOffsetFlag()) {
			throw new IllegalStateException("传入了非平仓成交：" + trade.toString());
		}
		// 确保是反向平仓
		if(positionDir == PositionDirectionEnum.PD_Long && trade.getDirection() == DirectionEnum.D_Sell
				|| positionDir == PositionDirectionEnum.PD_Short && trade.getDirection() == DirectionEnum.D_Buy) {
			if(volume < trade.getVolume()) {
				throw new IllegalStateException("成交数量大于持仓数量");
			}
			volume -= trade.getVolume();
			int factor = positionDir == PositionDirectionEnum.PD_Long ? 1 : -1;
			double closeProfit = factor * (trade.getPrice() - openPrice) * trade.getVolume() * multiplier;
			return Optional.of(DealRecordEntity.builder()
					.contractName(trade.getContract().getSymbol())
					.direction(positionDir)
					.tradingDay(openTradingDay)
					.dealTimestamp(openTime)
					.openPrice(openPrice)
					.closePrice(trade.getPrice())
					.volume(trade.getVolume())
					.closeProfit((int)closeProfit)
					.build());
		}
		return Optional.empty();
	}
	
	public boolean isLongPosition() {
		return positionDir == PositionDirectionEnum.PD_Long;
	}
	
	public boolean isShortPosition() {
		return positionDir == PositionDirectionEnum.PD_Short;
	}
	
	public String getUnifiedSymbol() {
		return unifiedSymbol;
	}
	
	public double getHoldingProfit() {
		return holdingProfit;
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
