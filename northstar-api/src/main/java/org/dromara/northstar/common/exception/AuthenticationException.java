package org.dromara.northstar.common.exception;

/**
 * 认证异常
 * @author KevinHuangwl
 *
 */
public class AuthenticationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3503902860043562239L;

	private static final String MSG = "认证异常：";

	public AuthenticationException(String message) {
		super(MSG + message);
	}

}
