package org.dromara.northstar.app.restful.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.common.constant.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 认证链路 e2e 测试（此前零覆盖的关键路径）
 *
 * 覆盖场景：
 * 1. 未认证访问受保护接口 → 403
 * 2. 错误密码登录 → 401（ResultBean 错误体）
 * 3. 正确登录 → 返回 CSRF Token，认证后访问受保护接口 → 200
 * 4. Session 有效但 CSRF Token 错误 → 403
 * 5. 会话校验接口 validate
 * 6. 登出后会话失效
 * 7. /northstar/reset 无需认证（供 e2e 环境复位）
 *
 * 注意：LoginIPLockService 对同一 IP 连续失败 5 次会冻结 1 小时，
 * 本类负面登录用例仅 1 次，且全部 IT 共享同一 Spring 上下文，请勿随意增加失败登录次数。
 */
class AuthFlowIT extends RestE2ESupport {

	@Test
	void 未认证访问受保护接口应返回403() throws Exception {
		mvc.perform(get("/northstar/gateway/all"))
				.andExpect(status().isForbidden());
	}

	@Test
	void 错误密码登录应返回401() throws Exception {
		mvc.perform(post("/northstar/user/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(containsString("\"status\":401")));
	}

	@Test
	void 登录成功后携带会话与Token可访问受保护接口() throws Exception {
		Auth auth = login();
		assertThat(auth.csrfToken()).isNotBlank();

		// 认证后可访问核心端点
		mvc.perform(authed(auth, get("/northstar/gateway/all")))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/metadata/version")))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/metadata/channels")))
				.andExpect(status().isOk());

		// 会话校验
		MvcResult validateResult = mvc.perform(authed(auth, get("/northstar/user/validate")))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(validateResult.getResponse().getContentAsString()).isEqualTo("true");
	}

	@Test
	void 会话有效但Token错误应返回403() throws Exception {
		Auth auth = login();
		// 注意：Token 必须是合法 UUID 格式（拦截器对格式错误的 Token 会抛 IllegalArgumentException → 406）
		mvc.perform(get("/northstar/gateway/all")
					.session(auth.session())
					.header(Constants.CSRF_HEADER, java.util.UUID.randomUUID().toString()))
				.andExpect(status().isForbidden());
	}

	@Test
	void 登出后会话失效应无法再访问() throws Exception {
		Auth auth = login();
		mvc.perform(authed(auth, get("/northstar/user/logout")))
				.andExpect(status().isOk());

		MockHttpSession session = auth.session();
		if (session.isInvalid()) {
			// 会话已失效：旧会话 + 旧 Token 必然无法通过校验
			mvc.perform(get("/northstar/gateway/all")
						.session(session)
						.header(Constants.CSRF_HEADER, auth.csrfToken()))
					.andExpect(status().isForbidden());
		} else {
			// 会话对象未标记失效但服务端已清除：validate 应为 false
			MvcResult validateResult = mvc.perform(authed(auth, get("/northstar/user/validate")))
					.andReturn();
			assertThat(validateResult.getResponse().getContentAsString()).isEqualTo("false");
		}
	}

	@Test
	void reset接口无需认证() throws Exception {
		mvc.perform(delete("/northstar/reset"))
				.andExpect(status().isOk());
	}
}
