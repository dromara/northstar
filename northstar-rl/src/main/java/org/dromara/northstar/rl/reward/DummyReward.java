package org.dromara.northstar.rl.reward;

import com.alibaba.fastjson2.JSONObject;

public class DummyReward implements Reward{
    private String rewardType;
    
    public DummyReward(DummyRewardDescription dummyRewardDescription) {
        this.rewardType = dummyRewardDescription.getRewardType();
    }

    public JSONObject createReward() {
        JSONObject rewardData = new JSONObject();
        rewardData.put("rewardType", this.rewardType);
        return rewardData;
    }
    
    public double getReward() {
        return 0.0;
    }
}
