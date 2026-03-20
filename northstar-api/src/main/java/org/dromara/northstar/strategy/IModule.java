package org.dromara.northstar.strategy;

import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.core.Contract;

/**
 * 模组抽象接口
 * @author KevinHuangwl
 *
 */
public interface IModule {
	/**
	 * 设置运行状态
	 * @param enabled
	 */
	void setEnabled(boolean enabled);
	/**
	 * 监听事件
	 * @param event
	 */
	void onEvent(NorthstarEvent event);
	/**
	 * 获取合约关联的交易账户
	 * @param contract
	 * @return
	 */
	IAccount getAccount(Contract contract);
	/**
	 * 获取模组上下文
	 * @return
	 */
	IModuleContext getModuleContext();
}
