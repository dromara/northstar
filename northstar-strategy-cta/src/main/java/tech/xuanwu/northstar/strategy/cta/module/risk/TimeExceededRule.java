package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当下单超时时，会撤单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("委托超时限制")
public class TimeExceededRule implements RiskControlRule, DynamicParamsAware{
	
	protected long timeoutSeconds;

	protected long lastUpdateTime;

	@Override
	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
		if(lastUpdateTime == Long.MAX_VALUE) {
			lastUpdateTime = tick.getActionTimestamp();
		}
		if(tick.getActionTimestamp() - lastUpdateTime > timeoutSeconds * 1000) {
			log.info("挂单超时，撤单追单");
			return RiskAuditResult.RETRY;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
		this.lastUpdateTime = Long.MAX_VALUE;
		return this;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.timeoutSeconds = initParams.timeoutSeconds;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="超时时间", unit="秒")
		private int timeoutSeconds;
		
	}
}
