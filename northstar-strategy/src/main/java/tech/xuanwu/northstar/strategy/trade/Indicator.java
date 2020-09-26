package tech.xuanwu.northstar.strategy.trade;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 指标
 * @author kevinhuangwl
 *
 */
public interface Indicator {

	/**
	 * 初始化/重置
	 */
	void init();
	
	/**
	 * 更新数据
	 * @param v
	 */
	void update(BarField bar);
	
	/**
	 * 获取最近的指标计算值
	 * @return
	 */
	double getValue();
	
	/**
	 * 获取N个周期前的指标计算值
	 * @param ref
	 * @return
	 */
	double getValue(int ref);
	
	/**
	 * 当前指标最大的回溯长度
	 * @return
	 */
	int getMaxRef();
}
