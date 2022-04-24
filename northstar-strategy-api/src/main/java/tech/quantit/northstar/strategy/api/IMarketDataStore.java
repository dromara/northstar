package tech.quantit.northstar.strategy.api;

import java.util.List;

import tech.quantit.northstar.common.TickDataAware;
import xyz.redtorch.pb.CoreField.BarField;

/**
 * 负责行情数据更新
 * @author KevinHuangwl
 *
 */
public interface IMarketDataStore extends TickDataAware, BarDataAware, ContextAware{
	/**
	 * 历史数据初始化
	 * @param bars
	 */
	void initWithBars(List<BarField> bars);
	/**
	 * 模组启停设置
	 * @param enabled
	 */
	void onModuleEnabledChange(boolean enabled);
}
