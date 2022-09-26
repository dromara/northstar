package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.common.TickDataAware;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 嵌入式策略接口
 * 用于模组策略内部定义子策略
 * @author KevinHuangwl
 *
 */
public interface IEmbededRule extends BarDataAware, TickDataAware {

	/**
	 * 响应TICK数据
	 */
	@Override
	default void onTick(TickField tick) {}

	/**
	 * 响应BAR数据
	 */
	@Override
	default void onBar(BarField bar) {}

	/**
	 * 策略当前的持仓
	 * 0为无持仓
	 * 1为多头仓
	 * -1为空头仓
	 * @return
	 */
	int holdingStatus();
}
