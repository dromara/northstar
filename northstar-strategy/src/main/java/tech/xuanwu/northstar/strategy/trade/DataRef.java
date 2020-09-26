package tech.xuanwu.northstar.strategy.trade;

import java.util.List;

/**
 * 行情数据引用接口
 * @author kevinhuangwl
 *
 * @param <T>
 */
public interface DataRef<T> {
	
	/**
	 * 加载数据
	 * @param numOfRef
	 */
	void load(int numOfRef);

	/**
	 * 更新数据
	 * @param data
	 */
	void updateData(T data);
	
	/**
	 * 获取数据
	 * @return
	 */
	List<T> getDataRef();
	
	/**
	 * 向数据源加入指标，监听数据变化
	 * @param indicator
	 */
	void addIndicator(Indicator indicator);
	
	
	enum PriceType{
		/**
		 * 最高价
		 */
		HIGH,
		/**
		 * 最低价
		 */
		LOW,
		/**
		 * 开盘价
		 */
		OPEN,
		/**
		 * 收盘价
		 */
		CLOSE;
	}
}
