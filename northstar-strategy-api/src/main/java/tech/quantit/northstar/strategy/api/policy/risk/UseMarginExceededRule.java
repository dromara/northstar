package tech.quantit.northstar.strategy.api.policy.risk;

import tech.quantit.northstar.common.AccountAware;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.annotation.Setting;
import tech.quantit.northstar.strategy.api.annotation.StrategicComponent;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 当下单成本超过实际资金限制时，会拒绝继续下单
 * @author KevinHuangwl
 *
 */
@StrategicComponent("模组占用账户资金限制")
public class UseMarginExceededRule extends AbstractRule implements RiskControlRule, AccountAware {

	protected double limitedPercentageOfTotalBalance;

	private AccountField account;

	@Override
	public RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick) {
		// 只对开仓请求做一次审查
		if(orderReq.getActionTimestamp() != tick.getActionTimestamp()) {
			return RiskAuditResult.ACCEPTED;
		}
		ContractField contract = orderReq.getContract();
		double marginRatio = orderReq.getDirection() == DirectionEnum.D_Buy ? contract.getLongMarginRatio() : contract.getShortMarginRatio();
		double totalCost = contract.getMultiplier() * orderReq.getPrice() * orderReq.getVolume() * marginRatio;
		int moduleAvailable = (int) Math.min(accountBalance() * limitedPercentageOfTotalBalance / 100.0, accountAvailable());
		if(totalCost > moduleAvailable) {
			log.info("[{}] 开仓成本超过风控限制。成本金额：{}, 当前模组占用比例：{}, 当前模组可用资金：{}", getModuleName(), totalCost,
					limitedPercentageOfTotalBalance, moduleAvailable);
			return RiskAuditResult.REJECTED;
		}
		return RiskAuditResult.ACCEPTED;
	}

	@Override
	public void onAccount(AccountField account) {
		this.account = account;
	}

	@Override
	public double accountBalance() {
		return account.getBalance();
	}

	@Override
	public double accountAvailable() {
		return account.getAvailable();
	}

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
	
}
