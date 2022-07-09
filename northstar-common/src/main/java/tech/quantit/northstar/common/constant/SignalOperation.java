package tech.quantit.northstar.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 *  
 * @author KevinHuangwl
 *
 */
public enum SignalOperation {
	/**
	 * 多开，发出多单委托，结果为多头持仓+1；
	 */
	BUY_OPEN("多开"),
	/**
	 * 空开，发出空单委托，结果为空头持仓+1；
	 */
	SELL_OPEN("空开"),
	/**
	 * 多平，发出多单委托，结果为空头持仓-1；
	 */
	BUY_CLOSE("多平"),
	/**
	 * 空平，发出空单委托，结果为多头持仓-1；
	 */
	SELL_CLOSE("空平");
	
	private String text;
	private SignalOperation(String text) {
		this.text = text;
	}
	
	public String text() {
		return text;
	}
	
	public boolean isOpen() {
		return this == BUY_OPEN || this == SELL_OPEN;
	}
	
	public boolean isSell() {
		return this == SELL_OPEN || this == SELL_CLOSE;
	}
	
	public boolean isBuy() {
		return this == BUY_OPEN || this == BUY_CLOSE;
	}
	
	public boolean isClose() {
		return this == BUY_CLOSE || this == SELL_CLOSE;
	}
	
	private static final Map<String, SignalOperation> enumMap = new HashMap<>();
	
	static {
		enumMap.put("多开", BUY_OPEN);
		enumMap.put("空开", SELL_OPEN);
		enumMap.put("多平", BUY_CLOSE);
		enumMap.put("空平", SELL_CLOSE);
	}
	
	public static SignalOperation parse(String val) {
		if(enumMap.containsKey(val)) {
			return enumMap.get(val);
		}
		throw new IllegalArgumentException("未知信号：" + val);
	}
	
}
