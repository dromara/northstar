package org.dromara.northstar.app.restful.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.common.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

/**
 * 网关全生命周期 REST e2e 测试
 *
 * 覆盖场景：
 * 1. 创建 SIM 行情网关 / SIM 交易网关（模拟账户）
 * 2. 重名创建 → 406 业务异常
 * 3. 删除被交易网关绑定的行情网关 → 406 业务异常
 * 4. 模拟账户出入金
 * 5. 网关连线 → 模拟账户就绪（余额上报）
 * 6. 断开 → 删除 → 列表清空
 */
class GatewayLifecycleIT extends RestE2ESupport {

	private static final String MKT_GW_ID = "e2eMkt";
	private static final String ACC_GW_ID = "e2eAcc";

	private Auth auth;

	@BeforeEach
	void setUp() throws Exception {
		reset();
		auth = login();
	}

	@Test
	void 网关全生命周期() throws Exception {
		// 创建行情网关与交易网关
		createGateway(auth, simMarketGateway(MKT_GW_ID));
		createGateway(auth, simTradeGateway(ACC_GW_ID, MKT_GW_ID));

		// 网关列表应包含两个网关
		String json = mvc.perform(authed(auth, get("/northstar/gateway/all")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JSONArray gateways = JSON.parseArray(json);
		assertThat(gateways).hasSize(2);

		// 出入金：入金 100 万
		String balance = mvc.perform(authed(auth, put("/northstar/gateway/deposit/{gatewayId}", ACC_GW_ID)
					.param("amount", "1000000")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(Double.parseDouble(balance)).isGreaterThan(0);

		// 连线交易网关，等待模拟账户余额就绪
		mvc.perform(authed(auth, get("/northstar/gateway/connect/{gatewayId}", ACC_GW_ID)))
				.andExpect(status().isOk());
		awaitCondition(() -> accMgr.get(Identifier.of(ACC_GW_ID)).accountBalance() > 0, "等待模拟账户就绪");

		// 断开并删除交易网关
		mvc.perform(authed(auth, get("/northstar/gateway/disconnect/{gatewayId}", ACC_GW_ID)))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, delete("/northstar/gateway/remove/{gatewayId}", ACC_GW_ID)))
				.andExpect(status().isOk());

		// 删除行情网关
		mvc.perform(authed(auth, delete("/northstar/gateway/remove/{gatewayId}", MKT_GW_ID)))
				.andExpect(status().isOk());

		String after = mvc.perform(authed(auth, get("/northstar/gateway/all")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JSON.parseArray(after)).isEmpty();
	}

	@Test
	void 重名创建网关应返回406() throws Exception {
		createGateway(auth, simMarketGateway(MKT_GW_ID));

		mvc.perform(authed(auth, post("/northstar/gateway/create")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(simMarketGateway(MKT_GW_ID)))))
				.andExpect(status().isNotAcceptable())
				.andExpect(content().string(containsString("\"status\":406")));
	}

	@Test
	void 删除被交易网关绑定的行情网关应返回406() throws Exception {
		createGateway(auth, simMarketGateway(MKT_GW_ID));
		createGateway(auth, simTradeGateway(ACC_GW_ID, MKT_GW_ID));

		mvc.perform(authed(auth, delete("/northstar/gateway/remove/{gatewayId}", MKT_GW_ID)))
				.andExpect(status().isNotAcceptable())
				.andExpect(content().string(containsString("\"status\":406")));
	}

	@Test
	void 未认证不能操作网关() throws Exception {
		mvc.perform(post("/northstar/gateway/create")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(simMarketGateway(MKT_GW_ID))))
				.andExpect(status().isForbidden());
		mvc.perform(get("/northstar/gateway/all"))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/northstar/gateway/remove/{gatewayId}", MKT_GW_ID))
				.andExpect(status().isForbidden());
	}
}
