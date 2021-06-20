package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.event.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import xyz.redtorch.pb.CoreField.TickField;

public interface RiskController extends EventDrivenComponent{
	
	void setModuleAgent(ModuleAgent agent);

	void onTick(TickField tick);
	
	void approveOrder();
	
	void rejectOrder();
	
	void retryOrder();
}
