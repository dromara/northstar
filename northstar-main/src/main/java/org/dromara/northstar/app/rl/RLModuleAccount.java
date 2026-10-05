package org.dromara.northstar.app.rl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.module.ModuleAccount;
import org.dromara.northstar.strategy.IAccount;

import com.google.common.collect.Table;

import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

/**
 * RL 训练沙箱的模组账户：在普通模组账户之上记录决策区间内的成交，
 * 供奖励函数显式感知交易成本。
 */
public class RLModuleAccount extends ModuleAccount {

	private final List<Trade> intervalTrades = new ArrayList<>();

	public RLModuleAccount(String moduleName, double initBalance, double initRealizedProfit, double initCommission,
			Table<Contract, PositionDirectionEnum, org.dromara.northstar.gateway.sim.trade.AccountPosition> positionTbl,
			Map<Contract, IAccount> contractAccountMap) {
		super(moduleName, initBalance, initRealizedProfit, initCommission, positionTbl, contractAccountMap);
	}

	@Override
	public void onTrade(Trade trade) {
		super.onTrade(trade);
		intervalTrades.add(trade);
	}

	/** 取出并清空自上次调用以来的成交记录（决策区间结算用） */
	public List<Trade> drainIntervalTrades() {
		List<Trade> result = List.copyOf(intervalTrades);
		intervalTrades.clear();
		return result;
	}
}
