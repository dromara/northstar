package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.strategy.api.model.TimeSeriesData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 行情数据接收器
 * @author KevinHuangwl
 *
 */
public interface MarketDataReceiver extends TickDataAware, BarDataAware, ContractBindedAware{

	/**
	 * 使用TICK数据初始化
	 * @param ticks
	 */
	void initByTick(Iterable<TickField> ticks);
	
	/**
	 * 使用BAR数据初始化
	 * @param bars
	 */
	void initByBar(Iterable<BarField> bars);
	
	/**
	 * 透视数据（用于给外部提供数据透视入口）
	 * 注意出于传输性能考虑，数据量不能大于10000，否则会抛出异常
	 * @return
	 */
	TimeSeriesData inspection();
}
