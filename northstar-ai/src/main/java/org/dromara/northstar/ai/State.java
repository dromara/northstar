package org.dromara.northstar.ai;

import com.alibaba.fastjson.JSONObject;

public abstract class State {

	public JSONObject getValue() {
		return evaluate();
	}
	
	public abstract JSONObject evaluate();
	
}
