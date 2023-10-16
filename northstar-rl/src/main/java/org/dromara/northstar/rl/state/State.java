package org.dromara.northstar.rl.state;

import com.alibaba.fastjson2.JSONObject;

public interface State {
    public JSONObject createState();
}
