package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@StrategicComponent("示例交易策略")
public class SampleDealer implements Dealer {
	
	private String bindedUnifiedSymbol;
	
	private CtaSignal currentSignal;
	
	private SubmitOrderReqField currentOrderReq;
	
	private ContractManager contractMgr;
	
	private int openVol;
	
	private StrategyModule module;
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public void onSignal(Signal signal, StrategyModule module) {
		currentSignal = (CtaSignal) signal;
		this.module = module;
	}

	//注意防止重复下单
	@Override
	public Optional<SubmitOrderReqField> onTick(TickField tick) {
		if(currentSignal != null) {
			log.info("交易策略生成订单");
			DirectionEnum direction = currentSignal.getState().isBuy() ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
			OffsetFlagEnum offsetFlag = currentSignal.getState().isOpen() ? OffsetFlagEnum.OF_Open : module.getModulePosition().getClosingOffsetFlag(module.getTradingDay());
			ContractField contract = contractMgr.getContract(tick.getUnifiedSymbol());
			// FIXME 未考虑反手处理
			// 按信号下单
			currentOrderReq = SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(contract)
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
					.setGatewayId(module.getGateway().getGatewaySetting().getGatewayId())
					.setPrice(currentSignal.getState().isBuy() ? tick.getBidPrice(0) : tick.getAskPrice(0))
					.build();
			currentSignal = null;
			return Optional.of(currentOrderReq);
			
		} else {
			log.info("交易策略改价追单");
			// 按前订单改价
			currentOrderReq = SubmitOrderReqField.newBuilder(currentOrderReq)
					.setOriginOrderId(UUID.randomUUID().toString())
					.setPrice(tick.getLastPrice())
					.build();
			return Optional.of(currentOrderReq);
		}
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
