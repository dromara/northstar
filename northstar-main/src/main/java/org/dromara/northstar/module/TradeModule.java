package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
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
import org.dromara.northstar.strategy.TradeStrategy;
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
	
	private IModuleContext ctx;
	
	private Set<String> mktGatewayIdSet = new HashSet<>();
	
	private Set<String> accountIdSet = new HashSet<>();
	
	private Set<Contract> bindedContractSet = new HashSet<>();
	
	private ModuleDescription md;
	
	private Map<Contract, IAccount> contractAccountMap = new HashMap<>();
	
	private Logger logger;
	
	public TradeModule(ModuleDescription moduleDescription, IModuleContext ctx, AccountManager accountMgr, IContractManager contractMgr) {
		this.ctx = ctx;
		this.logger = ctx.getLogger(getClass());
		this.md = moduleDescription;
		moduleDescription.getModuleAccountSettingsDescription().forEach(mad -> { 
			MarketGateway mktGateway = accountMgr.get(Identifier.of(mad.getAccountGatewayId())).getMarketGateway();
			mktGatewayIdSet.add(mktGateway.gatewayId());
			accountIdSet.add(mad.getAccountGatewayId());
			mad.getBindedContracts().forEach(contract -> {
				IContract ic = contractMgr.getContract(Identifier.of(contract.getValue()));
				Contract c = ic.contract();
				bindedContractSet.add(c);
				if(ic instanceof OptionChainContract) {
					ic.memberContracts().forEach(cont -> bindedContractSet.add(cont.contract()));
				}
				contractAccountMap.put(c, accountMgr.get(Identifier.of(mad.getAccountGatewayId())));
			});
		});
		ctx.setModule(this);
	}
	
	@Override
	public String getName() {
		return md.getModuleName();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if(enabled) {
			contractAccountMap.values().stream().distinct().forEach(acc -> {
				TradeGateway gateway = acc.getTradeGateway();
				if(gateway.getConnectionState() != ConnectionState.CONNECTED) {
					log.warn("模组 [{}] 启动时，检测到账户 [{}] 没有连线，将自动连线", getName(), gateway.gatewayId());
					CompletableFuture.runAsync(gateway::connect);
				}
			});
		}
		ctx.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return ctx.isEnabled();
	}

	@Override
	public synchronized void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		if(!ctx.isReady()) {
			return;
		}
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
	public ModuleRuntimeDescription getRuntimeDescription() {
		if(!ctx.isReady()) {
			return null;
		}
		return ctx.getRuntimeDescription(true);
	}

	@Override
	public IAccount getAccount(Contract contract) {
		if(!contractAccountMap.containsKey(contract)) {
			throw new NoSuchElementException("[" + contract.contractId() + "] 找不到绑定的账户");
		}
		return contractAccountMap.get(contract);
	}

	@Override
	public ModuleDescription getModuleDescription() {
		return md;
	}

	@Override
	public IModuleContext getModuleContext() {
		return ctx;
	}

	@Override
	public TradeStrategy getTradeStrategy() {
		return ctx.getStrategy();
	}

}
