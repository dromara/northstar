package org.dromara.northstar.common.constant;

import lombok.Getter;

public enum ClosingPolicy {

	PRIOR_TODAY("平今优先"),
	
	FIFO("先开先平"),
	
	PRIOR_BEFORE_HEGDE_TODAY("平昨锁今");

	@Getter
	private String name;
	private ClosingPolicy(String name) {
		this.name = name;
	}
	
}
