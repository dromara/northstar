package tech.xuanwu.northstar.strategy.cta.module.risk;

import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("委托超时限制")
public class TimeExceededRule implements RiskControlRule, DynamicParamsAware{
	
	private long timeoutInterval;

	private long lastUpdateTime;

	@Override
	public short canDeal(TickField tick, ModuleAgent agent) {
		if(tick.getActionTimestamp() - lastUpdateTime > timeoutInterval) {
			return RiskAuditResult.RETRY;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrderReq(SubmitOrderReqField orderReq) {
		this.lastUpdateTime = System.currentTimeMillis();
		return this;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.timeoutInterval = initParams.timeoutSeconds;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Label(value="超时时间", unit="秒")
		private int timeoutSeconds;
		
	}
}
