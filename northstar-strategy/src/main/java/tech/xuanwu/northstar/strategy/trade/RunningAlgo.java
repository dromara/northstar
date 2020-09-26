package tech.xuanwu.northstar.strategy.trade;

/**
 * 滚动算法接口
 * 主要考虑到数据更新的特点，采用空间换时间的思路来优化计算效率
 * @author kevinhuangwl
 *
 */
public interface RunningAlgo {
	
	/**
	 * 初始化算法数据
	 * @param data				数据
	 * @param nextUpdateCursor	下一个数据的更新下标位置
	 */
	void init(double[] data, int nextUpdateCursor);

	/**
	 * 更新数据
	 * @param val
	 */
	void update(double val);
	
	/**
	 * 获取计算结果
	 * @return
	 */
	double getResult();
}
