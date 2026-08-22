package org.dromara.northstar.app.restful.it;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.strategy.IMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.alibaba.fastjson2.JSON;

/**
 * RESTful API 端到端测试支撑类（无界面，直连 Controller 层）
 *
 * 与 unittest profile 的测试不同，本层统一使用 e2e profile：
 * - AuthCheckerInterceptor 会被注册（/northstar/** 需要登录 + CSRF Token），从而覆盖真实认证链路；
 * - ResetController 生效，可用 DELETE /northstar/reset 做用例隔离；
 * - MockMvc 不经过 SSL 连接器，e2e profile 的 SSL 配置不影响测试。
 *
 * 命名 *IT.java 由 maven-failsafe-plugin 在 integration-test 阶段执行（mvn verify），
 * 与 surefire 执行的 *Test.java 单元测试分离。
 */
@SpringBootTest(classes = NorthstarApplication.class, value = "spring.profiles.active=e2e")
@AutoConfigureMockMvc
abstract class RestE2ESupport {

	protected static final long AWAIT_TIMEOUT_MS = 15000;

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected FastEventEngine feEngine;

	@Autowired
	protected MarketCenter mktCenter;

	@Autowired
	protected AccountManager accMgr;

	@Autowired
	protected GatewayManager gatewayMgr;

	@MockBean
	protected IMessageSender sender;

	/** 登录凭证：会话 + CSRF Token */
	protected record Auth(MockHttpSession session, String csrfToken) {}

	/** 走真实登录接口获取会话与 CSRF Token（响应体为 JSON 字符串，需去除引号） */
	protected Auth login() throws Exception {
		MvcResult result = mvc.perform(post("/northstar/user/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"admin\",\"password\":\"123456\"}"))
				.andExpect(status().isOk())
				.andReturn();
		String token = result.getResponse().getContentAsString().replace("\"", "");
		return new Auth((MockHttpSession) result.getRequest().getSession(), token);
	}

	/** 为请求附加认证信息（同一 Session + CSRF_token 头） */
	protected MockHttpServletRequestBuilder authed(Auth auth, MockHttpServletRequestBuilder builder) {
		return builder.session(auth.session()).header(Constants.CSRF_HEADER, auth.csrfToken());
	}

	/** 用 ResetController 复位环境（该路径被拦截器放行，无需认证） */
	protected void reset() throws Exception {
		mvc.perform(delete("/northstar/reset")).andExpect(status().isOk());
	}

	/** 事件引擎是异步的，统一轮询等待条件满足 */
	protected void awaitCondition(BooleanSupplier condition, String description) {
		long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail(description + "，等待被中断");
			}
		}
		fail(description + "，超时未满足条件");
	}

	/* ---------- 网关构造工具 ---------- */

	/** SIM 行情网关（不自动连线，避免产生随机行情）
	 *  settings 统一用空 Map：fastjson2 对 Object 类型字段会按首次遇到的运行时类缓存写入器，
	 *  若先用 new Object() 序列化，会导致后续 Map 类型的 settings 被序列化为 {} */
	protected GatewayDescription simMarketGateway(String gatewayId) {
		return GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.channelType(ChannelType.SIM)
				.settings(java.util.Collections.emptyMap())
				.build();
	}

	/** SIM 交易网关（= 一个模拟账户） */
	protected GatewayDescription simTradeGateway(String gatewayId, String bindedMktGatewayId) {
		return GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.TRADE)
				.channelType(ChannelType.SIM)
				.bindedMktGatewayId(bindedMktGatewayId)
				.settings(java.util.Collections.emptyMap())
				.build();
	}

	/** PLAYBACK 回放网关（回放区间 20230501~20230506，低精度极速，不真正预加载则无需外部数据服务）
	 *  settings 用 Map 传递（与前端表单提交的 JSON 结构一致），避免序列化器对配置类的差异 */
	protected GatewayDescription playbackGateway() {
		java.util.Map<String, Object> settings = new java.util.LinkedHashMap<>();
		settings.put("preStartDate", "20230401");
		settings.put("startDate", "20230501");
		settings.put("endDate", "20230506");
		settings.put("precision", "LOW");
		settings.put("speed", "RUSH");
		return GatewayDescription.builder()
				.gatewayId("PLAYBACK")
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.channelType(ChannelType.PLAYBACK)
				.settings(settings)
				.subscribedContracts(List.of(ContractSimpleInfo.builder()
						.id("rb0000@SHFE@FUTURES@PLAYBACK")
						.name("螺纹钢指数")
						.symbol("rb0000")
						.unifiedSymbol("rb0000@SHFE@FUTURES")
						.channelType(ChannelType.PLAYBACK)
						.build()))
				.build();
	}

	/** 通过 REST 创建网关并断言成功 */
	protected void createGateway(Auth auth, GatewayDescription gd) throws Exception {
		MvcResult result = mvc.perform(authed(auth, post("/northstar/gateway/create")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(gd))))
				.andReturn();
		org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
				.as("创建网关" + gd.getGatewayId() + "失败：" + result.getResponse().getContentAsString())
				.isEqualTo(200);
	}
}
