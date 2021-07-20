package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Set;
import java.util.UUID;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@StrategicComponent("CTA交易策略")
public class CtaDealer implements Dealer {
	
	private String bindedUnifiedSymbol;
	
	private ModuleEventBus moduleEventBus;
	
	private CtaSignal currentSignal;
	
	private SubmitOrderReqField currentOrderReq;
	
	private ModuleAgent agent;
	
	private ContractManager contractMgr;
	
	private int openVol;
	
	@Override
	public void setModuleAgent(ModuleAgent agent) {
		this.agent = agent;
	}
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}

	@Override
	public void onEvent(ModuleEvent event) {
		if(event.getEventType() == ModuleEventType.SIGNAL_CREATED) {
			CtaSignal signal = (CtaSignal) event.getData();
			log.info("收到来自信号策略-[{}] 的交易信号：{}", currentSignal.getSignalClass().getSimpleName(), JSON.toJSONString(currentSignal));
			if(signal.getState().isOpen() && agent.getModuleState() != ModuleState.EMPTY) {				
				log.warn("模组并非空仓状态，忽略该信号");
				return;
			}
			if(!signal.getState().isOpen() && agent.getModuleState() != ModuleState.HOLDING) {
				log.warn("模组并非持仓状态，忽略该信号");
				return;
			}
			currentSignal = signal;
			
		} else if(event.getEventType() == ModuleEventType.ORDER_RETRY) {
			currentOrderReq = (SubmitOrderReqField) event.getData();
			log.info("模组追单：{}，{}, {}, {}, {}手, {}", currentOrderReq.getOriginOrderId(), currentOrderReq.getContract().getName(),
					currentOrderReq.getDirection(), currentOrderReq.getOffsetFlag(), currentOrderReq.getVolume(), currentOrderReq.getPrice());
			
		}
	}
	
	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.moduleEventBus = moduleEventBus;
	}

	//注意防止重复下单
	@Override
	public void onTick(TickField tick) {
		if(currentSignal != null) {
			log.info("交易策略生成订单");
			DirectionEnum direction = currentSignal.getState().isBuy() ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
			OffsetFlagEnum offsetFlag = currentSignal.getState().isOpen() ? OffsetFlagEnum.OF_Open : agent.getClosingOffsetFlag();
			// FIXME 未考虑反手处理
			// 按信号下单
			currentOrderReq = SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(contractMgr.getContract(tick.getUnifiedSymbol()))
					.setDirection(direction)
					.setOffsetFlag(offsetFlag)
					.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
					.setVolume(openVol)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setTimeCondition(TimeConditionEnum.TC_GFD)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setMinVolume(1)
					.setGatewayId(agent.getAccountGatewayId())
					.setPrice(currentSignal.getState().isBuy() ? tick.getAskPrice(0) : tick.getBidPrice(0))
					.build();
			currentSignal = null;
			emitOrder();
			
		} else if(agent.getModuleState() == ModuleState.TRACING_ORDER) {
			log.info("交易策略改价追单");
			// 按前订单改价
			currentOrderReq = SubmitOrderReqField.newBuilder(currentOrderReq)
					.setOriginOrderId(UUID.randomUUID().toString())
					.setPrice(tick.getLastPrice())
					.build();
			emitOrder();
		}
	}
	
	private void emitOrder() {
		log.info("交易策略把交易信号转成下单请求");
		moduleEventBus.post(ModuleEvent.builder()
				.eventType(ModuleEventType.ORDER_REQ_CREATED)
				.data(currentOrderReq)
				.build());
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		this.openVol = initParams.openVol;
	}
	
	public static class InitParams extends DynamicParams{

		@Label(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Label(value="开仓手数", order = 20)
		private int openVol = 1;
	}

	@Override
	public void setContractManager(ContractManager contractMgr) {
		this.contractMgr = contractMgr;
	}

}
