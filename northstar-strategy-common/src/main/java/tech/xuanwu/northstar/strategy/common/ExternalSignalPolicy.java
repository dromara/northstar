package tech.xuanwu.northstar.strategy.common;

/**
 * 外部信号策略
 * @author KevinHuangwl
 *
 */
public interface ExternalSignalPolicy extends SignalPolicy{

	/**
	 * 接收外部传入的数据来生成信号
	 * @param text
	 */
	void onExtMsg(String text);
}
