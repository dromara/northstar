package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 模组抽象接口
 * @author KevinHuangwl
 *
 */
public interface IModule {

	/**
	 * 模组名称
	 * @return
	 */
	String getName();
	
	/**
	 * 设置运行状态
	 * @param enabled
	 */
	void setEnabled(boolean enabled);
	
	/**
	 * 获取运行状态
	 * @return
	 */
	boolean isEnabled();
	
	/**
	 * 模组初始化
	 */
	void initModule();
	
	/**
	 * 模组数据初始化
	 * @param historyBars
	 */
	void initData(List<BarField> historyBars);
	
	/**
	 * 监听事件
	 * @param event
	 */
	void onEvent(NorthstarEvent event);
	
	/**
	 * 获取模组状态描述
	 * @return
	 */
	ModuleRuntimeDescription getRuntimeDescription();
}
