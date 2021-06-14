package tech.xuanwu.northstar.strategy.cta;

import tech.xuanwu.northstar.strategy.common.AbstractModuleFactory;
import tech.xuanwu.northstar.strategy.common.ModuleAccount;
import tech.xuanwu.northstar.strategy.common.ModuleOrder;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.cta.module.CtaModuleAccount;
import tech.xuanwu.northstar.strategy.cta.module.CtaModuleOrder;
import tech.xuanwu.northstar.strategy.cta.module.CtaModulePosition;
import tech.xuanwu.northstar.strategy.cta.module.CtaModuleTrade;

public class CtaModuleFactory extends AbstractModuleFactory{

	@Override
	public ModuleAccount newModuleAccount(double share) {
		return new CtaModuleAccount(share);
	}

	@Override
	public ModulePosition newModulePosition() {
		return new CtaModulePosition();
	}

	@Override
	public ModulePosition loadModulePosition(ModuleStatus status) {
		return null;
	}

	@Override
	public ModuleOrder newModuleOrder() {
		return new CtaModuleOrder();
	}

	@Override
	public ModuleTrade newModuleTrade() {
		return new CtaModuleTrade();
	}

}
