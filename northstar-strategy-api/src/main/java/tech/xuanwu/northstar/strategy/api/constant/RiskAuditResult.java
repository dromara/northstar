package tech.xuanwu.northstar.strategy.api.constant;

public interface RiskAuditResult {
	
	/**
	 * 风控通过
	 */
	short ACCEPTED = 1;
	/**
	 * 追单重试
	 */
	short RETRY = 2;
	/**
	 * 风控拒绝
	 */
	short REJECTED = 4;
}
