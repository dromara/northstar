package tech.xuanwu.northstar.strategy.cta.module.risk;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.ModuleStatus;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.annotation.Setting;
import tech.xuanwu.northstar.strategy.api.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.api.constant.RiskAuditResult;
import tech.xuanwu.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当下单成本超过实际资金限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@Slf4j
@StrategicComponent("模组占用账户资金限制")
public class UseMarginExceededRule implements RiskControlRule {

	protected double limitedPercentageOfTotalBalance;

	private double totalCost;

//	@Override
//	public short canDeal(TickField tick, ModuleStatus moduleStatus) {
//		int moduleAvailable = (int) Math.min(limitedPercentageOfTotalBalance/100.0 * moduleStatus.getAccountAvailable(), moduleStatus.getAccountAvailable());
//		if(totalCost > moduleAvailable) {
//			log.info("开仓成本超过风控限制。成本金额：{}, 当前模组占用比例：{}, 当前模组可用资金：{}",
//					totalCost, limitedPercentageOfTotalBalance, moduleAvailable);
//			return RiskAuditResult.REJECTED;
//		}
//		return RiskAuditResult.ACCEPTED;
//	}
//	
//	@Override
//	public RiskControlRule onSubmitOrder(SubmitOrderReqField orderReq) {
//		ContractField contract = orderReq.getContract();
//		double marginRatio = orderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
//		totalCost = contract.getMultiplier() * orderReq.getPrice() * orderReq.getVolume() * marginRatio;
//		return this;
//	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		this.limitedPercentageOfTotalBalance = initParams.limitedPercentageOfTotalBalance;
	}
	
	
	public static class InitParams extends DynamicParams{
		
		@Setting(value="账户分配比例", unit="%")
		private double limitedPercentageOfTotalBalance;
		
	}

	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick, ModuleStatus moduleStatus) {
		// TODO Auto-generated method stub
		return null;
	}
}
