package org.dromara.northstar.app.restful.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.constant.TradeOperation;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.SubmitOrderDTO;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.alibaba.fastjson2.JSON;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * 手工交易链路 REST e2e 测试
 *
 * 与 TradePlanE2ETest（画线交易计划）互补，本类聚焦下单主链路与异常链路：
 * 1. 未连线账户发单 → 406 业务异常
 * 2. 连线后：投 Tick 定价 → 市价买开 → 持仓断言 → 成交记录查询 → 卖平 → 持仓清零
 * 3. 未认证发单 → 403
 *
 * 行情模拟方式与 TradePlanE2ETest 一致：行情网关只创建不连线（避免随机行情），
 * 直接向事件引擎投递指定价格的 Tick。
 */
class TradeFlowIT extends RestE2ESupport {

	private static final String MKT_GW_ID = "e2eTradeMkt";
	private static final String ACC_GW_ID = "e2eTradeAcc";
	private static final String CONTRACT_ID = "sim9902@SHFE@FUTURES@SIM";
	private static final double BASE_PRICE = 5000;

	private Auth auth;
	private Contract contract;

	@BeforeEach
	void setUp() throws Exception {
		reset();
		auth = login();
		contract = mktCenter.getContract(Identifier.of(CONTRACT_ID)).contract();
	}

	private SubmitOrderDTO order(double volume, double price, TradeOperation action) {
		SubmitOrderDTO dto = new SubmitOrderDTO();
		dto.setGatewayId(ACC_GW_ID);
		dto.setOriginOrderId(UUID.randomUUID().toString());
		dto.setContractId(CONTRACT_ID);
		dto.setVolume(volume);
		dto.setPrice(price);
		dto.setAction(action);
		return dto;
	}

	private void submit(SubmitOrderDTO dto, int expectedStatus) throws Exception {
		mvc.perform(authed(auth, post("/northstar/trade/order/submit")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(dto))))
				.andExpect(status().is(expectedStatus));
	}

	private void mockTick(double price) {
		LocalDateTime now = LocalDateTime.now();
		Tick tick = Tick.builder()
				.gatewayId(MKT_GW_ID)
				.channelType(ChannelType.SIM)
				.contract(contract)
				.lastPrice(price).preSettlePrice(price)
				.openPrice(price).highPrice(price).lowPrice(price)
				.askPrice(List.of(price + 1)).bidPrice(List.of(price - 1))
				.askVolume(List.of(0)).bidVolume(List.of(0))
				.tradingDay(now.toLocalDate()).actionDay(now.toLocalDate())
				.actionTime(now.toLocalTime())
				.actionTimestamp(System.currentTimeMillis())
				.type(TickType.MARKET_TICK)
				.build();
		feEngine.emitEvent(NorthstarEventType.TICK, tick);
	}

	/** 等待期间持续投 Tick（驱动市价单按最新价成交） */
	private void awaitWithTicks(double price, BooleanSupplier condition, String description) {
		long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
		while (System.currentTimeMillis() < deadline) {
			mockTick(price);
			if (condition.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		org.junit.jupiter.api.Assertions.fail(description + "，超时未满足条件");
	}

	private double longPosition() {
		return accMgr.get(Identifier.of(ACC_GW_ID))
				.getPosition(PositionDirectionEnum.PD_Long, contract)
				.map(p -> p.position() - p.frozen())
				.orElse(0D);
	}

	@Test
	void 未连线账户发单应返回406() throws Exception {
		createGateway(auth, simMarketGateway(MKT_GW_ID));
		createGateway(auth, simTradeGateway(ACC_GW_ID, MKT_GW_ID));
		// 不连线直接发单
		submit(order(1, 0, TradeOperation.BK), 406);
	}

	@Test
	void 下单成交平仓全链路() throws Exception {
		createGateway(auth, simMarketGateway(MKT_GW_ID));
		createGateway(auth, simTradeGateway(ACC_GW_ID, MKT_GW_ID));
		mvc.perform(authed(auth, put("/northstar/gateway/deposit/{gatewayId}", ACC_GW_ID)
					.param("amount", "1000000")))
				.andExpect(status().isOk());
		mvc.perform(authed(auth, get("/northstar/gateway/connect/{gatewayId}", ACC_GW_ID)))
				.andExpect(status().isOk());
		awaitCondition(() -> accMgr.get(Identifier.of(ACC_GW_ID)).accountBalance() > 0, "等待模拟账户就绪");

		// 投 Tick 定价，确保 MarketCenter 有最新价（市价单成交依赖）
		awaitWithTicks(BASE_PRICE, () -> mktCenter.lastTick(contract).isPresent(), "等待模拟行情就绪");

		// 市价买开 1 手
		submit(order(1, 0, TradeOperation.BK), 200);
		awaitWithTicks(BASE_PRICE, () -> longPosition() == 1, "等待多头持仓");

		// 卖平 1 手
		submit(order(1, 0, TradeOperation.SP), 200);
		awaitWithTicks(BASE_PRICE, () -> longPosition() == 0, "等待持仓清零");

		// 成交记录（一次完整开平计一笔，事件引擎异步落库，轮询等待）
		awaitWithTicks(BASE_PRICE, () -> {
			try {
				MvcResult r = mvc.perform(authed(auth, get("/northstar/trade/deals")
							.param("accountId", ACC_GW_ID)))
						.andReturn();
				return !JSON.parseArray(r.getResponse().getContentAsString()).isEmpty();
			} catch (Exception e) {
				return false;
			}
		}, "等待成交记录落库");
	}

	@Test
	void 未认证发单应返回403() throws Exception {
		mvc.perform(post("/northstar/trade/order/submit")
					.contentType(MediaType.APPLICATION_JSON)
					.content(JSON.toJSONString(order(1, 0, TradeOperation.BK))))
				.andExpect(status().isForbidden());
	}
}
