package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("模组占用账户资金限制")
public class UseMarginExceededRule implements RiskControlRule, DynamicParamsAware{

	private double limitedPercentageOfTotalBalance;

	private double totalCost;

	@Override
	public short canDeal(TickField tick, StrategyModule module) {
		AccountField account = module.getAccount();
		int moduleAvailable = (int) Math.min(limitedPercentageOfTotalBalance * account.getBalance(), account.getAvailable());
		if(totalCost > moduleAvailable) {
			log.info("开仓成本超过风控限制。成本金额：{}, 当前模组占用比例：{}, 当前模组可用资金：{}",
					totalCost, (int)(limitedPercentageOfTotalBalance * 100), moduleAvailable);
			return RiskAuditResult.REJECTED;
		}
		return RiskAuditResult.ACCEPTED;
	}
	
	@Override
	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
		ContractField contract = orderReq.getContract();
		double marginRatio = orderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		totalCost = contract.getMultiplier() * orderReq.getPrice() * orderReq.getVolume() * marginRatio;
		return this;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.limitedPercentageOfTotalBalance = initParams.limitedPercentageOfTotalBalance / 100.0;
	}
	
	
	public static class InitParams extends DynamicParams{
		
		@Label(value="账户分配比例", unit="%")
		private double limitedPercentageOfTotalBalance;
		
	}
}
