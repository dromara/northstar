package tech.quantit.northstar.strategy.api.policy.risk;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当下单超时时，会撤单
 * @author KevinHuangwl
 *
 */
@StrategicComponent("委托超时限制")
public class TimeExceededRule extends AbstractRule implements RiskControlRule {
	
	protected long timeoutSeconds;

	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		if(tick.getActionTimestamp() - orderReq.getActionTimestamp() > timeoutSeconds * 1000) {
			LocalTime submitTime = LocalTime.ofInstant(Instant.ofEpochMilli(orderReq.getActionTimestamp()), ZoneId.systemDefault());
			LocalTime curTime = LocalTime.ofInstant(Instant.ofEpochMilli(tick.getActionTimestamp()), ZoneId.systemDefault());
			log.warn("[{}] 委托超时，撤单重试：下单时间{}，当前时间{}，超时设置{}秒", getModuleName(), submitTime, curTime, timeoutSeconds);
			return RiskAuditResult.RETRY;
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
		this.timeoutSeconds = initParams.timeoutSeconds;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="超时时间", unit="秒")
		private int timeoutSeconds;
		
	}

}
