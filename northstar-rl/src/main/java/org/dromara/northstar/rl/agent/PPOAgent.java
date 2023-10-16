package org.dromara.northstar.rl.agent;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.dromara.northstar.rl.agent.Agent;
import com.alibaba.fastjson.JSONObject;

public class PPOAgent implements Agent {
    
    public JSONObject createAgent(PPOAgentDescription agentDescription) {

        JSONObject jsonData = new JSONObject();
        jsonData.put("indicator_symbol", agentDescription.getIndicatorSymbol());
        jsonData.put("agent_name", agentDescription.getAgentName());
        jsonData.put("state_dim", agentDescription.getStateDim());
        jsonData.put("action_dim", agentDescription.getActionDim());
        jsonData.put("lr_actor", agentDescription.getLrActor());
        jsonData.put("lr_critic", agentDescription.getLrCritic());
        jsonData.put("gamma", agentDescription.getGamma());
        jsonData.put("K_epochs", agentDescription.getKEpochs());
        jsonData.put("eps_clip", agentDescription.getEpsClip());
        jsonData.put("is_train", agentDescription.isTrain());
        jsonData.put("model_version", agentDescription.getModelVersion());

        return jsonData;
    }
}
