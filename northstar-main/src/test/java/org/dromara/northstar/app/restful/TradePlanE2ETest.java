package org.dromara.northstar.app.restful;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.account.TradeAccount;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.constant.TradeAction;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LineTraderDTO;
import org.dromara.northstar.common.model.LineTraderDTO.State;
import org.dromara.northstar.common.model.SubmitOrderDTO;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.strategy.IMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 画线交易计划的 Controller 层端到端测试
 *
 * 与 Cypress 版本（northstar-monitor-next/cypress/e2e/07-trading.cy.ts 已移除的"画线交易计划测试"）的区别：
 * Cypress 版本依赖 SIM 行情网关产生的随机行情，价位不可控导致用例不稳定；
 * 本测试不连线行情网关，改为直接向事件引擎投递指定价格的 Tick（mock 行情数据），
 * 从而精确控制行情穿越、止盈止损触发等时序，操作全部通过 Controller 接口完成。
 *
 * 覆盖场景：
 * 1. 创建组合交易计划（入场价 + 止盈价 + 止损价）→ 行情穿越触发入场委托并成交
 * 2. 入场触发后，模拟前端收到 LINE_TRADER_RUNNING 重新保存同ID计划（画线变色同步/拖动更新），
 *    计划不应被误终结而消失（回归场景）
 * 3. 再次重新保存同ID计划（等价于拖动止损线），止损触发后自动平仓，计划完成并从列表移除
 * 4. 变更组合计划的止损价与止盈价，计划不消失且价位更新为新值
 * 5. 单独止损线：对已持仓位进行止损监控，变更止损价后按新价位自动平仓
 * 6. 单独止盈线：对已持仓位进行止盈监控，变更止盈价后按新价位自动平仓
 * 7. 手动删除交易计划
 */
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class TradePlanE2ETest {

	private static final String MKT_GW_ID = "SIM";					// 行情网关（只创建不连线，避免产生随机行情）
	private static final String ACC_GW_ID = "ltTestAcc";			// 模拟交易账户
	private static final String CONTRACT_ID = "sim9902@SHFE@FUTURES@SIM";
	private static final String CONTRACT_NAME = "模拟合约9902";

	private static final String PLAN_ID = "e2e-line-trader-1";
	private static final String PLAN_ID2 = "e2e-line-trader-2";
	private static final String PLAN_ID_SL = "e2e-line-trader-sl";
	private static final String PLAN_ID_TP = "e2e-line-trader-tp";

	private static final double BASE_PRICE = 5000;
	private static final long AWAIT_TIMEOUT_MS = 15000;

	@Autowired
	MockMvc mvc;

	@Autowired
	FastEventEngine feEngine;

	@Autowired
	MarketCenter mktCenter;

	@Autowired
	AccountManager accMgr;

	@Autowired
	GatewayManager gatewayMgr;

	@MockitoBean
	IMessageSender sender;

	Contract contract;

	@BeforeEach
	void setUp() throws Exception {
		ensureGatewaysReady();
		contract = mktCenter.getContract(Identifier.of(CONTRACT_ID)).contract();
		// 推送基准行情，确保 MarketCenter 存在最新价（市价单成交依赖该价格）
		awaitWithTicks(BASE_PRICE, () -> mktCenter.lastTick(contract).isPresent(), "等待模拟行情就绪");
		// 清理可能残留的持仓与交易计划，保证用例间互不影响
		closeLeftoverPosition();
		for (LineTraderDTO plan : queryPlans()) {
			deletePlan(plan.getId());
		}
	}

	/**
	 * 组合交易计划触发入场后不消失，重新保存后仍继续监控，止损触发后自动完成
	 */
	@Test
	void testComboPlanTriggeredThenResaveThenStopLoss() throws Exception {
		int dealsBefore = queryDeals().size();

		// 入场价设在现价下方，等待行情向下穿越触发；止损价远离现价不会误触发
		LineTraderDTO plan = comboPlan(PLAN_ID, 4990, 4996, 4700, State.STANDBY);
		savePlan(plan);

		// 先让计划观察到入场价上方的行情，再向下穿越入场价，触发入场委托（状态变为 RUNNING）
		mockTick(BASE_PRICE);
		mockTick(4985);
		awaitPlanState(PLAN_ID, State.RUNNING);

		// 继续推送行情驱动入场限价单成交，并等待持仓推送到达。
		// 此阶段原始计划的入场订单号仍在跟踪中，计划不会被误终结
		awaitWithTicks(4985, () -> longPosition() == 1, "等待入场委托成交与持仓推送");

		// 回归场景：模拟前端收到 LINE_TRADER_RUNNING 后立即重新保存同ID计划（画线变色同步）
		plan.setState(State.RUNNING);
		savePlan(plan);

		// 继续推送数个 Tick 后，计划不应消失，且保持运行中
		mockTick(4985);
		mockTick(4986);
		mockTick(4985);
		Thread.sleep(500);	// 等待事件引擎处理完毕
		LineTraderDTO p = findPlan(PLAN_ID);
		assertThat(p).as("入场触发并重新保存后，交易计划不应消失").isNotNull();
		assertThat(p.getState()).as("交易计划应保持运行中").isEqualTo(State.RUNNING);

		// 再次重新保存同ID计划（等价于拖动止损线），将止损价调整至现价上方，
		// 计划应继续监控并触发自动止损平仓，完成后从列表移除
		plan.setOrderPrice(null);
		plan.setStopLossPrice(4990D);
		savePlan(plan);
		mockTick(4985);		// 4985 <= 4990 触发多头止损
		awaitPlanRemoved(PLAN_ID);

		// 持仓已被自动平掉，开平配对产生一笔交易记录
		awaitCondition(() -> longPosition() == 0, "等待持仓被自动平掉");
		assertThat(queryDeals()).as("开平后应新增一笔交易记录").hasSize(dealsBefore + 1);
	}

	/**
	 * 能变更组合交易计划的止损价与止盈价
	 */
	@Test
	void testUpdateComboPlanPrices() throws Exception {
		mockTick(BASE_PRICE);
		// 入场价远离现价，计划保持就绪状态不会触发
		LineTraderDTO plan = comboPlan(PLAN_ID2, 5100, 5150, 5050, State.STANDBY);
		savePlan(plan);

		// 变更止损价与止盈价（等价于拖动止损线与止盈线）
		plan.setStopLossPrice(5070D);
		plan.setTakeProfitPrice(5130D);
		savePlan(plan);

		// 行情不触及入场价，计划应唯一存在、保持就绪，且价位已更新为新值
		mockTick(BASE_PRICE);
		Thread.sleep(500);	// 等待事件引擎处理完毕
		List<LineTraderDTO> matched = queryPlans().stream().filter(p -> PLAN_ID2.equals(p.getId())).toList();
		assertThat(matched).as("变更价位后交易计划不应消失或重复").hasSize(1);
		assertThat(matched.get(0).getState()).as("交易计划应保持就绪").isEqualTo(State.STANDBY);
		assertThat(matched.get(0).getStopLossPrice()).as("止损价应已更新").isEqualTo(5070D);
		assertThat(matched.get(0).getTakeProfitPrice()).as("止盈价应已更新").isEqualTo(5130D);

		// 清理：删除计划，避免影响后续用例
		deletePlan(PLAN_ID2);
		assertThat(findPlan(PLAN_ID2)).as("计划应已删除").isNull();
	}

	/**
	 * 单独止损线能对持仓进行止损监控，变更止损价后按新价位止损
	 */
	@Test
	void testStandaloneStopLoss() throws Exception {
		mockTick(BASE_PRICE);
		// 先持有一手多头
		submitOrder(TradeAction.BK, 1, 0);
		awaitCondition(() -> longPosition() == 1, "等待市价买开成交");

		// 创建仅含止损价的交易计划（等价于单独止损线），止损价远离现价不会误触发
		LineTraderDTO plan = simplePlan(PLAN_ID_SL, 4950D, null, State.STANDBY);
		savePlan(plan);

		// 已持有仓位且无入场价，计划应直接进入运行中
		awaitPlanState(PLAN_ID_SL, State.RUNNING);

		// 继续推送行情，计划不应被误终结，且保持运行中
		mockTick(BASE_PRICE);
		mockTick(5001);
		Thread.sleep(500);	// 等待事件引擎处理完毕
		LineTraderDTO p = findPlan(PLAN_ID_SL);
		assertThat(p).as("止损线计划不应消失").isNotNull();
		assertThat(p.getState()).as("止损线计划应保持运行中").isEqualTo(State.RUNNING);

		// 变更止损价至现价上方（等价于拖动止损线），应立即触发多头止损平仓
		plan.setStopLossPrice(5005D);
		plan.setState(State.RUNNING);
		savePlan(plan);
		mockTick(BASE_PRICE);	// 5000 <= 5005 触发多头止损
		awaitPlanRemoved(PLAN_ID_SL);

		// 持仓已被自动平掉
		awaitCondition(() -> longPosition() == 0, "等待持仓被自动平掉");
	}

	/**
	 * 单独止盈线能对持仓进行止盈监控，变更止盈价后按新价位止盈
	 */
	@Test
	void testStandaloneTakeProfit() throws Exception {
		mockTick(BASE_PRICE);
		// 先持有一手多头
		submitOrder(TradeAction.BK, 1, 0);
		awaitCondition(() -> longPosition() == 1, "等待市价买开成交");

		// 创建仅含止盈价的交易计划（等价于单独止盈线），止盈价远离现价不会误触发
		LineTraderDTO plan = simplePlan(PLAN_ID_TP, null, 5050D, State.STANDBY);
		savePlan(plan);

		// 已持有仓位且无入场价，计划应直接进入运行中
		awaitPlanState(PLAN_ID_TP, State.RUNNING);

		// 继续推送行情，计划不应被误终结，且保持运行中
		mockTick(BASE_PRICE);
		mockTick(4999);
		Thread.sleep(500);	// 等待事件引擎处理完毕
		LineTraderDTO p = findPlan(PLAN_ID_TP);
		assertThat(p).as("止盈线计划不应消失").isNotNull();
		assertThat(p.getState()).as("止盈线计划应保持运行中").isEqualTo(State.RUNNING);

		// 变更止盈价至现价下方（等价于拖动止盈线），应立即触发多头止盈平仓
		plan.setTakeProfitPrice(4995D);
		plan.setState(State.RUNNING);
		savePlan(plan);
		mockTick(BASE_PRICE);	// 5000 >= 4995 触发多头止盈
		awaitPlanRemoved(PLAN_ID_TP);

		// 持仓已被自动平掉
		awaitCondition(() -> longPosition() == 0, "等待持仓被自动平掉");
	}

	/**
	 * 能手动删除交易计划
	 */
	@Test
	void testManualDeletePlan() throws Exception {
		// 入场价远离现价，计划保持就绪状态不会触发
		savePlan(comboPlan(PLAN_ID, 4000, 4100, 3900, State.STANDBY));

		LineTraderDTO p = findPlan(PLAN_ID);
		assertThat(p).as("交易计划应存在").isNotNull();
		assertThat(p.getState()).as("交易计划应保持就绪").isEqualTo(State.STANDBY);

		deletePlan(PLAN_ID);
		assertThat(findPlan(PLAN_ID)).as("交易计划应已删除").isNull();
	}

	/* ==================== 网关与账户准备 ==================== */

	private void ensureGatewaysReady() throws Exception {
		if (Objects.isNull(gatewayMgr.get(Identifier.of(MKT_GW_ID)))) {
			// 行情网关只创建不连线，行情完全由测试投递的 Tick 决定
			GatewayDescription gd = GatewayDescription.builder()
					.gatewayId(MKT_GW_ID)
					.gatewayUsage(GatewayUsage.MARKET_DATA)
					.channelType(ChannelType.SIM)
					.settings(new Object())
					.build();
			mvc.perform(post("/northstar/gateway/create")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(gd)))
				.andExpect(status().isOk());
		}
		if (Objects.isNull(gatewayMgr.get(Identifier.of(ACC_GW_ID)))) {
			GatewayDescription gd = GatewayDescription.builder()
					.gatewayId(ACC_GW_ID)
					.gatewayUsage(GatewayUsage.TRADE)
					.channelType(ChannelType.SIM)
					.bindedMktGatewayId(MKT_GW_ID)
					.settings(new Object())
					.build();
			mvc.perform(post("/northstar/gateway/create")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(gd)))
				.andExpect(status().isOk());
			mvc.perform(put("/northstar/gateway/deposit/{gatewayId}", ACC_GW_ID).param("amount", "1999999"))
				.andExpect(status().isOk());
		}
		Gateway gateway = gatewayMgr.get(Identifier.of(ACC_GW_ID));
		if (gateway.getConnection() != ConnectionState.CONNECTED) {
			mvc.perform(get("/northstar/gateway/connect/{gatewayId}", ACC_GW_ID))
				.andExpect(status().isOk());
		}
		// 账户状态依赖模拟网关的定时上报，等待账户就绪（可用于下单）
		awaitCondition(() -> {
			TradeAccount acc = accMgr.get(Identifier.of(ACC_GW_ID));
			return acc != null && acc.accountBalance() > 0;
		}, "等待模拟账户就绪");
	}

	/* ==================== Controller 接口操作 ==================== */

	private void savePlan(LineTraderDTO plan) throws Exception {
		mvc.perform(post("/northstar/trade/plan")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(plan)))
			.andExpect(status().isOk());
	}

	private List<LineTraderDTO> queryPlans() throws Exception {
		String json = mvc.perform(get("/northstar/trade/plan")
				.param("accountId", ACC_GW_ID)
				.param("contractId", CONTRACT_ID))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JSON.parseArray(json, LineTraderDTO.class);
	}

	private void deletePlan(String id) throws Exception {
		mvc.perform(delete("/northstar/trade/plan").param("id", id))
			.andExpect(status().isOk());
	}

	private LineTraderDTO findPlan(String id) throws Exception {
		return queryPlans().stream().filter(p -> id.equals(p.getId())).findFirst().orElse(null);
	}

	private JSONArray queryDeals() throws Exception {
		String json = mvc.perform(get("/northstar/trade/deals").param("accountId", ACC_GW_ID))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return JSON.parseArray(json);
	}

	private void submitOrder(TradeAction action, double volume, double price) throws Exception {
		SubmitOrderDTO dto = new SubmitOrderDTO();
		dto.setGatewayId(ACC_GW_ID);
		dto.setOriginOrderId(UUID.randomUUID().toString());
		dto.setContractId(CONTRACT_ID);
		dto.setVolume(volume);
		dto.setPrice(price);
		dto.setAction(action);
		mvc.perform(post("/northstar/trade/order/submit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(dto)))
			.andExpect(status().isOk());
	}

	/* ==================== 模拟行情 ==================== */

	/** 向事件引擎投递一个指定价格的 Tick（mock 行情数据） */
	private void mockTick(double price) {
		LocalDateTime now = LocalDateTime.now();
		Tick tick = Tick.builder()
				.gatewayId(MKT_GW_ID)
				.channelType(ChannelType.SIM)
				.contract(contract)
				.lastPrice(price)
				.preSettlePrice(price)
				.openPrice(price)
				.highPrice(price)
				.lowPrice(price)
				.askPrice(List.of(price + 1))
				.bidPrice(List.of(price - 1))
				.askVolume(List.of(0))
				.bidVolume(List.of(0))
				.tradingDay(now.toLocalDate())
				.actionDay(now.toLocalDate())
				.actionTime(now.toLocalTime())
				.actionTimestamp(System.currentTimeMillis())
				.type(TickType.MARKET_TICK)
				.build();
		feEngine.emitEvent(NorthstarEventType.TICK, tick);
	}

	/* ==================== 断言与等待工具 ==================== */

	private double longPosition() {
		TradeAccount acc = accMgr.get(Identifier.of(ACC_GW_ID));
		if (Objects.isNull(acc)) {
			return 0;
		}
		return acc.getPosition(PositionDirectionEnum.PD_Long, contract).map(p -> p.position() - p.frozen()).orElse(0D);
	}

	private void closeLeftoverPosition() throws Exception {
		double vol = longPosition();
		if (vol > 0) {
			submitOrder(TradeAction.SP, vol, 0);
			awaitCondition(() -> longPosition() == 0, "等待残留持仓平仓");
		}
	}

	private void awaitPlanState(String planId, State state) throws Exception {
		awaitCondition(() -> {
			LineTraderDTO p = findPlanUnchecked(planId);
			return p != null && p.getState() == state;
		}, String.format("等待交易计划[%s]状态变为[%s]", planId, state));
	}

	private void awaitPlanRemoved(String planId) throws Exception {
		awaitCondition(() -> findPlanUnchecked(planId) == null, String.format("等待交易计划[%s]完成后移除", planId));
	}

	private void awaitCondition(BooleanSupplier condition, String description) {
		long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			sleep(100);
		}
		fail(description + "，超时未满足条件");
	}

	/** 在等待条件满足的同时持续推送指定价格的行情（用于驱动限价单成交） */
	private void awaitWithTicks(double price, BooleanSupplier condition, String description) {
		long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
		while (System.currentTimeMillis() < deadline) {
			mockTick(price);
			sleep(150);
			if (condition.getAsBoolean()) {
				return;
			}
		}
		fail(description + "，超时未满足条件");
	}

	private LineTraderDTO findPlanUnchecked(String planId) {
		try {
			return findPlan(planId);
		} catch (Exception e) {
			return null;
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/* ==================== 交易计划构造 ==================== */

	/** 构造组合交易计划（入场价 + 止盈价 + 止损价） */
	private LineTraderDTO comboPlan(String id, double entryPrice, double takeProfit, double stopLoss, State state) {
		return LineTraderDTO.builder()
				.id(id)
				.positionDir("long")
				.orderPrice(entryPrice)
				.stopLossPrice(stopLoss)
				.takeProfitPrice(takeProfit)
				.accountGatewayId(ACC_GW_ID)
				.contractId(CONTRACT_ID)
				.contractName(CONTRACT_NAME)
				.pricePrecision(2)
				.isAnyPrice(false)
				.volume(1)
				.state(state)
				.build();
	}

	/** 构造简单交易计划（仅止损线或仅止盈线，无入场价） */
	private LineTraderDTO simplePlan(String id, Double stopLoss, Double takeProfit, State state) {
		return LineTraderDTO.builder()
				.id(id)
				.positionDir("long")
				.orderPrice(null)
				.stopLossPrice(stopLoss)
				.takeProfitPrice(takeProfit)
				.accountGatewayId(ACC_GW_ID)
				.contractId(CONTRACT_ID)
				.contractName(CONTRACT_NAME)
				.pricePrecision(2)
				.isAnyPrice(false)
				.volume(1)
				.state(state)
				.build();
	}

}
