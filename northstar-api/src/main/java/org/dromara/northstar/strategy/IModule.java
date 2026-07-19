package org.dromara.northstar.strategy;

import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.kline.KlineGenerator;

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
	 * 获取模组账户
	 * @return
	 */
	IAccount getModuleAccount();
	/**
	 * 获取模组上下文
	 * @return
	 */
	IModuleContext getModuleContext();
	/**
	 * 获取K线合成器
	 * @param contract
	 * @return
	 */
	KlineGenerator getKlineGenerator(Contract contract);
}
