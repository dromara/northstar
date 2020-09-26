package tech.xuanwu.northstar.strategy.trade;

import xyz.redtorch.pb.CoreField.TickField;

/**
 * 交易策略接口
 * @author kevinhuangwl
 *
 */
public interface Strategy {
	
	/**
	 * 响应tick
	 * @param tick
	 */
	void updateTick(TickField tick);
	
	/**
	 * 启用策略
	 * @return
	 */
	void resume();
	
	/**
	 * 停用策略
	 * @return
	 */
	void suspend();
	
	
	/**
	 * 是否运行
	 * @return
	 */
	boolean isRunning();
}
