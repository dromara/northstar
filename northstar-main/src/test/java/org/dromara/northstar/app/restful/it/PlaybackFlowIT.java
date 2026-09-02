package org.dromara.northstar.app.restful.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * 历史回放 REST e2e 测试（不依赖外部数据服务的部分）
 *
 * 覆盖场景：
 * 1. PLAYBACK 网关不存在时查询状态 → 406
 * 2. 创建 PLAYBACK 网关后：状态为 INITIAL
 * 3. INITIAL 状态直接开始回放 → 500（"需先进行数据预加载"）
 * 4. 调速 / 跳转时间线 / 暂停 / 重置 在 INITIAL 态下可安全调用
 * 5. 历史K线查询：from 晚于回放时间线时直接返回空（不触碰外部数据服务）
 * 6. 删除 PLAYBACK 网关后查询状态恢复 406
 *
 * 说明：preload/start 的完整数据链路依赖外部数据服务（marketplace），
 * 不在本层覆盖；该链路已由 Cypress 13-playback 在有数据服务的环境验证。
 */
class PlaybackFlowIT extends RestE2ESupport {

	private Auth auth;

	@BeforeEach
	void setUp() throws Exception {
		reset();
		auth = login();
	}

	@Test
	void 回放网关不存在时查询状态应返回406() throws Exception {
		mvc.perform(authed(auth, get("/northstar/playback/status")))
				.andExpect(status().isNotAcceptable())
				.andExpect(content().string(containsString("\"status\":406")));
	}

	@Test
	void 回放控制链路() throws Exception {
		createGateway(auth, playbackGateway());

		// 初始状态
		String statusJson = mvc.perform(authed(auth, get("/northstar/playback/status")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JSONObject status = JSON.parseObject(statusJson);
		assertThat(status.getString("state")).isEqualTo("INITIAL");

		// INITIAL 态直接开始回放 → 500（兜底错误响应）
		mvc.perform(authed(auth, get("/northstar/playback/start")))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string(containsString("\"status\":500")));

		// 调速
		mvc.perform(authed(auth, put("/northstar/playback/speed/{interval}", 100)))
				.andExpect(status().isOk());
		String afterSpeed = mvc.perform(authed(auth, get("/northstar/playback/status")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JSON.parseObject(afterSpeed).getIntValue("speed")).isEqualTo(100);

		// 跳转时间线 / 暂停 / 重置在 INITIAL 态下安全
		mvc.perform(authed(auth, get("/northstar/playback/time/{timestamp}", 1682899200000L)))	// 2023-05-01
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/playback/pause")))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/playback/reset")))
				.andExpect(status().isOk());

		String finalStatus = mvc.perform(authed(auth, get("/northstar/playback/status")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JSON.parseObject(finalStatus).getString("state")).isEqualTo("INITIAL");

		// 历史K线：from 晚于回放时间线 → 直接返回空（不访问外部数据服务）
		long future = System.currentTimeMillis() + 86400000L;
		String bars = mvc.perform(authed(auth, get("/northstar/playback/history/{contractId}/{interval}",
						"rb0000@SHFE@FUTURES@PLAYBACK", "1m")
					.param("from", String.valueOf(future))
					.param("to", String.valueOf(future + 86400000L))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JSON.parseArray(bars)).isEmpty();

		// 删除回放网关后恢复 406
		mvc.perform(authed(auth, delete("/northstar/gateway/remove/{gatewayId}", "PLAYBACK")))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/playback/status")))
				.andExpect(status().isNotAcceptable());
	}

	@Test
	void 未认证不能操作回放() throws Exception {
		mvc.perform(get("/northstar/playback/status"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/northstar/playback/reset"))
				.andExpect(status().isForbidden());
	}
}
