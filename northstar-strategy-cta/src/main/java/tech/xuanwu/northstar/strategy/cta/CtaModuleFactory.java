package tech.xuanwu.northstar.strategy.cta;

import java.util.ArrayList;
import java.util.List;

import tech.xuanwu.northstar.strategy.common.AbstractModuleFactory;
import tech.xuanwu.northstar.strategy.common.ModulePosition;
import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.cta.module.CtaModulePosition;
import tech.xuanwu.northstar.strategy.cta.module.CtaModuleTrade;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModuleFactory extends AbstractModuleFactory{

	@Override
	public ModulePosition newModulePosition() {
		return new CtaModulePosition();
	}

	@Override
	public ModulePosition loadModulePosition(ModuleStatus status) {
		List<byte[]> tradeData = status.getLastOpenTrade();
		List<TradeField> tradeList = new ArrayList<>();
		for(byte[] data : tradeData) {
			try {
				tradeList.add(TradeField.parseFrom(data));
			} catch(Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return new CtaModulePosition(tradeList);
	}

	@Override
	public ModuleTrade newModuleTrade() {
		return new CtaModuleTrade();
	}

	@Override
	public ModuleTrade loadModuleTrade(List<TradeField> originTradeList) {
		return new CtaModuleTrade(originTradeList);
	}
	
	

}
