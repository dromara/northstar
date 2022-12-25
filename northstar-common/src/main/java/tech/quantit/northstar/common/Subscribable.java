package tech.quantit.northstar.common;

/**
 * 可订阅接口
 * @author KevinHuangwl
 *
 */
public interface Subscribable {

	/**
	 * 订阅
	 * @return
	 */
	boolean subscribe();
	
	/**
	 * 取消订阅
	 * @return
	 */
	boolean unsubscribe();
}
