package tech.xuanwu.northstar.persistance;

import java.util.List;

import tech.xuanwu.northstar.persistance.po.Bar;

public interface BarRepo {

	void save(Bar bar);
	
	List<Bar> loadCurrentTradingDay(String unifiedSymbol);
	
	List<Bar> loadNDaysRef(String unifiedSymbol, int days);
}
