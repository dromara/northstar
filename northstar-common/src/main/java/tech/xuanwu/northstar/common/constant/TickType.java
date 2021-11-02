package tech.xuanwu.northstar.common.constant;

public enum TickType {
	/**
	 * 回测行情
	 */
	PLAYBACK_TICK(-1),
	/**
	 * 非开市行情
	 */
	NON_OPENING_TICK(0),
	/**
	 * 开市前非连续交易行情
	 */
	PRE_OPENING_TICK(1),
	/**
	 * 开市行情
	 */
	NORMAL_TICK(3),
	/**
	 * 一分钟行情
	 */
	END_OF_MIN_TICK(4),
	/**
	 * 收市前最后一个TICK
	 */
	CLOSING_TICK(8);
	
	private int code;
	private TickType(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
