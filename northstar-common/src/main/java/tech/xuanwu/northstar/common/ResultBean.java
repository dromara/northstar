package tech.xuanwu.northstar.common;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一返回实体
 * @author kevinhuangwl
 *
 */
@Getter
@Setter
public class ResultBean<T> implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3108634633102878613L;
	//返回码
	private int rtnCode;
	//返回信息
	private String msg;
	//返回对象
	private T data;
	
	public ResultBean(T t) {
		rtnCode = ReturnCode.SUCCESS;
		data = t;
	}
	
	public ResultBean(int rtnCode, String msg) {
		this.rtnCode = rtnCode;
		this.msg = msg;
	}
}
