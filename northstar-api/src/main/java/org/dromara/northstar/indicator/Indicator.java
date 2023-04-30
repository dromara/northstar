package org.dromara.northstar.indicator;

import java.util.List;

public interface Indicator {

	/**
	 * 获取指标回溯值对象
	 * @param step		回溯步长，取值范围为(-size, 0]。0代表当前值，-1代表回溯上一步，-2代表回溯上两步，如此类推
	 * @return
	 */
	public Num get(int step);
	/**
	 * 获取指标回溯值
	 * @param step		回溯步长，取值范围为(-size, 0]。0代表当前值，-1代表回溯上一步，-2代表回溯上两步，如此类推
	 * @return
	 */
	public double value(int step);
	/**
	 * 指标是否已完成初始化
	 * @return
	 */
	public boolean isReady();
	/**
	 * 获取系列值
	 * @return			返回一个按时间升序的列表数据
	 */
	public List<Num> getData();
	/**
	 * 值更新
	 * 此接口为幂等设计：同一个Num对象，更新多次与更新一次的效果相等。这个设计主要是使用户编写策略时可以避免考虑同一个指标可能存在的多次更新问题。
	 * 具体的例子可以参考布林带指标
	 * @param num
	 */
	public void update(Num num);
	/**
	 * 获取该指标的依赖指标
	 * 此接口主要用于暴露指标的依赖关系，从而可以让模组上下文通过该接口递归获取指标的依赖树关系，为指标更新与图表可视化提供入口
	 * @return
	 */
	public List<Indicator> dependencies();
	/**
	 * 获取指标配置信息
	 * @return
	 */
	public Configuration getConfiguration();
}
