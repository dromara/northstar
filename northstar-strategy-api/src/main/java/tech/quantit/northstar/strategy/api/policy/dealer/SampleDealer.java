package tech.quantit.northstar.strategy.api.policy.dealer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.AbstractDealerPolicy;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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
	protected SubmitOrderReqField genOrderReq(DirectionEnum direction, OffsetFlagEnum offsetFlag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SubmitOrderReqField genTracingOrderReq(SubmitOrderReqField originOrderReq) {
		// TODO Auto-generated method stub
		return null;
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
		
		@Setting(value="超价", order = 40, unit = "Tick")
		private int overprice;
	}

}
