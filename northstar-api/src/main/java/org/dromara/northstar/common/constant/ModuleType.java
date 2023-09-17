package org.dromara.northstar.common.constant;

public enum ModuleType {
	/**
	 * 投机
	 */
	SPECULATION("投机"), 
	/**
	 * 套利类
	 */
	ARBITRAGE("套利");
	
	
	private String name;
	private ModuleType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
