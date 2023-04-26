package org.dromara.northstar.indicator;

import java.util.List;

public interface Indicator {

	/**
	 * 获取指标回溯值
	 * @param step		回溯步长，取值范围为(-size, 0]。0代表当前值，-1代表回溯上一步，-2代表回溯上两步，如此类推
	 * @return
	 */
	public Num value(int step);
	/**
	 * 指标是否已完成初始化
	 * @return
	 */
	public boolean isReady();
	/**
	 * 获取系列值
	 * @return
	 */
	public List<Num> getData();
	/**
	 * 值更新
	 * @param num
	 */
	public void update(Num num);
	/**
	 * 获取该指标的依赖指标
	 * @return
	 */
	public List<Indicator> dependencies();
	/**
	 * 获取指标配置信息
	 * @return
	 */
	public Configuration getConfiguration();
}
