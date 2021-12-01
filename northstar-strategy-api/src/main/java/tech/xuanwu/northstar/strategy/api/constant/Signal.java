package tech.xuanwu.northstar.strategy.api.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用四位二进制数表达六个状态
 * 
 * 反手本质上是两组运作的叠加，例如反手多=多平+多开
 * 因此由该二进制编码可得规律，并总结成枚举方法如下
 * 
 * @author KevinHuangwl
 *
 */
public enum Signal {
	/**
	 * 多开
	 */
	BUY_OPEN("多开"),
	/**
	 * 空开
	 */
	SELL_OPEN("空开"),
	/**
	 * 多平
	 */
	BUY_CLOSE("多平"),
	/**
	 * 空平
	 */
	SELL_CLOSE("空平");
	
	private String val;
	private Signal(String text) {
		this.val = text;
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
	
	private static final Map<String, Signal> enumMap = new HashMap<>();
	
	static {
		enumMap.put("多开", BUY_OPEN);
		enumMap.put("空开", SELL_OPEN);
		enumMap.put("多平", BUY_CLOSE);
		enumMap.put("空平", SELL_CLOSE);
	}
	
	public static Signal parse(String val) {
		if(enumMap.containsKey(val)) {
			return enumMap.get(val);
		}
		throw new IllegalArgumentException("未知信号：" + val);
	}
	
}
