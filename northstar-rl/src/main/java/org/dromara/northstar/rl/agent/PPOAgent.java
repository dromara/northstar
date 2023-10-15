package org.dromara.northstar.rl.agent;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.dromara.northstar.rl.agent.Agent;
import com.alibaba.fastjson.JSONObject;

public class PPOAgent extends Agent {
    
    public boolean initInfo(
        String indicatorSymbol,
        String agentName,
        int stateDim,
        int actionDim,
        double lrActor,
        double lrCritic,
        double gamma,
        int kEpochs,
        double epsClip,
        boolean isTrain,
        String modelVersion
    ) {
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("indicator_symbol", indicatorSymbol);

            // PPO 模型超参数设置
            jsonData.put("state_dim", stateDim);
            jsonData.put("action_dim", actionDim);
            jsonData.put("lr_actor", lrActor);
            jsonData.put("lr_critic", lrCritic);
            jsonData.put("gamma", gamma);
            jsonData.put("K_epochs", kEpochs);
            jsonData.put("eps_clip", epsClip);

            // 通用参数设置
            jsonData.put("is_train", isTrain);
            jsonData.put("model_version", modelVersion);
            
            String jsonContent = jsonData.toString();
            HttpPost httpPost = new HttpPost(initInfoUrl);
            httpPost.setHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(jsonContent);
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            
            if (responseEntity != null) {
                String jsonResponse = EntityUtils.toString(responseEntity);
                JSONObject jsonObject = JSONObject.parseObject(jsonResponse);
                boolean success = jsonObject.getBoolean("success");
                return success;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
