package tech.xuanwu.northstar.strategy.trade;

/**
 * 交易逻辑接口
 * @author kevinhuangwl
 *
 */
public interface TradeLogic {
	
	/**
	 * 初始化/重置
	 */
	void init();

	/**
	 * 获取交易逻辑信号
	 * @return
	 */
	Signal getCurrentSignal();
	
	/**
	 * 交易信号
	 * @author kevinhuangwl
	 *
	 */
	enum Signal{
		/**
		 * 无信号
		 */
		NONE,
		
		/**
		 * 多头信号
		 */
		LONG,
		
		/**
		 * 空头信号
		 */
		SHORT;
	}
}
