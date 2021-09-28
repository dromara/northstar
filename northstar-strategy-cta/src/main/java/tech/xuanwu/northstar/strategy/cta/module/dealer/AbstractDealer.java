package tech.xuanwu.northstar.strategy.cta.module.dealer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.cta.module.signal.CtaSignal;
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
public abstract class AbstractDealer implements Dealer{

	protected CtaSignal currentSignal;
	
	@Setter
	protected ContractManager contractManager;
	
	protected String bindedUnifiedSymbol;
	
	protected int openVol;
	
	protected String priceTypeStr;
	
	protected int overprice;
	
	protected ModuleStatus moduleStatus;
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public void onSignal(Signal signal) {
		currentSignal = (CtaSignal) signal;
	}
	
	@Override
	public void setModuleStatus(ModuleStatus status) {
		moduleStatus = status;
	}
	
	protected double resolvePrice(CtaSignal currentSignal, TickField tick) {
		int factor = currentSignal.getState().isBuy() ? 1 : -1;
		ContractField contract = contractManager.getContract(tick.getUnifiedSymbol());
		double priceTick = contract.getPriceTick();
		double orderPrice = 0;
		switch(priceTypeStr) {
		case "对手价":
			double oppPrice = currentSignal.getState().isBuy() ? tick.getAskPrice(0) : tick.getBidPrice(0);
			orderPrice = oppPrice + factor * priceTick * overprice;
			log.info("当前使用[对手价]为基准，基础价为：{}，超价：{} Tick，最终下单价：{}", oppPrice, overprice, orderPrice);
			break;
		case "市价":
			orderPrice = 0;
			log.info("当前使用[市价]为基准，最终下单价：{}", orderPrice);
			break;
		case "最新价":
			orderPrice = tick.getLastPrice() + factor * priceTick * overprice;
			log.info("当前使用[最新价]为基准，基础价为：{}，超价：{} Tick，最终下单价：{}", tick.getLastPrice(), overprice, orderPrice);
			break;
		case "排队价":
			orderPrice = currentSignal.getState().isBuy() ? tick.getBidPrice(0) : tick.getAskPrice(0);
			log.info("当前使用[排队价]为基准，基础价为：{}，忽略超价，最终下单价：{}", orderPrice, orderPrice);
			break;
		case "信号价":
			if(!StringUtils.equals(currentSignal.getSourceUnifiedSymbol(), bindedUnifiedSymbol)) {
				log.warn("限价会根据信号价格来计算，当信号源合约与下单合约不一致时，有可能会导致下单价格异常。当前信号源合约为：{}，下单合约为：{}", 
						currentSignal.getSourceUnifiedSymbol(), bindedUnifiedSymbol);
			}
			orderPrice = currentSignal.getSignalPrice() + factor * priceTick * overprice;
			log.info("当前使用[限价]为基准，基础价为：{}，超价：{} Tick，最终下单价：{}", currentSignal.getSignalPrice(), overprice, orderPrice);
			break;
		default:
			throw new IllegalStateException("未知下单价格类型：" + priceTypeStr);
		}
		return orderPrice;
	}
	
	protected SubmitOrderReqField genSubmitOrder(ContractField contract, DirectionEnum direction, OffsetFlagEnum offsetFlag, 
			int vol, double price, double stopPrice) {
		return SubmitOrderReqField.newBuilder()
				.setOriginOrderId(UUID.randomUUID().toString())
				.setContract(contract)
				.setDirection(direction)
				.setOffsetFlag(offsetFlag)
				.setOrderPriceType(price == 0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolume(vol)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(price == 0 ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setStopPrice(stopPrice)
				.setPrice(price)
				.build();
	}
	
	public Optional<SubmitOrderReqField> tryStopLoss(TickField tick){
		Optional<SubmitOrderReqField> orderReq = moduleStatus.triggerStopLoss(tick, contractManager.getContract(tick.getUnifiedSymbol()));
		if(orderReq.isPresent()) 
			moduleStatus.transform(ModuleEventType.STOP_LOSS);
		return orderReq;
	}
}
