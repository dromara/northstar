package tech.quantit.northstar.strategy.api.policy.risk;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.TickDataAware;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当下单超时时，会撤单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("委托超时限制")
public class TimeExceededRule implements RiskControlRule, TickDataAware, EventDrivenComponent {
	
	protected long timeoutSeconds;

	protected long lastUpdateTime;
	
	protected SubmitOrderReqField orderReq;
	
	protected ModuleEventBus meb;

	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		this.orderReq = orderReq;
		this.lastUpdateTime = tick.getActionTimestamp();
		return RiskAuditResult.ACCEPTED;
	}

	@Override
	public void onTick(TickField tick) {
		if(orderReq != null && tick.getUnifiedSymbol().equals(orderReq.getContract().getUnifiedSymbol())
				&& tick.getActionTimestamp() - lastUpdateTime > timeoutSeconds * 1000) {
			LocalTime submitTime = LocalTime.ofInstant(Instant.ofEpochMilli(lastUpdateTime), ZoneId.systemDefault());
			LocalTime curTime = LocalTime.ofInstant(Instant.ofEpochMilli(tick.getActionTimestamp()), ZoneId.systemDefault());
			log.warn("委托超时，撤单重试：下单时间{}，当前时间{}，超时设置{}秒", submitTime, curTime, timeoutSeconds);
			meb.post(new ModuleEvent<>(ModuleEventType.RETRY_RISK_ALERTED, orderReq));
			orderReq = null;
		}
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

	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		// 不处理
	}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		meb = moduleEventBus;
	}

}
