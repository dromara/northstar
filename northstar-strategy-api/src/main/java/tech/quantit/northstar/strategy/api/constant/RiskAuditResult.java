package tech.quantit.northstar.strategy.api.constant;

public enum RiskAuditResult {
	
	/**
	 * 风控通过
	 */
	ACCEPTED,
	/**
	 * 追单重试
	 */
	RETRY,
	/**
	 * 风控拒绝
	 */
	REJECTED;

}
