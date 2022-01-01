package tech.quantit.northstar.strategy.api;

import java.util.UUID;

import org.slf4j.Logger;

import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;
import tech.quantit.northstar.strategy.api.model.Signal;
import tech.quantit.northstar.strategy.api.utils.PriceResolver;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public abstract class AbstractDealerPolicy implements DealerPolicy {

	protected ModuleState currentState;
	
	protected ModuleEventBus moduleEventBus;
	
	protected SubmitOrderReqField currentOrderReq;
	
	protected TickField lastTick;
	
	protected String bindedUnifiedSymbol;
	
	protected ContractField bindedContract;

	private String moduleName;
	
	protected Logger log;
	
	@Override
	public void onChange(ModuleState state) {
		currentState = state;
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_CANCELLED) {
			OrderField orderField = (OrderField) moduleEvent.getData();
			if(currentOrderReq != null && orderField.getOriginOrderId().equals(currentOrderReq.getOriginOrderId()) 
					&& currentState == ModuleState.PLACING_ORDER) {
				currentOrderReq = genTracingOrderReq(currentOrderReq);
				currentOrderReq = currentOrderReq.toBuilder()
						.setOriginOrderId(UUID.randomUUID().toString())
						.setActionTimestamp(lastTick.getActionTimestamp())
						.build();
				moduleEventBus.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, currentOrderReq));
				if(log.isInfoEnabled()) {					
					log.info("[{}->{}] 追单", getModuleName(), name());
				}
			}
		}
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_CONFIRMED) {
			if(log.isInfoEnabled()) {				
				log.info("[{}->{}] 订单确认", getModuleName(), name());
			}
		}
		if(moduleEvent.getEventType() == ModuleEventType.RETRY_RISK_ALERTED || moduleEvent.getEventType() == ModuleEventType.REJECT_RISK_ALERTED) {
			moduleEventBus.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CANCELLED, genCancelReq()));
			if(log.isInfoEnabled()) {				
				log.info("[{}->{}] 撤单", getModuleName(), name());
			}
		}
		if(moduleEvent.getEventType() == ModuleEventType.SIGNAL_CREATED) {
			Signal signal = (Signal) moduleEvent.getData();
			OffsetFlagEnum offsetFlag;
			DirectionEnum direction;
			switch(signal.getSignalOperation()) {
			case BUY_OPEN:
				offsetFlag = OffsetFlagEnum.OF_Open;
				direction = DirectionEnum.D_Buy;
				break;
			case BUY_CLOSE:
				offsetFlag = OffsetFlagEnum.OF_Close; //平今平昨问题会在下一阶段的Position校验中处理
				direction = DirectionEnum.D_Buy;
				break;
			case SELL_OPEN:
				offsetFlag = OffsetFlagEnum.OF_Open;
				direction = DirectionEnum.D_Sell;
				break;
			case SELL_CLOSE:
				offsetFlag = OffsetFlagEnum.OF_Close; //平今平昨问题会在下一阶段的Position校验中处理
				direction = DirectionEnum.D_Sell;
				break;
			default: 
				throw new IllegalStateException("未知信号：" + signal);
			}
			currentOrderReq = genOrderReq(direction, offsetFlag, signal.getSignalPrice(), signal.getTicksToStop());
			moduleEventBus.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, currentOrderReq));
			if(log.isInfoEnabled()) {				
				log.info("[{}->{}] 生成订单", getModuleName(), name());
			}
		}
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.moduleEventBus = moduleEventBus;
	}

	@Override
	public void onTick(TickField tick) {
		if(tick.getUnifiedSymbol().equals(bindedUnifiedSymbol)) {			
			lastTick = tick;
		}
	}
	
	@Override
	public String bindedContractSymbol() {
		return bindedUnifiedSymbol;
	}
	
	@Override
	public void setBindedContract(ContractField contract) {
		bindedContract = contract;
	}
	
	@Override
	public void setModuleName(String name) {
		this.moduleName = name;
		log = NorthstarLoggerFactory.getLogger(name, getClass());
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}

	private CancelOrderReqField genCancelReq() {
		return CancelOrderReqField.newBuilder()
				.setGatewayId(currentOrderReq.getGatewayId())
				.setOriginOrderId(currentOrderReq.getOriginOrderId())
				.build();
	}

	private SubmitOrderReqField genOrderReq(DirectionEnum direction, OffsetFlagEnum offsetFlag, double signalPrice, int ticksToStop) {
		PriceType priceType = FieldUtils.isClose(offsetFlag) ? closePriceType() : openPriceType();
		priceType = priceType == null ? PriceType.ANY_PRICE : priceType;	// 为防止子类没实现，默认使用市价，避免空指针异常
		double price = PriceResolver.getPrice(priceType, signalPrice, lastTick, FieldUtils.isBuy(direction));
		int factor = FieldUtils.isBuy(direction) ? 1 : -1;
		double stopPrice = ticksToStop > 0 ? lastTick.getLastPrice() - factor * ticksToStop * bindedContract.getPriceTick() : 0;
		
		return SubmitOrderReqField.newBuilder()
				.setOriginOrderId(UUID.randomUUID().toString())
				.setContract(bindedContract)
				.setDirection(direction)
				.setOffsetFlag(offsetFlag)
				.setStopPrice(stopPrice)
				.setPrice(price)
				.setVolume(tradeVolume())
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setActionTimestamp(lastTick.getActionTimestamp())
				.setMinVolume(1)
				.build();
	}
	
	// 按价格类型重新计算下单价格
	private SubmitOrderReqField genTracingOrderReq(SubmitOrderReqField originOrderReq) {
		PriceType priceType = FieldUtils.isClose(originOrderReq.getOffsetFlag()) ? closePriceType() : openPriceType();
		double tracePrice = PriceResolver.getPrice(priceType, originOrderReq.getPrice(), lastTick, FieldUtils.isBuy(originOrderReq.getDirection()));
		return originOrderReq.toBuilder()
				.setPrice(tracePrice)
				.setActionTimestamp(lastTick.getActionTimestamp())
				.build();
	}
	
	protected abstract PriceType openPriceType();
	
	protected abstract PriceType closePriceType();
	
	protected abstract int tradeVolume();
	
}
