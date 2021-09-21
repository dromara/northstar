package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当日内交易次数超过限制时，会拒绝继续下单
 * @author kevin
 *
 */
@Slf4j
@StrategicComponent("日内开仓次数限制")
public class DailyDealLimitedRule implements RiskControlRule {
	
	protected int dailyDealLimit;
	
	@Override
	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
		long numberOfOpeningTradeToday = moduleStatus.getCountOfOpeningToday();
		if(numberOfOpeningTradeToday < dailyDealLimit) {
			return RiskAuditResult.ACCEPTED;
		}
		log.info("日内开仓次数限制，拒绝订单");
		return RiskAuditResult.REJECTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
		//该风控规则不需要参考订单状态，不需要做任何处理
		return this;
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

}
