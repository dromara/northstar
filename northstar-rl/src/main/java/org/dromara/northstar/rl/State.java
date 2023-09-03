package org.dromara.northstar.rl;

import com.alibaba.fastjson.JSONObject;

public abstract class State {

	public JSONObject getValue() {
		return evaluate();
	}
	
	public abstract JSONObject evaluate();
	
}