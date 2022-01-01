package tech.quantit.northstar.strategy.api.policy.dealer;

import tech.quantit.northstar.strategy.api.AbstractDealerPolicy;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
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
		
	}

}
