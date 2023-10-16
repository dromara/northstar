package org.dromara.northstar.rl.state;

import java.util.List;

import com.alibaba.fastjson2.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;

public class BaseState implements State {
    // BaseState是一个最基础的State，维度为4，分别是openPrice, highPrice, lowPrice, closePrice。

   
    private int stateDim;
    private List<String> stateNames;

    public BaseState(BaseStateDescription stateDescription) {
        this.stateDim = stateDescription.getStateDim();
        this.stateNames = stateDescription.getStateNames();
    }
    
    @Override
    public JSONObject createState() {
        JSONObject stateData = new JSONObject();
        stateData.put("stateDim", this.stateDim);
        stateData.put("stateNames", this.stateNames);

        return stateData;
    }

    @Override
    public JSONObject getState(BarField bar) {
        double openPrice = bar.getOpenPrice();
        double highPrice = bar.getHighPrice();
        double lowPrice = bar.getLowPrice();
        double closePrice = bar.getClosePrice();

        JSONObject stateData = new JSONObject();
        stateData.put("openPrice", openPrice);
        stateData.put("highPrice", highPrice);
        stateData.put("lowPrice", lowPrice);
        stateData.put("closePrice", closePrice);

        return stateData;
    }
}
