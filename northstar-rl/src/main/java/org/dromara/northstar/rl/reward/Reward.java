package org.dromara.northstar.rl.reward;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;

public interface Reward {

    public JSONObject createReward();

    public JSONObject getReward(BarField bar, BarField lastBar);

}
