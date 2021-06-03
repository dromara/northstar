package tech.xuanwu.northstar.strategy.cta.risk;

/**
 * 
 * @author KevinHuangwl
 *
 */
public class UseMarginExceededRule {

	private double limitedPercentageOfTotalBalance;
	
	public UseMarginExceededRule(double limitedPercentageOfTotalBalance) {
		this.limitedPercentageOfTotalBalance = limitedPercentageOfTotalBalance;
	}
}
