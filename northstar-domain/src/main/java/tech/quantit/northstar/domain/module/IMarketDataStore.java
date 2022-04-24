package tech.quantit.northstar.domain.module;

import java.util.List;

import tech.quantit.northstar.common.TickDataAware;
import tech.quantit.northstar.strategy.api.BarDataAware;
import tech.quantit.northstar.strategy.api.ContextAware;
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
