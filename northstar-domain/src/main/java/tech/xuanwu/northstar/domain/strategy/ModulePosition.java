package tech.xuanwu.northstar.domain.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import tech.xuanwu.northstar.strategy.api.ModuleEventBusAware;
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
public class ModulePosition implements TickDataAware, TransactionAware, ModuleEventBusAware{
	
	private StopLoss stopLoss;
	
	private ModuleEventBus meb;
	
	@Getter
	private double profit;
	
	@Getter
	private int volume;
	
	private int availableVol;
	
	@Getter
	private PositionDirectionEnum direction;
	
	private TradeField openTrade;
	
	private int factor;
	
	private TickField lastTick;
	
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
	}

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tick.getUnifiedSymbol(), openTrade.getContract().getUnifiedSymbol())) {
			return;
		}
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
			volume += fzn.vol - order.getTradedVolume();
			availableVol += fzn.vol - order.getTradedVolume();
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
				frozenMap.get(trade.getOriginOrderId()).vol -= trade.getVolume();
				volume -= trade.getVolume();
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
		frozenMap.put(id, new Frozen(id, vol));
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
		
		private String originOrderId;
		
		private int vol;
	}
}
