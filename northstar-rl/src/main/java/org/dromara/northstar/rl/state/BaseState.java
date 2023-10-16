package org.dromara.northstar.rl.state;

import java.util.List;

import com.alibaba.fastjson2.JSONObject;

public class BaseState implements State {
    private int stateDim;
    private List<String> stateNames;

    public BaseState(BaseStateDescription stateDescription) {
        this.stateDim = stateDescription.getStateDim();
        this.stateNames = stateDescription.getStateNames();
    }
    
    public JSONObject createState() {
        JSONObject stateData = new JSONObject();
        stateData.put("stateDim", this.stateDim);
        stateData.put("stateNames", this.stateNames);

        return stateData;
    }
}
