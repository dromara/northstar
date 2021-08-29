package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
@StrategicComponent("委托超时限制")
public class TimeExceededRule implements RiskControlRule, DynamicParamsAware{
	
	private long timeoutInterval;

	private long lastUpdateTime;

	@Override
	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
		if(tick.getActionTimestamp() - lastUpdateTime > timeoutInterval) {
			log.info("挂单超时，撤单追单");
			return RiskAuditResult.RETRY;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
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
		this.timeoutInterval = initParams.timeoutSeconds * 1000;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="超时时间", unit="秒")
		private int timeoutSeconds;
		
	}
}
