package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.annotation.Setting;
import tech.xuanwu.northstar.strategy.api.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;
import tech.xuanwu.northstar.strategy.api.constant.RiskAuditResult;
import tech.xuanwu.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当日内交易次数超过限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("日内开仓次数限制")
public class DailyDealLimitedRule implements RiskControlRule {
	
	protected int dailyDealLimit;
	
//	@Override
//	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
//		long numberOfOpeningTradeToday = moduleStatus.getCountOfOpeningToday();
//		if(numberOfOpeningTradeToday < dailyDealLimit) {
//			return RiskAuditResult.ACCEPTED;
//		}
//		log.info("日内开仓次数限制，拒绝订单");
//		return RiskAuditResult.REJECTED;
//	}
	
	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		// TODO Auto-generated method stub
		return null;
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
	public void onChange(ModuleState state) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onAccount(AccountField account) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public double accountBalance() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double accountAvailable() {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
