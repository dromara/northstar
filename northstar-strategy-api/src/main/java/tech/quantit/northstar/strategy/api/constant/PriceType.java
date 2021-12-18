package tech.quantit.northstar.strategy.api.constant;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public enum PriceType {

	ANY_PRICE("市价"),
	
	OPP_PRICE("对手价"),
	
	LAST_PRICE("最新价"),
	
	WAITING_PRICE("排队价"),
	
	SIGNAL_PRICE("信号价");
	
	@Getter
	private String name;
	private PriceType(String name) {
		this.name = name;
	}
	
	private static Map<String, PriceType> map = new HashMap<>() {
		private static final long serialVersionUID = -5075741267974469725L;

		{
			put("市价", ANY_PRICE);
			put("对手价", OPP_PRICE);
			put("最新价", LAST_PRICE);
			put("排队价", WAITING_PRICE);
			put("信号价", SIGNAL_PRICE);
		}
	};
	public static PriceType parse(String name) {
		return map.get(name);
	} 
}
