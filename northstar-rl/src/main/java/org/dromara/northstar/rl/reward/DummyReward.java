package org.dromara.northstar.rl.reward;

import com.alibaba.fastjson.JSONObject;
import xyz.redtorch.pb.CoreField.BarField;

public class DummyReward implements Reward{
    private String rewardType;
    
    public DummyReward(DummyRewardDescription dummyRewardDescription) {
        this.rewardType = dummyRewardDescription.getRewardType();
    }

    @Override
    public JSONObject createReward() {
        JSONObject rewardData = new JSONObject();
        rewardData.put("rewardType", this.rewardType);
        return rewardData;
    }
    
    @Override
    public JSONObject getReward(BarField bar, BarField lastBar) {
        double reward = 0;
        JSONObject rewardData = new JSONObject();
        rewardData.put("lastReward", reward);
        return rewardData;
    }

}
