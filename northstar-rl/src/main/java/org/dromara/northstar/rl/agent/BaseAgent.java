package org.dromara.northstar.rl.agent;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dromara.northstar.rl.RLReward;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;

public class BaseAgent {
    private String getActionUrl = "http://localhost:5001/get-action";
	private String initInfoUrl = "http://localhost:5001/init-info";

	private CloseableHttpClient httpClient = HttpClients.createDefault();

    public boolean initInfo(
        String indicatorSymbol,
        String agentName,
        boolean isTrain,
        String modelVersion
    ) {
		// 将参数传给Python端
		try {
			JSONObject jsonData = new JSONObject();
			jsonData.put("indicator_symbol", indicatorSymbol);
			jsonData.put("agent_name", agentName);
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
				JSONObject jsonObject = JSON.parseObject(jsonResponse);
				boolean success = jsonObject.getBoolean("success");
                return success;
			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}

    public int getAction(BarField bar, double lastReward) {

		try {
			JSONObject jsonData = new JSONObject();
			jsonData.put("unified_symbol", bar.getUnifiedSymbol());
			jsonData.put("open_price", bar.getOpenPrice());
			jsonData.put("high_price", bar.getHighPrice());
			jsonData.put("low_price", bar.getLowPrice());
			jsonData.put("close_price", bar.getClosePrice());
			jsonData.put("last_reward", lastReward);
			String jsonContent = jsonData.toString();
			HttpPost httpPost = new HttpPost(getActionUrl);
			httpPost.setHeader("Content-Type", "application/json");
			StringEntity entity = new StringEntity(jsonContent);
			httpPost.setEntity(entity);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			HttpEntity responseEntity = response.getEntity();

			if (responseEntity != null) {
				String jsonResponse = EntityUtils.toString(responseEntity);
				JSONObject jsonObject = JSON.parseObject(jsonResponse);
				
				Integer actionID = jsonObject.getInteger("action"); // 0: 持仓；1：买；2: 卖
				return actionID;

			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

    public double getReward(BarField bar, BarField lastBar, int actionID, String rewardType) {
        return RLReward.rewardGenerator(bar, lastBar, actionID, rewardType);
    }
}
