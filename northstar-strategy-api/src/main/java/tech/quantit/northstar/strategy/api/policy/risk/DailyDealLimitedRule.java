package tech.quantit.northstar.strategy.api.policy.risk;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.StateChangeListener;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当日内交易次数超过限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("日内开仓次数限制")
public class DailyDealLimitedRule extends AbstractRule implements RiskControlRule, StateChangeListener{
	
	protected int dailyDealLimit;
	
	private int countOfTrade;
	
	private String currentTradingDay;
	
	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		if(!tick.getTradingDay().equals(currentTradingDay)) {
			countOfTrade = 0;
			currentTradingDay = tick.getTradingDay();
		}
		if(countOfTrade >= dailyDealLimit) {
			log.warn("[{}] 日内开仓次数到达上限，日内开仓次数限制为{}次", getModuleName(), dailyDealLimit);
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
		this.dailyDealLimit = initParams.dailyDealLimit;
	}
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="日内开仓限制", unit="次")
		private int dailyDealLimit;
		
	}

	@Override
	public void onChange(ModuleState curState) {
		if(curState.isHolding()) {
			dailyDealLimit++;
		}		
	}

	
}
