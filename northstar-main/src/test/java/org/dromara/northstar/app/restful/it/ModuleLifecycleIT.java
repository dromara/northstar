package org.dromara.northstar.app.restful.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleMockTradeDTO;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

/**
 * 模组全生命周期 REST e2e 测试
 *
 * 覆盖场景：
 * 1. 创建模组（模拟盘用途 + 测试策略 + SIM 模拟账户 + SIM 合约）
 * 2. 重名创建 → 406
 * 3. 模组列表 / 运行时状态 / 交易记录 / 图表描述 接口可达
 * 4. 启用 / 停用
 * 5. 手工调仓（mocktrade 开多）→ 模组持仓与状态变化
 * 6. 停用后移除模组
 *
 * 说明：weeksOfDataForPreparation 必须为 0，避免从外部数据服务拉取预热数据；
 * 策略使用 TestStrategy（无必填参数），通过反射实例化。
 */
class ModuleLifecycleIT extends RestE2ESupport {

	private static final String MKT_GW_ID = "e2eModuleMkt";
	private static final String ACC_GW_ID = "e2eModuleAcc";
	private static final String MODULE_NAME = "e2e模组";
	private static final String CONTRACT_ID = "sim9902@SHFE@FUTURES@SIM";

	private Auth auth;

	@BeforeEach
	void setUp() throws Exception {
		reset();
		auth = login();
		// 准备模拟账户
		createGateway(auth, simMarketGateway(MKT_GW_ID));
		createGateway(auth, simTradeGateway(ACC_GW_ID, MKT_GW_ID));
		mvc.perform(authed(auth, put("/northstar/gateway/deposit/{gatewayId}", ACC_GW_ID)
					.param("amount", "1000000")))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/gateway/connect/{gatewayId}", ACC_GW_ID)))
				.andExpect(status().isOk());
		awaitCondition(() -> accMgr.get(Identifier.of(ACC_GW_ID)).accountBalance() > 0, "等待模拟账户就绪");
	}

	private ModuleDescription moduleDescription() {
		return ModuleDescription.builder()
				.name(MODULE_NAME)
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.UAT)
				.weeksOfDataForPreparation(0)	// 避免拉取外部预热数据
				.moduleCacheDataSize(500)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.initBalance(10000)
				.defaultVolume(1)
				.orderPlusTick(0)
				.bindedAccounts(List.of(ACC_GW_ID))
				.bindedContracts(List.of(ContractSimpleInfo.builder()
						.id(CONTRACT_ID)
						.name("模拟合约9902")
						.symbol("sim9902")
						.unifiedSymbol("sim9902@SHFE@FUTURES")
						.channelType(ChannelType.SIM)
						.build()))
				.strategySetting(StrategyMetaInfo.builder()
						.name("测试策略")
						.className("org.dromara.northstar.strategy.example.TestStrategy")
						.settings(Collections.emptyList())
						.build())
				.build();
	}

	@Test
	void 模组全生命周期() throws Exception {
		// 创建模组
		mvc.perform(authed(auth, post("/northstar/module")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(moduleDescription()))))
				.andExpect(status().isOk());

		// 列表包含该模组
		String listJson = mvc.perform(authed(auth, get("/northstar/module")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JSON.parseArray(listJson)).hasSize(1);

		// 启用模组
		mvc.perform(authed(auth, get("/northstar/module/{name}/enable", MODULE_NAME)))
				.andExpect(status().isOk());

		// 运行时状态 / 交易记录 / 图表描述 接口可达
		mvc.perform(authed(auth, get("/northstar/module/{name}/rt", MODULE_NAME)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("initBalance")));
		mvc.perform(authed(auth, get("/northstar/module/{name}/deals", MODULE_NAME)))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/module/{name}/chart", MODULE_NAME)))
				.andExpect(status().isOk());

		// 手工调仓：开多 1 手
		ModuleMockTradeDTO mockTrade = ModuleMockTradeDTO.builder()
				.moduleName(MODULE_NAME)
				.item(moduleDescription().getBindedContracts().get(0))
				.price(5000)
				.volume(1)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();
		mvc.perform(authed(auth, post("/northstar/module/mocktrade")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(mockTrade))))
				.andExpect(status().isOk());

		// 模组状态应变为多头持仓
		awaitCondition(() -> {
			try {
				String json = mvc.perform(authed(auth, get("/northstar/module")))
						.andReturn().getResponse().getContentAsString();
				return JSON.parseArray(json).toList(org.dromara.northstar.common.model.ModuleVO.class)
						.stream().anyMatch(m -> "HOLDING_LONG".equals(m.getState().name()));
			} catch (Exception e) {
				return false;
			}
		}, "等待模组状态变为多头持仓");

		// 停用并移除
		mvc.perform(authed(auth, get("/northstar/module/{name}/disable", MODULE_NAME)))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, delete("/northstar/module").param("moduleName", MODULE_NAME)))
				.andExpect(status().isOk());

		String after = mvc.perform(authed(auth, get("/northstar/module")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(JSON.parseArray(after)).isEmpty();
	}

	@Test
	void 重名创建模组应返回406() throws Exception {
		String body = JSON.toJSONString(moduleDescription());
		mvc.perform(authed(auth, post("/northstar/module")
					.contentType(MediaType.APPLICATION_JSON).content(body)))
				.andExpect(status().isOk());

		mvc.perform(authed(auth, post("/northstar/module")
					.contentType(MediaType.APPLICATION_JSON).content(body)))
				.andExpect(status().isNotAcceptable())
				.andExpect(content().string(containsString("\"status\":406")));
	}

	@Test
	void 缺少必填项创建模组应被拦截() throws Exception {
		// 不绑定账户与合约
		// 注：当前实现对缺省字段返回 500（NPE 兜底），而非 4xx 参数校验，属于待改进项（见差距分析）
		ModuleDescription incomplete = ModuleDescription.builder()
				.name("非法模组")
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.UAT)
				.weeksOfDataForPreparation(0)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.strategySetting(StrategyMetaInfo.builder()
						.name("测试策略")
						.className("org.dromara.northstar.strategy.example.TestStrategy")
						.settings(Collections.emptyList())
						.build())
				.build();
		mvc.perform(authed(auth, post("/northstar/module")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(incomplete))))
				.andExpect(status().is5xxServerError());
	}

	@Test
	void 未认证不能操作模组() throws Exception {
		mvc.perform(get("/northstar/module"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/northstar/module/{name}/enable", MODULE_NAME))
				.andExpect(status().isForbidden());
	}
}
