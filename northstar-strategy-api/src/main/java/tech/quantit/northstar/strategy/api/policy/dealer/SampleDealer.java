package tech.quantit.northstar.strategy.api.policy.dealer;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.AbstractDealerPolicy;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.utils.PriceResolver;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
/**
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("示例交易策略")
public class SampleDealer extends AbstractDealerPolicy implements DealerPolicy {
	
	private int openVol;
	
	private String openPriceTypeStr;
	
	private String closePriceTypeStr;
	
	private int overprice;

	@Override
	public String name() {
		return "示例交易策略";
	}

	@Override
	protected SubmitOrderReqField genOrderReq(DirectionEnum direction, OffsetFlagEnum offsetFlag, double signalPrice, int ticksToStop) {
		int factor = FieldUtils.isBuy(direction) ? 1 : -1;
		String priceType = FieldUtils.isClose(offsetFlag) ? closePriceTypeStr : openPriceTypeStr;
		double price = PriceResolver.getPrice(PriceType.parse(priceType), signalPrice, lastTick, FieldUtils.isBuy(direction));
		double stopPrice = price - factor * ticksToStop * bindedContract.getPriceTick();
		
		return SubmitOrderReqField.newBuilder()
				.setOriginOrderId(UUID.randomUUID().toString())
				.setContract(bindedContract)
				.setDirection(direction)
				.setOffsetFlag(offsetFlag)
				.setStopPrice(stopPrice)
				.setPrice(price)
				.setVolume(openVol)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(price == 0 ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(price == 0 ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.build();
	}

	@Override
	protected SubmitOrderReqField genTracingOrderReq(SubmitOrderReqField originOrderReq) {
		int factor = FieldUtils.isBuy(originOrderReq.getDirection()) ? 1 : -1;
		String priceType = FieldUtils.isClose(originOrderReq.getOffsetFlag()) ? closePriceTypeStr : openPriceTypeStr;
		double basePrice = PriceResolver.getPrice(PriceType.parse(priceType), originOrderReq.getPrice(), lastTick, factor > 0);
		double tracePrice = basePrice == 0 ? 0 : basePrice + overprice * bindedContract.getPriceTick();
		log.info("追单采用基础价{}，超价{}个Tick", basePrice, overprice);
		return SubmitOrderReqField.newBuilder(originOrderReq)
				.setPrice(tracePrice)
				.build();
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
		this.openPriceTypeStr = initParams.openPriceTypeStr;
		this.closePriceTypeStr = initParams.closePriceTypeStr;
		this.overprice = initParams.overprice;
	}
	
	public static class InitParams extends DynamicParams{

		@Setting(value="绑定合约", order = 10)
		private String bindedUnifiedSymbol;
		
		@Setting(value="开仓手数", order = 20)
		private int openVol;
		
		@Setting(value="开仓价格类型", order = 30, options = {"市价","对手价","最新价","排队价","信号价"})
		private String openPriceTypeStr;
		
		@Setting(value="平仓价格类型", order = 31, options = {"市价","对手价","最新价","排队价","信号价"})
		private String closePriceTypeStr;
		
		@Setting(value="追单超价", order = 40, unit = "Tick")
		private int overprice;
	}

}
