package tech.quantit.northstar.strategy.api;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.Signal;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public abstract class AbstractDealerPolicy implements DealerPolicy {

	protected ModuleState currentState;
	
	protected ModuleEventBus moduleEventBus;
	
	protected SubmitOrderReqField currentOrderReq;
	
	protected TickField lastTick;
	
	protected String bindedUnifiedSymbol;
	
	@Override
	public void onChange(ModuleState state) {
		currentState = state;
	}

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_CANCELLED) {
			OrderField orderField = (OrderField) moduleEvent.getData();
			if(currentOrderReq != null && orderField.getOriginOrderId().equals(currentOrderReq.getOriginOrderId()) 
					&& currentState == ModuleState.PENDING_ORDER) {
				moduleEventBus.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, genTracingOrderReq(currentOrderReq)));
				log.info("[{}] 追单", name());
			}
		}
		if(moduleEvent.getEventType() == ModuleEventType.ORDER_CONFIRMED) {
			currentOrderReq = (SubmitOrderReqField) moduleEvent.getData();
			log.info("[{}] 订单确认", name());
		}
		if(moduleEvent.getEventType() == ModuleEventType.RETRY_RISK_ALERTED) {
			moduleEventBus.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CANCELLED, genCancelReq()));
			log.info("[{}] 撤单", name());
		}
		if(moduleEvent.getEventType() == ModuleEventType.SIGNAL_CREATED) {
			Signal signal = (Signal) moduleEvent.getData();
			OffsetFlagEnum offsetFlag;
			DirectionEnum direction;
			switch(signal) {
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
			
			moduleEventBus.post(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, genOrderReq(direction, offsetFlag)));
			log.info("[{}] 生成订单", name());
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

	private CancelOrderReqField genCancelReq() {
		return CancelOrderReqField.newBuilder()
				.setGatewayId(currentOrderReq.getGatewayId())
				.setOriginOrderId(currentOrderReq.getOriginOrderId())
				.build();
	}

	protected abstract SubmitOrderReqField genOrderReq(DirectionEnum direction, OffsetFlagEnum offsetFlag);
	
	protected abstract SubmitOrderReqField genTracingOrderReq(SubmitOrderReqField originOrderReq);
}
