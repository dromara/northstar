package tech.quantit.northstar.strategy.api.policy.risk;


import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;


/**
 * 当价格超过限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@StrategicComponent("委托超价限制")
public class PriceExceededRule extends AbstractRule implements RiskControlRule {

	protected int priceDifTolerance;
	
	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		if(orderReq.getOrderPriceType() != OrderPriceTypeEnum.OPT_AnyPrice && Math.abs(tick.getLastPrice() - orderReq.getPrice()) > priceDifTolerance) {
			log.warn("[{}] 当前市价 [{}] 距离委托价 [{}] 超过风控限制 [{}]，放弃订单", getModuleName(), tick.getLastPrice(), orderReq.getPrice(), priceDifTolerance);
			return RiskAuditResult.REJECTED;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.priceDifTolerance = initParams.priceDifTolerance;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="超价限制")
		private int priceDifTolerance;
		
	}

}
