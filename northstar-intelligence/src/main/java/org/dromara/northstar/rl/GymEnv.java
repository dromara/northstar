package org.dromara.northstar.rl;

import java.util.Objects;

import org.dromara.northstar.ai.rl.RLEnvironment;
import org.dromara.northstar.ai.rl.model.RLAction;
import org.dromara.northstar.ai.rl.model.RLEnvResponse;
import org.dromara.northstar.ai.rl.model.RLState;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class GymEnv implements RLEnvironment {
	
	private boolean hasInit;
	
	private Process proc;
	
	private OkHttpClient client = new OkHttpClient();
	
	private static final String BASE_URL = "http://localhost:5001";
	
	public GymEnv() {
		start();
	}
	
	public void start() {
		if(hasInit) {
			return;
		}
		hasInit = true;
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = classLoader.getResource("org/dromara/northstar/rl/main.py").getFile().replaceFirst("/", "");
		log.info("加载main.py的路径：{}", path);
		try {
		    ProcessBuilder pb = new ProcessBuilder("python", path);
		    proc = pb.start();
		    Thread.sleep(1000);
		} catch (Exception e) {
		    log.error("", e);
		}
	}
	
	public void close() {
		if(hasInit && Objects.nonNull(proc)) {
			proc.destroy();
		}
	}

	@Override
	public RLEnvResponse interact(RLAction action) {
		MediaType mediaType = MediaType.get("application/json; charset=utf-8");
		RequestBody body = RequestBody.create(JSON.toJSONString(action), mediaType);
		Request request = new Request.Builder()
				.url(BASE_URL + "/interact")
				.post(body)
				.build();
		try {
            Response response = client.newCall(request).execute();
            return JSON.parseObject(response.body().bytes(), RLEnvResponse.class);
        } catch (Exception e) {
            log.error("", e);
        }
		return null;
	}

	@Override
	public RLState reset() {
		Request request = new Request.Builder()
				.url(BASE_URL + "/reset")
				.build();
		try {
            Response response = client.newCall(request).execute();
            return JSON.parseObject(response.body().bytes(), RLState.class);
        } catch (Exception e) {
            log.error("", e);
        }
		return null;
	}
}
