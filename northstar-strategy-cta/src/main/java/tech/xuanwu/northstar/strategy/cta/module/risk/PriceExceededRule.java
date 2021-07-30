package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@StrategicComponent("委托超价限制")
public class PriceExceededRule implements RiskControlRule, DynamicParamsAware{

	protected int priceDifTolerance;
	
	private SubmitOrderReqField orderReq;
	
	@Override
	public short canDeal(TickField tick, StrategyModule module) {
		int factor = orderReq.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
		if(factor * (tick.getLastPrice() - orderReq.getPrice()) > priceDifTolerance) {
			log.info("委托超价限制：限制为{}，期望价为{}，实际价为{}", priceDifTolerance, orderReq.getPrice(), tick.getLastPrice());
			return RiskAuditResult.REJECTED;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
		this.orderReq = orderReq;
		return this;
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
