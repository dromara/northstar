package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public interface RiskController {
	
	/**
	 * 风控测试
	 * @param tick
	 * @return		风控码
	 */
	short onTick(TickField tick, StrategyModule module);
	
	/**
	 * 下单风控测试
	 * @param orderReq
	 * @return		是否拒绝
	 */
	boolean testReject(TickField tick, StrategyModule module, SubmitOrderReqField orderReq);
	
	
}
