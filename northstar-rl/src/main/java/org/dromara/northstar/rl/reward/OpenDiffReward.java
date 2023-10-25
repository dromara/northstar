package org.dromara.northstar.rl.reward;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;

public class OpenDiffReward implements Reward{
    private String rewardType;

    public OpenDiffReward(OpenDiffRewardDescription openDiffRewardDescription) {
        this.rewardType = openDiffRewardDescription.getRewardType();
    }

    @Override
    public JSONObject createReward() {
        JSONObject rewardData = new JSONObject();
        rewardData.put("rewardType", "open-diff");
        return rewardData;
    }

    @Override
    public JSONObject getReward(BarField bar, BarField lastBar) {
        double reward = bar.getOpenPrice() - lastBar.getOpenPrice();
        JSONObject rewardData = new JSONObject();
        rewardData.put("lastReward", reward);
        return rewardData;
    }
    
}
