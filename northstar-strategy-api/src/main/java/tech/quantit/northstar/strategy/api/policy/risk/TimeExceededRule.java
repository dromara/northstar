package tech.quantit.northstar.strategy.api.policy.risk;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当下单超时时，会撤单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("委托超时限制")
public class TimeExceededRule implements RiskControlRule {
	
	protected long timeoutSeconds;

	protected long lastUpdateTime;

//	@Override
//	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
//		if(lastUpdateTime == Long.MAX_VALUE) {
//			lastUpdateTime = tick.getActionTimestamp();
//		}
//		if(tick.getActionTimestamp() - lastUpdateTime > timeoutSeconds * 1000) {
//			log.info("挂单超时，撤单追单");
//			return RiskAuditResult.RETRY;
//		}
//		return RiskAuditResult.ACCEPTED;
//	}
//	
//	@Override
//	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
//		this.lastUpdateTime = Long.MAX_VALUE;
//		return this;
//	}

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
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		// TODO Auto-generated method stub
		return null;
	}

}
