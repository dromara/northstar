package tech.quantit.northstar.domain.module;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleContext;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/*
 * 难点：
 * 1. 模组启停状态，如何能既不影响行情数据更新，又能限制指令发送	DONE（通过TradeStrategy的接入传入启停状态，让策略自行处理）
 * 2. 外部需要观察模组当前运行的各种状态，要如何封装描述 		DONE（通过ModuleDescription封装）
 * 3. 模组可能存在多个交易所，根据合约价差在多个交易所下单		DONE（多个交易所网关在context中保存，由于策略下单时指定）
 * 4. 策略、风控、下单模块怎么整合
 * 
 * */

/**
 * 交易模组
 * 主要负责模组状态组件管理、更新
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TradeModule implements IModule {
	
	private boolean enabled;
	
	private IModuleContext ctx;
	
	private Set<String> mktGatewayIdSet;
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback;
	
	public TradeModule(IModuleContext context, Set<MarketGateway> mktGatewaySet, Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback) {
		this.ctx = context;
		this.mktGatewayIdSet = mktGatewaySet.stream().map(mktGateway -> mktGateway.getGatewaySetting().getGatewayId()).collect(Collectors.toSet());
		this.onRuntimeChangeCallback = onRuntimeChangeCallback;
	}
	
	@Override
	public String getName() {
		return ctx.getModuleName();
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		onRuntimeChangeCallback.accept(ctx.getRuntimeDescription(false));
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void initModule() {
		ctx.setModule(this);
	}
	
	@Override
	public void initData(List<BarField> historyBars) {
		if(historyBars.isEmpty()) {
			log.debug("[{}] 初始化数据为空", ctx.getModuleName());
			return;
		}
		
		log.debug("[{}] 合约{} 初始化数据 {} {} -> {} {}", ctx.getModuleName(), historyBars.get(0).getUnifiedSymbol(),
				historyBars.get(0).getActionDay(), historyBars.get(0).getActionTime(), 
				historyBars.get(historyBars.size() - 1).getActionDay(), historyBars.get(historyBars.size() - 1).getActionTime());
		boolean flag = enabled;
		enabled = false;
		for(BarField bar : historyBars) {
			ctx.onBar(bar);
		}
		enabled = flag;
	}

	@Override
	public void onEvent(NorthstarEvent event) {
		Object data = event.getData();
		if(data instanceof TickField tick && mktGatewayIdSet.contains(tick.getGatewayId())) {
			ctx.onTick(tick);
		} else if (data instanceof BarField bar && mktGatewayIdSet.contains(bar.getGatewayId())) {
			ctx.onBar(bar);
		} else if (data instanceof OrderField order) {
			ctx.onOrder(order);
		} else if (data instanceof TradeField trade) {
			ctx.onTrade(trade);
		}
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription() {
		return ctx.getRuntimeDescription(true);
	}

}
