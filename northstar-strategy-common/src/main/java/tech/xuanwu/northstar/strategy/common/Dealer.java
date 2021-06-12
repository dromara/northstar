package tech.xuanwu.northstar.strategy.common;

import java.util.List;

import tech.xuanwu.northstar.gateway.api.Gateway;
import xyz.redtorch.pb.CoreField.TickField;

public interface Dealer {

	void onTick(TickField tick, List<RiskControlRule> riskRules, Gateway gateway);
	
	void tryDeal(TickField tick, List<RiskControlRule> riskRules, Gateway gateway);
}
