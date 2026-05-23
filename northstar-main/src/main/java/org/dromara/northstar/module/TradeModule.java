package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;
import org.slf4j.Logger;

import lombok.extern.slf4j.Slf4j;

/**
 * 交易模组
 * 主要负责模组状态组件管理、更新
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TradeModule implements IModule {
	
	private ModuleContext ctx;
	
	private Set<String> mktGatewayIdSet = new HashSet<>();
	
	private Set<String> accountIdSet = new HashSet<>();
	
	private Set<Contract> bindedContractSet = new HashSet<>();
	
	private Map<String, String> mktToAccountMap = new HashMap<>();
	
	private Map<Contract, IAccount> contractAccountMap = new HashMap<>();
	
	private Logger logger;
	
	public TradeModule(ModuleContext ctx, AccountManager accountMgr, IContractManager contractMgr) {
		this.ctx = ctx;
		ctx.setModule(this);
		logger = ctx.getLogger(getClass());
		ctx.getModuleDescription().getBindedAccounts().forEach(accountId -> { 
			MarketGateway mktGateway = accountMgr.get(Identifier.of(accountId)).getMarketGateway();
			mktGatewayIdSet.add(mktGateway.getDescription().getGatewayId());
			accountIdSet.add(accountId);
			String mktId = mktGateway.getDescription().getGatewayId();
			mktToAccountMap.put(mktId, accountId);
		});
		ctx.getModuleDescription().getBindedContracts().forEach(contract -> {
			IContract ic = contractMgr.getContract(Identifier.of(contract.getId()));
			Contract c = ic.contract();
			if(ic instanceof OptionChainContract) {
				ic.memberContracts()
				.stream()
				.map(IContract::contract)
				.forEach(cc -> {
					bindedContractSet.add(cc);
					String accountId = mktToAccountMap.get(cc.gatewayId());
					if(accountId == null) {
						throw new IllegalStateException(String.format("合约[%s]的渠道来源与模组没有绑定关系：期望[%s]，实际绑定%s", cc.name(), cc.gatewayId(), mktToAccountMap.keySet()));
					}
					contractAccountMap.put(cc, accountMgr.get(Identifier.of(accountId)));
				});
			} 
			bindedContractSet.add(c);
			String accountId = mktToAccountMap.get(c.gatewayId());
			if(accountId == null) {
				throw new IllegalStateException(String.format("合约[%s]的渠道来源与模组没有绑定关系：期望[%s]，实际绑定%s", c.name(), c.gatewayId(), mktToAccountMap.keySet()));
			}
			contractAccountMap.put(c, accountMgr.get(Identifier.of(accountId)));
		});
		logger.info("模组 [{}] 创建成功", ctx.getModuleDescription().getName());
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		if(enabled) {
			contractAccountMap.values().stream().distinct().forEach(acc -> {
				TradeGateway gateway = acc.getTradeGateway();
				if(gateway.getConnection() != ConnectionState.CONNECTED) {
					log.warn("模组 [{}] 启动时，检测到账户 [{}] 没有连线，将自动连线", ctx.getModuleDescription().getName(), gateway.getDescription().getGatewayId());
					CompletableFuture.runAsync(gateway::connect);
				}
			});
		}
		ctx.setEnabled(enabled);
	}

	@Override
	public synchronized void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		try {
			if(data instanceof Tick tick && bindedContractSet.contains(tick.contract()) && mktGatewayIdSet.contains(tick.gatewayId())) {
				ctx.onTick(tick);
			} else if (data instanceof Bar bar && bindedContractSet.contains(bar.contract()) && mktGatewayIdSet.contains(bar.gatewayId())) {
				ctx.onBar(bar);
			} else if (data instanceof Order order && accountIdSet.contains(order.gatewayId())) {
				ctx.onOrder(order);
			} else if (data instanceof Trade trade && accountIdSet.contains(trade.gatewayId())) {
				ctx.onTrade(trade);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public IAccount getAccount(Contract contract) {
		if(!contractAccountMap.containsKey(contract)) {
			throw new IllegalStateException("[" + contract.contractId() + "] 找不到绑定的账户");
		}
		return contractAccountMap.get(contract);
	}

	@Override
	public IModuleContext getModuleContext() {
		return ctx;
	}

}
