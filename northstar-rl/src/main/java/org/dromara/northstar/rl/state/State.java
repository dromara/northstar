package org.dromara.northstar.rl.state;

import com.alibaba.fastjson.JSONObject;
import xyz.redtorch.pb.CoreField.BarField;

public interface State {
    public JSONObject createState();

    public JSONObject getState(BarField bar);
}
