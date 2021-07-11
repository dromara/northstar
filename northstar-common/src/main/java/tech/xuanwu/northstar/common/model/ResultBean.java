package tech.xuanwu.northstar.common.model;

import lombok.Data;
import tech.xuanwu.northstar.common.constant.ReturnCode;

@Data
public class ResultBean<T> {
	/**
	 * 返回码
	 */
	private int status = ReturnCode.ERROR;
	/**
	 * 异常信息
	 */
	private String message;
	/**
	 * 返回值
	 */
	private T data;
	
	public ResultBean(T t) {
		this.status = ReturnCode.SUCCESS;
		this.data = t;
	}
	
	public ResultBean(int returnCode, String message) {
		this.status = returnCode;
		this.message = message;
	}
}
