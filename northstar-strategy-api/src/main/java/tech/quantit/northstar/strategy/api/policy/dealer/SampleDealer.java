package tech.quantit.northstar.strategy.api.policy.dealer;

import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.strategy.api.AbstractDealerPolicy;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
/**
 * 
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @author KevinHuangwl
 *
 */
@StrategicComponent("示例交易策略")
public class SampleDealer extends AbstractDealerPolicy implements DealerPolicy {
	
	private int openVol;
	
	private String openPriceTypeStr;
	
	private String closePriceTypeStr;
	
	private int overprice;
	
	private int stopPriceTick;

	@Override
	public String name() {
		return "示例交易策略";
	}

	@Override
	protected PriceType openPriceType() {
		return PriceType.parse(openPriceTypeStr);
	}


	@Override
	protected PriceType closePriceType() {
		return PriceType.parse(closePriceTypeStr);
	}


	@Override
	protected int tradeVolume() {
		return openVol;
	}

	/**
	 * 可以根据需要设置固定止损，甚至更复杂的止损逻辑
	 */
	@Override
	protected double stopLossPrice(double orderPrice, int ticksToStop, DirectionEnum direction) {
		int factor = FieldUtils.isBuy(direction) ? 1 : -1;
		if(ticksToStop > 0) {
			stopPriceTick = ticksToStop;
		}
		return orderPrice - factor * stopPriceTick * bindedContract.getPriceTick();
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}
	
	/**
	 * 实现追价逻辑
	 * 这个方法扩展有一定危机性，容易出错而不好排查
	 */
	@Override
	protected SubmitOrderReqField genTracingOrderReq(SubmitOrderReqField originOrderReq) {
		int factor = FieldUtils.isBuy(originOrderReq.getDirection()) ? 1 : -1;
		double originPrice = originOrderReq.getPrice();
		double tracingPrice = originPrice + factor * overprice * originOrderReq.getContract().getPriceTick();
		return SubmitOrderReqField.newBuilder(originOrderReq)
				.setPrice(tracingPrice)
				.build();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.bindedUnifiedSymbol = initParams.bindedUnifiedSymbol;
		this.openVol = initParams.openVol;
		this.openPriceTypeStr = initParams.openPriceTypeStr;
		this.closePriceTypeStr = initParams.closePriceTypeStr;
		this.overprice = initParams.overprice;
		this.stopPriceTick = initParams.stopPriceTick;
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
		
		@Setting(value="固定止损", order = 50, unit = "Tick")
		private int stopPriceTick;
	}

}
