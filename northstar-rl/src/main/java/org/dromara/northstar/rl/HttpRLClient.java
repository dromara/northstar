package org.dromara.northstar.rl;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dromara.northstar.rl.agent.Agent;
import org.dromara.northstar.rl.reward.Reward;
import org.dromara.northstar.rl.state.State;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;

public class HttpRLClient {
    protected String getActionUrl = "http://localhost:5001/get-action";
	protected String initInfoUrl = "http://localhost:5001/init-info";
    protected State state;
    protected Agent agent;
    protected Reward reward;

    public HttpRLClient(State state, Agent agent, Reward reward) {
        this.state = state;
        this.agent = agent;
        this.reward = reward;
    }

    public boolean createClient() {
        JSONObject stateData = this.state.createState();
        JSONObject agentData = this.agent.createAgent();
        JSONObject rewardData = this.reward.createReward();

        JSONObject mergedData = mergeJSON(stateData, agentData, rewardData);
        String jsonContent = mergedData.toString();
        
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(initInfoUrl);
            httpPost.setHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(jsonContent);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String jsonResponse = EntityUtils.toString(responseEntity);
                JSONObject responseJSON = JSON.parseObject(jsonResponse);
                boolean success = responseJSON.getBoolean("success");
                return success;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public int getAction(BarField bar, BarField lastBar) {
        JSONObject stateData = this.state.getState(bar);
        JSONObject rewardData = this.reward.getReward(bar, lastBar);

        JSONObject mergedData = mergeJSON(stateData, rewardData);
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(getActionUrl);
            httpPost.setHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(mergedData.toString());
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                String jsonResponse = EntityUtils.toString(responseEntity);
                JSONObject responseJSON = JSON.parseObject(jsonResponse);
                int actionID = responseJSON.getIntValue("actionID");
                return actionID;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private JSONObject mergeJSON(JSONObject stateData, JSONObject rewardData) {
        JSONObject mergedData = stateData;
        mergedData.putAll(rewardData);

        return mergedData;
    }

    private JSONObject mergeJSON(JSONObject stateData, JSONObject agentData, JSONObject rewardData) {
        JSONObject mergedData = stateData;
        mergedData.putAll(agentData);
        mergedData.putAll(rewardData);

        return mergedData;
    }
}
