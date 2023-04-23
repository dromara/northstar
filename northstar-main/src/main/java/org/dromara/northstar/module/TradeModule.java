package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 交易模组
 * 主要负责模组状态组件管理、更新
 * 
 * @author KevinHuangwl
 *
 */
public class TradeModule implements IModule {
	
	private IModuleContext ctx;
	
	private Set<String> unifiedSymbolSet = new HashSet<>();
	
	private Set<String> accountIdSet = new HashSet<>();
	
	private ModuleDescription md;
	
	private Map<ContractField, IAccount> contractAccountMap = new HashMap<>();
	
	public TradeModule(ModuleDescription moduleDescription, IModuleContext ctx, AccountManager accountMgr, IContractManager contractMgr) {
		this.ctx = ctx;
		this.md = moduleDescription;
		moduleDescription.getModuleAccountSettingsDescription().forEach(mad -> {
			mad.getBindedContracts().forEach(contract -> {
				unifiedSymbolSet.add(contract.getUnifiedSymbol());
				accountIdSet.add(mad.getAccountGatewayId());
				Contract c = contractMgr.getContract(Identifier.of(contract.getValue()));
				contractAccountMap.put(c.contractField(), accountMgr.get(Identifier.of(mad.getAccountGatewayId())));
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
		ctx.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return ctx.isEnabled();
	}

	@Override
	public synchronized void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		if(data instanceof TickField tick && unifiedSymbolSet.contains(tick.getUnifiedSymbol())) {
			ctx.onTick(tick);
		} else if (data instanceof BarField bar && unifiedSymbolSet.contains(bar.getUnifiedSymbol())) {
			ctx.onBar(bar);
		} else if (data instanceof OrderField order && accountIdSet.contains(order.getGatewayId())) {
			ctx.onOrder(order);
		} else if (data instanceof TradeField trade && accountIdSet.contains(trade.getGatewayId())) {
			ctx.onTrade(trade);
		}
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription() {
		return ctx.getRuntimeDescription(true);
	}

	@Override
	public IAccount getAccount(ContractField contract) {
		if(!contractAccountMap.containsKey(contract)) {
			throw new NoSuchElementException("[" + contract.getContractId() + "] 找不到绑定的账户");
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

}
