package xyz.redtorch.gateway.ctp.common;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.domain.gateway.Contract;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * CTP网关全局注册中心，负责路由行情数据到合适的BAR生成器
 * @author KevinHuangwl
 *
 */
public class CtpGlobalRegistry {

	private FastEventEngine feEngine;
	
	public CtpGlobalRegistry(FastEventEngine feEngine) {
		this.feEngine = feEngine;
	}
	
	public void register(Contract contract) {
		
	}
	
	public void routeToBarGenerator(TickField tick) {
		
	}
}
