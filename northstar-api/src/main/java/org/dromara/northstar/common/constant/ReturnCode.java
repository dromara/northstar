package org.dromara.northstar.common.constant;

/**
 * RESTful接口返回码
 * @author KevinHuangwl
 *
 */
public interface ReturnCode {

	/**
	 * 成功
	 */
	int SUCCESS = 200;
	/**
	 * 未知异常
	 */
	int ERROR = 500;
	/**
	 * 认证异常
	 */
	int AUTH_ERR = 555;
	/**
	 * 交易异常
	 */
	int TRADE_EXCEPTION = 540;
	/**
	 * 数量不足异常
	 */
	int INSUFFICIENT_EXCEPTION = 541;
	/**
	 * 元素不存在异常
	 */
	int NO_SUCH_ELEMENT_EXCEPTION = 542;
	/**
	 * 数量不匹配异常
	 */
	int VALUE_MISMATCH_EXCEPTION = 543;
}
