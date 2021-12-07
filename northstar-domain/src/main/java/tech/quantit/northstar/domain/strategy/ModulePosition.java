package tech.quantit.northstar.domain.strategy;

import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.TickDataAware;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import tech.quantit.northstar.strategy.api.model.ModulePositionInfo;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组持仓
 * 负责浮盈计算
 * 负责平仓计算，包括持仓冻结与解冻
 * 负责监听止损
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModulePosition implements TickDataAware, EventDrivenComponent{
	
	private StopLoss stopLoss;
	
	protected ModuleEventBus meb;
	
	@Getter
	private double profit;
	
	@Getter
	private int volume;
	
	@Getter
	private PositionDirectionEnum direction;
	
	private TradeField trade;
	
	private final int factor;
	
	protected TickField lastTick;
	
	private final String governedByModule;
	
	private ModuleTradeIntent ti;
	
	// 清仓回调
	private Consumer<ModuleDealRecord> clearoutCallback;
	// 该回调属于外部调用，不能在ModulePosition内部调用，否则就是死循环
	private Consumer<ModulePosition> positionChangeCallback;
	
	public ModulePosition(ModulePositionInfo info, ContractField contract, Consumer<ModulePosition> positionChangeCallback,
			Consumer<ModuleDealRecord> clearoutCallback) {
		this(info.getModuleName(), TradeField.newBuilder()
				.setContract(contract)
				.setPrice(info.getOpenPrice())
				.setTradeTimestamp(info.getOpenTime())
				.setTradingDay(info.getOpenTradingDay())
				.setDirection(FieldUtils.isLong(info.getPositionDir()) ? DirectionEnum.D_Buy : DirectionEnum.D_Sell)
				.setVolume(info.getVolume())
				.build(), info.getStopLossPrice(), positionChangeCallback, clearoutCallback);
	}
	
	public ModulePosition(String moduleName, TradeField trade, double stopPrice, Consumer<ModulePosition> positionChangeCallback,
			Consumer<ModuleDealRecord> clearoutCallback) {
		this.governedByModule = moduleName;
		this.direction = trade.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		this.stopLoss = new StopLoss(direction, stopPrice);
		this.trade = trade;
		this.factor = FieldUtils.isLong(direction) ? 1 : -1;
		this.volume = trade.getVolume();
		this.clearoutCallback = clearoutCallback;
		this.positionChangeCallback = positionChangeCallback;
	}
	
	public ModulePosition merge(ModulePosition mp){
		if(!StringUtils.equals(trade.getContract().getUnifiedSymbol(), mp.trade.getContract().getUnifiedSymbol())) {
			String errMsg = String.format("持仓不匹配，不能叠加: %s | %s", trade.getContract().getUnifiedSymbol(), mp.trade.getContract().getUnifiedSymbol());
			throw new IllegalStateException(errMsg);
		}
		// 加仓情况
		if(trade.getDirection() == mp.trade.getDirection() && trade.getOffsetFlag() == mp.trade.getOffsetFlag()) {
			double mergePrice = (volume * trade.getPrice() + mp.volume * mp.trade.getPrice()) / (volume + mp.volume); 
			volume += mp.trade.getVolume();
			trade = trade.toBuilder()
					.setVolume(volume)
					.setPrice(mergePrice)
					.build();
			return this;
		}
		
		// 减仓与锁仓情况
		if(trade.getDirection() != mp.trade.getDirection()) {
			if(volume > mp.trade.getVolume()) {				
				volume -= mp.trade.getVolume();
				trade = trade.toBuilder()
						.setVolume(volume)
						.build();
			}else if(volume < mp.trade.getVolume()) {
				direction = FieldUtils.isBuy(mp.trade.getDirection()) ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
				volume = Math.abs(volume - mp.trade.getVolume());
				trade = mp.trade.toBuilder()
						.setVolume(volume)
						.build();
			} else {
				volume = 0;
				direction = PositionDirectionEnum.PD_Unknown;
			}
			return this;
		}
		
		String thisInfo = String.format("当时仓位成交信息：方向%s，开平%s； ", trade.getDirection(), trade.getOffsetFlag());
		String otherInfo = String.format("合并仓位成交信息：方向%s，开平%s", mp.trade.getDirection(), mp.trade.getOffsetFlag());
		throw new IllegalStateException("未知异常情况：" + thisInfo + otherInfo);
	}
	
	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tick.getUnifiedSymbol(), trade.getContract().getUnifiedSymbol())) {
			return;
		}
		lastTick = tick;
		// 更新持仓盈亏
		profit = factor * (tick.getLastPrice() - trade.getPrice()) * volume * trade.getContract().getMultiplier();
		// 监听止损
		if(available() > 0 && stopLoss.isTriggered(tick)) {
			SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(contract())
					.setDirection(closingDirection())
					.setVolume(available())
					.setPrice(0) 											
					.setOrderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)	
					.setTimeCondition(TimeConditionEnum.TC_IOC)				
					.setOffsetFlag(closeOffset())
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.build();
			ti = new ModuleTradeIntent(governedByModule, this, orderReq, positionChangeCallback, clearoutCallback, partiallyTraded -> ti = null);
			meb.post(new ModuleEvent<>(ModuleEventType.STOP_LOSS, ti));
		}
	}
	
	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			if(StringUtils.equals(trade.getContract().getUnifiedSymbol(), orderReq.getContract().getUnifiedSymbol()) 
					&& closingDirection() == orderReq.getDirection() && FieldUtils.isClose(orderReq.getOffsetFlag())) {
				//校验并冻结持仓
				if(available() < orderReq.getVolume()) {
					log.warn("模组可用持仓不足，订单被否决。可用数量[{}]，订单数量[{}]", available(), orderReq.getVolume());
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, orderReq));
				} else {					
					SubmitOrderReqField submitReq = orderReq.toBuilder()
							.setOffsetFlag(closeOffset())
							.setDirection(closingDirection())
							.build();
					ti = new ModuleTradeIntent(governedByModule, this, submitReq, positionChangeCallback, clearoutCallback, partiallyTraded -> ti = null );
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, ti));
				}
			}
		}
	}
	
	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.meb = moduleEventBus;
	}
	
	public ContractField contract() {
		return trade.getContract();
	}
	
	private OffsetFlagEnum closeOffset() {
		return StringUtils.equals(lastTick.getTradingDay(), trade.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_Close;
	}
	
	private DirectionEnum closingDirection() {
		return FieldUtils.isLong(direction) ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
	}
	
	public int available() {
		return ti == null ? volume : volume - ti.volume();
	}

	public double openPrice() {
		return trade.getPrice();
	}
	
	public ModulePositionInfo convertTo() {
		return ModulePositionInfo.builder()
				.moduleName(governedByModule)
				.multiplier(contract().getMultiplier())
				.openPrice(trade.getPrice())
				.openTime(trade.getTradeTimestamp())
				.openTradingDay(trade.getTradingDay())
				.positionDir(direction)
				.stopLossPrice(stopLoss.getStopPrice())
				.unifiedSymbol(contract().getUnifiedSymbol())
				.volume(volume)
				.build();
	}
	
}
