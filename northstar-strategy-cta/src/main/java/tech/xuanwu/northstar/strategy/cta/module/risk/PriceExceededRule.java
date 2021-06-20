package tech.xuanwu.northstar.strategy.cta.module.risk;

import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("委托超价限制")
public class PriceExceededRule implements RiskControlRule, DynamicParamsAware{

	protected int priceDifToleranceInTick;
	
	private SubmitOrderReqField orderReq;
	
	@Override
	public short canDeal(TickField tick, ModuleAgent agent) {
		int factor = orderReq.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
		if(factor * (tick.getLastPrice() - orderReq.getPrice()) > priceDifToleranceInTick) {
			return RiskAuditResult.REJECTED;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrderReq(SubmitOrderReqField orderReq) {
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
		this.priceDifToleranceInTick = initParams.priceDifToleranceInTick;
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="超价限制", unit="Tick")
		private int priceDifToleranceInTick;
		
	}

}
