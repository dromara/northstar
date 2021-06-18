package tech.xuanwu.northstar.common.constant;

public enum TickType {
	/**
	 * 开市行情
	 */
	NORMAL_TICK(0),
	/**
	 * 非开市行情
	 */
	NON_OPENING_TICK(0x1000),
	/**
	 * 开市前非连续交易行情
	 */
	PRE_OPENING_TICK(0x2000),
	/**
	 * 一分钟行情
	 */
	END_OF_MIN_TICK(0x4000),
	/**
	 * 收市前最后一个TICK
	 */
	CLOSING_TICK(0x8000);
	
	private int code;
	private TickType(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
