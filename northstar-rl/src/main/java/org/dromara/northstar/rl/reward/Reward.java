package org.dromara.northstar.rl.reward;

import com.alibaba.fastjson2.JSONObject;

public interface Reward {

    public JSONObject createReward();
}
