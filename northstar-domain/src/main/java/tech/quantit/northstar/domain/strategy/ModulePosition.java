package tech.quantit.northstar.domain.strategy;

import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
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
@Builder
public class ModulePosition implements TickDataAware, EventDrivenComponent{
	
	private StopLoss stopLoss;
	
	protected ModuleEventBus meb;
	
	@Getter
	private double profit;
	
	@Getter
	private int volume;
	
	@Getter
	private PositionDirectionEnum direction;
	
	private String openTradingDay;
	
	private long openTime;
	@Getter
	private double openPrice;
	@Getter
	private ContractField contract;
	
	protected TickField lastTick;
	
	private final String moduleName;
	
	private ModuleTradeIntent ti;
	
	// 清仓回调
	private Consumer<ModuleDealRecord> clearoutCallback;
	// 该回调属于外部调用，不能在ModulePosition内部调用，否则就是死循环
	private Consumer<TradeField> positionChangeCallback;
	
	public ModulePosition merge(TradeField trade){
		if(volume > 0 && !StringUtils.equals(trade.getContract().getUnifiedSymbol(), contract.getUnifiedSymbol())) {
			String errMsg = String.format("持仓不匹配，不能叠加: %s | %s", trade.getContract().getUnifiedSymbol(), contract.getUnifiedSymbol());
			throw new IllegalStateException(errMsg);
		}
		// 开仓情况
		if(volume == 0) {
			openTradingDay = trade.getTradingDay();
			openTime = trade.getTradeTimestamp();
			openPrice = trade.getPrice();
			contract = trade.getContract();
			volume = trade.getVolume();
			direction = convertDir(trade.getDirection());
			return this;
		}
		
		// 加仓情况
		if(convertDir(trade.getDirection()) == direction && FieldUtils.isOpen(trade.getOffsetFlag())) {
			openPrice = (volume * openPrice + trade.getVolume() * trade.getPrice()) / (volume + trade.getVolume()); 
			volume += trade.getVolume();
			log.debug("[{}] 持仓变化，当前{}，共{}手，可用{}手", moduleName, direction, volume, available());
			return this;
		}
		
		// 减仓与锁仓情况
		if(convertDir(trade.getDirection()) != direction) {
			if(volume > trade.getVolume()) {				
				volume -= trade.getVolume();
			}else if(volume < trade.getVolume()) {
				direction = convertDir(trade.getDirection());
				volume = Math.abs(volume - trade.getVolume());
			} else {
				volume = 0;
				direction = PositionDirectionEnum.PD_Unknown;
			}
			log.debug("[{}] 持仓变化，当前{}，共{}手，可用{}手", moduleName, direction, volume, available());
			return this;
		}
		
		String thisInfo = String.format("当时仓位成交信息：方向%s，手数%s ", direction, trade.getVolume());
		String otherInfo = String.format("合并仓位成交信息：方向%s，开平%s，手数%s", trade.getDirection(), trade.getOffsetFlag(), trade.getVolume());
		throw new IllegalStateException("未知异常情况：" + thisInfo + otherInfo);
	}
	
	private PositionDirectionEnum convertDir(DirectionEnum dir) {
		if(FieldUtils.isBuy(dir))
			return PositionDirectionEnum.PD_Long;
		if(FieldUtils.isSell(dir))
			return PositionDirectionEnum.PD_Short;
		return PositionDirectionEnum.PD_Unknown;
	}
	
	public void clearout() {
		openTradingDay = "";
		openTime = 0;
		openPrice = 0;
		contract = null;
		volume = 0;
		direction = PositionDirectionEnum.PD_Unknown;
	}
	
	@Override
	public void onTick(TickField tick) {
		if(volume == 0 || !StringUtils.equals(tick.getUnifiedSymbol(), contract.getUnifiedSymbol())) {
			return;
		}
		lastTick = tick;
		// 更新持仓盈亏
		profit = factor() * (tick.getLastPrice() - openPrice) * volume * getContract().getMultiplier();
		// 监听止损
		if(available() > 0 && stopLoss != null && stopLoss.isTriggered(tick)) {
			SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(contract)
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
			ti = new ModuleTradeIntent(moduleName, this, orderReq, positionChangeCallback, clearoutCallback, partiallyTraded -> ti = null);
			meb.post(new ModuleEvent<>(ModuleEventType.STOP_LOSS, ti));
		}
	}
	
	private int factor() {
		return switch(direction) {
		case PD_Long -> 1;
		case PD_Short -> -1;
		default -> 0;
		};
	}
	
	private double multiplier() {
		return contract == null ? 0 : contract.getMultiplier();
	}
	
	private String unifiedSymbol() {
		return contract == null ? "" : contract.getUnifiedSymbol();
	}
	
	private double stopPrice() {
		return stopLoss == null ? 0 : stopLoss.getStopPrice();
	}
	
	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			if(StringUtils.equals(unifiedSymbol(), orderReq.getContract().getUnifiedSymbol()) 
					&& FieldUtils.isClose(orderReq.getOffsetFlag())) {
				//校验并冻结持仓
				if(available() < orderReq.getVolume()) {
					log.warn("模组可用持仓不足，订单被否决。可用数量[{}]，订单数量[{}]", available(), orderReq.getVolume());
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, orderReq));
				} else {					
					SubmitOrderReqField submitReq = orderReq.toBuilder()
							.setOffsetFlag(closeOffset())
							.setDirection(closingDirection())
							.build();
					ti = new ModuleTradeIntent(moduleName, this, submitReq, positionChangeCallback, clearoutCallback, partiallyTraded -> ti = null );
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, ti));
				}
			}
		}
	}
	
	public void stopLoss(double val) {
		if(val <= 0) {			
			stopLoss = null;
		} else if (val > openPrice && FieldUtils.isLong(direction) || val < openPrice && FieldUtils.isShort(direction)) {
			throw new IllegalArgumentException("止损价不能超过开仓价");
		}
		stopLoss = new StopLoss(direction, val);
	}
	
	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.meb = moduleEventBus;
		this.meb.register(this);
	}
	
	private OffsetFlagEnum closeOffset() {
		return StringUtils.equals(lastTick.getTradingDay(), openTradingDay) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_Close;
	}
	
	private DirectionEnum closingDirection() {
		return FieldUtils.isLong(direction) ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
	}
	
	public int available() {
		return ti == null ? volume : volume - ti.volume();
	}

	public ModulePositionInfo convertTo() {
		return ModulePositionInfo.builder()
				.moduleName(moduleName)
				.multiplier(multiplier())
				.openPrice(openPrice)
				.openTime(openTime)
				.openTradingDay(openTradingDay)
				.positionDir(direction)
				.stopLossPrice(stopPrice())
				.unifiedSymbol(unifiedSymbol())
				.volume(volume)
				.build();
	}
	
}
