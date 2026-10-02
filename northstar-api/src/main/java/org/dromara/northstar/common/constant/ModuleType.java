package org.dromara.northstar.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

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

	@JsonCreator
	public static ModuleType fromString(String value) {
		for (ModuleType type : values()) {
			if (type.name().equalsIgnoreCase(value) || type.name.equals(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("未知模组类型: " + value);
	}

	@Override
	public String toString() {
		return name;
	}
}
