package tech.quantit.northstar.domain.module;

import java.util.List;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSONObject;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 负责行情数据更新
 * @author KevinHuangwl
 *
 */
public interface IMarketDataStore {
	/**
	 * 响应TICK
	 * @param tick
	 */
	void onTick(TickField tick);
	/**
	 * 响应BAR
	 * @param bar
	 */
	void onBar(BarField bar);
	/**
	 * 历史数据初始化
	 * @param bars
	 */
	void initWithBars(List<BarField> bars);
	/**
	 * 模组启停设置
	 * @param enabled
	 */
	void setModuleEnabled(boolean enabled);
	/**
	 * 添加启停状态切换回调
	 * @param listener
	 */
	void addEnabledToggleCallback(Consumer<Boolean> listener);
	/**
	 * 获取数据状态
	 * @return
	 */
	JSONObject getDataState(); 
}
