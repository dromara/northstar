package tech.xuanwu.northstar.domain.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.api.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.api.TickDataAware;
import tech.xuanwu.northstar.strategy.api.TransactionAware;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
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
public class ModulePosition implements TickDataAware, TransactionAware, EventDrivenComponent{
	
	private StopLoss stopLoss;
	
	protected ModuleEventBus meb;
	
	@Getter
	private double profit;
	
	@Getter
	private int volume;
	
	protected int availableVol;
	
	@Getter
	private PositionDirectionEnum direction;
	
	private TradeField openTrade;
	
	private int factor;
	
	protected TickField lastTick;
	
	private Map<String, Frozen> frozenMap = new HashMap<>();
	
	// 清仓回调
	private Consumer<ModulePosition> clearoutCallback;
	
	public ModulePosition(TradeField trade, double stopPrice) {
		this(trade, stopPrice, null);
	}
	
	public ModulePosition(TradeField trade, double stopPrice, Consumer<ModulePosition> clearoutCallback) {
		this.direction = trade.getDirection() == DirectionEnum.D_Buy ? PositionDirectionEnum.PD_Long : PositionDirectionEnum.PD_Short;
		this.stopLoss = new StopLoss(direction, stopPrice);
		this.openTrade = trade;
		this.factor = FieldUtils.isLong(direction) ? 1 : -1;
		this.volume = trade.getVolume();
		this.availableVol = trade.getVolume();
		this.clearoutCallback = clearoutCallback;
	}

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tick.getUnifiedSymbol(), openTrade.getContract().getUnifiedSymbol())) {
			return;
		}
		lastTick = tick;
		// 更新持仓盈亏
		profit = factor * (tick.getLastPrice() - openTrade.getPrice()) * volume * openTrade.getContract().getMultiplier();
		// 监听止损
		if(availableVol > 0 && stopLoss.isTriggered(tick)) {
			meb.post(new ModuleEvent<>(ModuleEventType.STOP_LOSS, closePosition(availableVol, 0)));
		}
	}
	
	@Override
	public void onOrder(OrderField order) {
		if(frozenMap.keySet().contains(order.getOriginOrderId()) && order.getTotalVolume() == frozenMap.get(order.getOriginOrderId()).vol
				&& order.getOrderStatus() == OrderStatusEnum.OS_Canceled) {
			// 处理全部撤单与部分撤单
			Frozen fzn = frozenMap.remove(order.getOriginOrderId());
			fzn.consume(order);
			availableVol += fzn.vol;
		}
	}

	@Override
	public void onTrade(TradeField trade) {
		if(frozenMap.keySet().contains(trade.getOriginOrderId())) {
			// 全部成交情况
			if(trade.getVolume() == frozenMap.get(trade.getOriginOrderId()).vol) {
				frozenMap.remove(trade.getOriginOrderId());
				volume -= trade.getVolume();
				if(clearoutCallback != null) {
					clearoutCallback.accept(this);
				}
			} 
			// 部分成交情况
			else {
				volume -= trade.getVolume();
			}
		}
	}
	
	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_REQ_CREATED) {
			SubmitOrderReqField orderReq = (SubmitOrderReqField) moduleEvent.getData();
			if(StringUtils.equals(openTrade.getContract().getUnifiedSymbol(), orderReq.getContract().getUnifiedSymbol()) 
					&& closingDirection() == orderReq.getDirection() && FieldUtils.isClose(orderReq.getOffsetFlag())) {
				//校验并冻结持仓
				if(availableVol < orderReq.getVolume()) {
					log.warn("模组可用持仓不足，订单被否决。可用数量[{}]，订单数量[{}]", availableVol, orderReq.getVolume());
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_RETAINED, orderReq));
				} else {					
					meb.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_ACCEPTED, closePosition(orderReq.getVolume(), orderReq.getPrice())));
				}
			}
		}
	}
	
	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.meb = moduleEventBus;
	}
	
	public ContractField contract() {
		return openTrade.getContract();
	}
	
	private OffsetFlagEnum closeOffset() {
		return StringUtils.equals(lastTick.getTradingDay(), openTrade.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_Close;
	}
	
	private DirectionEnum closingDirection() {
		return FieldUtils.isLong(direction) ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
	}
	
	public SubmitOrderReqField closePosition(int vol, double price) {
		String id = UUID.randomUUID().toString();
		frozenMap.put(id, new Frozen(vol));
		availableVol -= vol;
		return SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract())
				.setDirection(closingDirection())
				.setVolume(vol)
				.setPrice(price) 											
				.setOrderPriceType(price==0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)	
				.setTimeCondition(price==0 ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)				
				.setOffsetFlag(closeOffset())
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.build();
	}

	public double openPrice() {
		return openTrade.getPrice();
	}
	
	@AllArgsConstructor
	class Frozen{
		
		private int vol;
		
		public void consume(OrderField order) {
			vol -= order.getTradedVolume();
		}
	}
}
