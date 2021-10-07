package tech.xuanwu.northstar.strategy.common.constants;

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
public enum SignalOperation {
	/**
	 * 多开
	 */
	BuyOpen("多开"),
	/**
	 * 空开
	 */
	SellOpen("空开"),
	/**
	 * 多平
	 */
	BuyClose("多平"),
	/**
	 * 空平
	 */
	SellClose("空平"),
	/**
	 * 无信号
	 */
	None("");

	
	private String val;
	private SignalOperation(String text) {
		this.val = text;
	}
	
	public boolean isOpen() {
		return this == BuyOpen || this == SellOpen;
	}
	
	public boolean isSell() {
		return this == SellOpen || this == SellClose;
	}
	
	public boolean isBuy() {
		return this == BuyOpen || this == BuyClose;
	}
	
	public boolean isClose() {
		return this == BuyClose || this == SellClose;
	}
	
	private static final Map<String, SignalOperation> enumMap = new HashMap<>();
	
	static {
		enumMap.put("多开", BuyOpen);
		enumMap.put("空开", SellOpen);
		enumMap.put("多平", BuyClose);
		enumMap.put("空平", SellClose);
	}
	
	public static SignalOperation parse(String val) {
		if(enumMap.containsKey(val)) {
			return enumMap.get(val);
		}
		return None;
	}
	
}
