package org.dromara.northstar.app.rl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickEntry;
import org.dromara.northstar.gateway.sim.trade.AccountPosition;
import org.dromara.northstar.module.PlaybackModuleContext;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.rl.RLStrategy;

import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * RL 模组沙箱：一个"单合约世界"的最小模组运行时（策略 + 回放上下文 + 模组账户）。
 *
 * 每个 episode 重建一个沙箱（gym reset 语义）：指标对象无法简单快照，
 * 预热段逐 episode 重算；账户权益从初始资金重新开始。
 *
 * 喂数链路与回测一致：每根 bar 生成模拟 tick 序列驱动最新价与持仓浮盈，
 * 再喂 bar 驱动指标与策略；下单以最新价立即全成（回放语义）。
 */
public class RLModuleSandbox {

	private final RLStrategy strategy;
	private final PlaybackModuleContext ctx;
	private final RLModuleAccount account;
	private final InMemoryAccountRepository accountRepo;
	private final Contract contract;
	private final SimplePriceSimulation tickSimulator = new SimplePriceSimulation();

	private RLModuleSandbox(RLStrategy strategy, PlaybackModuleContext ctx, RLModuleAccount account,
			InMemoryAccountRepository accountRepo, Contract contract) {
		this.strategy = strategy;
		this.ctx = ctx;
		this.account = account;
		this.accountRepo = accountRepo;
		this.contract = contract;
	}

	/**
	 * 装配一个沙箱（策略全新实例，InitParams 取默认值）。
	 */
	public static RLModuleSandbox create(String moduleName, RLStrategy strategyTemplate, Contract contract,
			double initBalance, int defaultVolume, IDataSource dataSource) {
		RLStrategy strategy = newStrategyInstance(strategyTemplate);
		InMemoryAccountRepository accountRepo = new InMemoryAccountRepository();

		ModuleDescription md = ModuleDescription.builder()
				.name(moduleName)
				.initBalance(initBalance)
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.PLAYBACK)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.bindedAccounts(List.of())
				.bindedContracts(List.of(ContractSimpleInfo.from(contract)))
				.moduleCacheDataSize(500)
				.defaultVolume(defaultVolume)
				.build();

		Table<Contract, PositionDirectionEnum, AccountPosition> positionTbl = HashBasedTable.create();
		positionTbl.put(contract, PositionDirectionEnum.PD_Long,
				new AccountPosition(moduleName, contract, PositionDirectionEnum.PD_Long, accountRepo));
		positionTbl.put(contract, PositionDirectionEnum.PD_Short,
				new AccountPosition(moduleName, contract, PositionDirectionEnum.PD_Short, accountRepo));

		RLModuleAccount account = new RLModuleAccount(moduleName, initBalance, 0, 0,
				positionTbl, Map.of(contract, new RLDummyAccount()));
		PlaybackModuleContext ctx = new PlaybackModuleContext(strategy, md, account,
				new RLModuleMarketCenter(List.of(contract), dataSource), null);
		strategy.setContext(ctx);
		strategy.setStoreObject(new JSONObject());
		return new RLModuleSandbox(strategy, ctx, account, accountRepo, contract);
	}

	private static RLStrategy newStrategyInstance(RLStrategy template) {
		try {
			RLStrategy strategy = template.getClass().getDeclaredConstructor().newInstance();
			strategy.initWithParams(strategy.getDynamicParams());
			return strategy;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("RL 策略实例化失败: " + template.getClass().getName(), e);
		}
	}

	public RLStrategy strategy() {
		return strategy;
	}

	public PlaybackModuleContext context() {
		return ctx;
	}

	public InMemoryAccountRepository accountRepo() {
		return accountRepo;
	}

	public void setEnabled(boolean enabled) {
		ctx.setEnabled(enabled);
	}

	/** 喂一根 bar：先生成模拟 tick 序列（驱动最新价与持仓浮盈），再喂 bar（驱动指标与策略） */
	public void feed(Bar bar) {
		for (TickEntry entry : tickSimulator.generateFrom(bar)) {
			Tick tick = makeTick(entry, bar);
			ctx.onTick(tick);
			account.onTick(tick);	// 模组账户持仓浮盈依赖最新价
		}
		ctx.onBar(bar);
	}

	/** 模组权益 = 静态权益（初始资金 + 已实现盈亏 - 手续费）+ 持仓浮动盈亏 */
	public double equity() {
		return account.accountBalance() + account.getModulePositions().stream()
				.mapToDouble(AccountPosition::profit)
				.sum();
	}

	/** 决策区间内的成交记录（取出并清空） */
	public List<Trade> drainIntervalTrades() {
		return account.drainIntervalTrades();
	}

	/** 回合边界强平：以最新价平掉全部持仓（truncated 语义下的收尾动作） */
	public void forceClose() {
		for (PositionDirectionEnum dir : List.of(PositionDirectionEnum.PD_Long, PositionDirectionEnum.PD_Short)) {
			account.getPosition(dir, contract).ifPresent(pos -> {
				if (pos.position() > 0) {
					ctx.submitOrderReq(SubmitOrderReq.builder()
							.contract(contract)
							.direction(dir == PositionDirectionEnum.PD_Long ? DirectionEnum.D_Sell : DirectionEnum.D_Buy)
							.offsetFlag(OffsetFlagEnum.OF_Close)
							.volume((int) pos.position())
							.build());
				}
			});
		}
	}

	private Tick makeTick(TickEntry tickEntry, Bar bar) {
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault());
		return Tick.builder()
				.preClosePrice(bar.preClosePrice())
				.preOpenInterest(bar.preOpenInterest())
				.preSettlePrice(bar.preSettlePrice())
				.highPrice(bar.highPrice())
				.lowPrice(bar.lowPrice())
				.contract(bar.contract())
				.tradingDay(bar.tradingDay())
				.actionDay(bar.actionDay())
				.actionTime(ldt.toLocalTime())
				.actionTimestamp(tickEntry.timestamp())
				.type(TickType.PLAYBACK_TICK)
				.lastPrice(tickEntry.price())
				.askPrice(List.of(tickEntry.askPrice0()))
				.bidPrice(List.of(tickEntry.bidPrice0()))
				.askVolume(List.of(ThreadLocalRandom.current().nextInt(10, 500)))
				.bidVolume(List.of(ThreadLocalRandom.current().nextInt(10, 500)))
				.channelType(ChannelType.PLAYBACK)
				.volume(bar.volume())
				.volumeDelta(Math.max(1, tickEntry.volume()))
				.openInterest(bar.openInterest())
				.openInterestDelta(tickEntry.openInterestDelta())
				.turnoverDelta(bar.turnoverDelta())
				.turnover(bar.turnover())
				.build();
	}

	/** 模组绑定的物理账户在 RL 沙箱中无意义（模组账户自包含），提供空实现 */
	private static class RLDummyAccount implements IAccount {
		@Override
		public void onOrder(Order order) { }

		@Override
		public void onTrade(Trade trade) { }

		@Override
		public void submitOrder(SubmitOrderReq orderReq) { }

		@Override
		public boolean cancelOrder(CancelOrderReq cancelReq) {
			return false;
		}

		@Override
		public double accountBalance() {
			return 0;
		}

		@Override
		public double availableBalance() {
			return 0;
		}

		@Override
		public String getId() {
			return "RLDummyAccount";
		}

		@Override
		public java.util.Optional<Position> getPosition(PositionDirectionEnum posDirection, Contract contract) {
			return java.util.Optional.empty();
		}

		@Override
		public List<Order> getPendingOrders() {
			return List.of();
		}

		@Override
		public TradeGateway getTradeGateway() {
			throw new UnsupportedOperationException();
		}
	}
}
