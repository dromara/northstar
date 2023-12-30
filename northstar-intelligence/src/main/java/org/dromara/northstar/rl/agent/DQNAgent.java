package org.dromara.northstar.rl.agent;

import java.io.IOException;

import org.dromara.northstar.ai.rl.RLAgent;
import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLExperience;
import org.dromara.northstar.ai.rl.model.RLState;

import com.alibaba.fastjson2.JSON;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DQNAgent extends AbstractAgent implements RLAgent{
	
	private static final String BASE_URL = "http://localhost:5002";
	
	private OkHttpClient client = new OkHttpClient();

	public DQNAgent() throws Exception {
		super("dqn", DQNAgent.class);
	}

	@Override
	public RLAction react(RLState state) {
		try {
			return JSON.parseObject(post(state, "/react"), RLAction.class);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void learn(RLExperience exp) {
		try {
			post(exp, "/learn");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	private byte[] post(Object bodyData, String path) throws IOException {
		MediaType mediaType = MediaType.get("application/json; charset=utf-8");
		RequestBody body = RequestBody.create(JSON.toJSONString(bodyData), mediaType);
		Request request = new Request.Builder()
				.url(BASE_URL + path)
				.post(body)
				.build();
        Response response = client.newCall(request).execute();
        if(!response.isSuccessful()) {
        	throw new IllegalStateException(response.message());
        }
        return response.body().bytes();
	}
	
	private byte[] get(String path) throws IOException {
		Request request = new Request.Builder()
				.url(BASE_URL + path)
				.build();
        Response response = client.newCall(request).execute();
        if(!response.isSuccessful()) {
        	throw new IllegalStateException(response.message());
        }
        return response.body().bytes(); 
	}

	@Override
	public void update() {
		try {
			get("/update");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void save(String name) {
		try {
			get("/save?name=" + name);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void load(String name) {
		try {
			get("/load?name=" + name);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
}
