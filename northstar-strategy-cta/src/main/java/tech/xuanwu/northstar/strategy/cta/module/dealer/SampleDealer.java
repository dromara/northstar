package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
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
	
	private OffsetFlagEnum currentOffset;
	
	private SubmitOrderReqField currentOrderReq;
	
	private ContractManager contractMgr;
	
	private int openVol;
	
	private StrategyModule module;
	
	private String priceTypeStr;
	
	private int overprice;
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public void onSignal(Signal signal, OffsetFlagEnum offsetFlag) {
		currentSignal = (CtaSignal) signal;
		currentOffset = offsetFlag;
	}

	//注意防止重复下单
	@Override
	public Optional<SubmitOrderReqField> onTick(TickField tick) {
		if(currentSignal != null) {
			log.info("交易策略生成订单");
			DirectionEnum direction = currentSignal.getState().isBuy() ? DirectionEnum.D_Buy : DirectionEnum.D_Sell;
			ContractField contract = contractMgr.getContract(tick.getUnifiedSymbol());
			// 按信号下单
			currentOrderReq = SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(contract)
					.setDirection(direction)
					.setOffsetFlag(currentOffset)
					.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
					.setVolume(openVol)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setTimeCondition(TimeConditionEnum.TC_GFD)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setMinVolume(1)
					.setGatewayId(module.getGateway().getGatewaySetting().getGatewayId())
					.setStopPrice(currentSignal.getStopPrice())
					.setPrice(resolvePrice(currentSignal, tick))
					.build();
			currentSignal = null;
			currentOffset = null;
			return Optional.of(currentOrderReq);
			
		} else {
			log.info("交易策略改价追单");
			int factor = currentOrderReq.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
			ContractField contract = contractMgr.getContract(tick.getUnifiedSymbol());
			double priceTick = contract.getPriceTick();
			// 按前订单改价
			currentOrderReq = SubmitOrderReqField.newBuilder(currentOrderReq)
					.setOriginOrderId(UUID.randomUUID().toString())
					.setPrice(tick.getLastPrice() + factor * priceTick * overprice)
					.build();
			return Optional.of(currentOrderReq);
		}
	}
	
	public double resolvePrice(CtaSignal currentSignal, TickField tick) {
		int factor = currentSignal.getState().isBuy() ? 1 : -1;
		ContractField contract = contractMgr.getContract(tick.getUnifiedSymbol());
		double priceTick = contract.getPriceTick();
		double orderPrice = 0;
		switch(priceTypeStr) {
		case "对手价":
			double oppPrice = currentSignal.getState().isBuy() ? tick.getAskPrice(0) : tick.getBidPrice(0);
			orderPrice = oppPrice + factor * priceTick * overprice;
			log.info("当前使用[对手价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", oppPrice, overprice, orderPrice);
			break;
		case "市价":
			orderPrice = currentSignal.getState().isBuy() ? tick.getUpperLimit() : tick.getLowerLimit();
			log.info("当前使用[市价]成交，最终下单价：{}", orderPrice);
			break;
		case "最新价":
			orderPrice = tick.getLastPrice() + factor * priceTick * overprice;
			log.info("当前使用[最新价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", tick.getLastPrice(), overprice, orderPrice);
			break;
		case "排队价":
			orderPrice = currentSignal.getState().isBuy() ? tick.getBidPrice(0) : tick.getAskPrice(0);
			log.info("当前使用[排队价]成交，基础价为：{}，忽略超价，最终下单价：{}", orderPrice, orderPrice);
			break;
		case "信号价":
			if(!StringUtils.equals(currentSignal.getSourceUnifiedSymbol(), bindedUnifiedSymbol)) {
				log.warn("限价会根据信号价格来计算，当信号源合约与下单合约不一致时，有可能会导致下单价格异常。当前信号源合约为：{}，下单合约为：{}", 
						currentSignal.getSourceUnifiedSymbol(), bindedUnifiedSymbol);
			}
			orderPrice = currentSignal.getSignalPrice() + factor * priceTick * overprice;
			log.info("当前使用[限价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", currentSignal.getSignalPrice(), overprice, orderPrice);
			break;
		default:
			throw new IllegalStateException("未知下单价格类型：" + priceTypeStr);
		}
		return orderPrice;
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
		this.priceTypeStr = initParams.priceTypeStr;
		this.overprice = initParams.overprice;
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Setting(value="开仓手数", order = 20)
		private int openVol = 1;
		
		@Setting(value="价格类型", order = 30, options = {"对手价", "市价", "最新价", "排队价", "信号价"})
		private String priceTypeStr;
		
		@Setting(value="超价", order = 40, unit = "Tick")
		private int overprice;
	}

	@Override
	public void setContractManager(ContractManager contractMgr) {
		this.contractMgr = contractMgr;
	}

}
