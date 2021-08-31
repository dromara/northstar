package tech.xuanwu.northstar.main.restful.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.exception.AuthenticationException;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.exception.ValueMismatchException;
import tech.xuanwu.northstar.common.model.ResultBean;

/**
 * 统一处理器
 * @author KevinHuangwl
 *
 */
@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice {

	@ExceptionHandler
	public ResultBean<?> handleException(Exception e) {
		String msg = StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "遇到未知异常";
		log.error(msg, e);
		return new ResultBean<>(ReturnCode.ERROR, msg);
	}
	
	@ExceptionHandler(InsufficientException.class)
	public ResultBean<?> handleInsufficientException(Exception e){
		log.error(e.getMessage(), e);
		return new ResultBean<>(ReturnCode.INSUFFICIENT_EXCEPTION, e.getMessage());
	}
	@ExceptionHandler(ValueMismatchException.class)
	public ResultBean<?> handleValueMismatchException(Exception e){
		log.error(e.getMessage(), e);
		return new ResultBean<>(ReturnCode.VALUE_MISMATCH_EXCEPTION, e.getMessage());
	}
	@ExceptionHandler(NoSuchElementException.class)
	public ResultBean<?> handleNoSuchElementException(Exception e){
		log.error(e.getMessage(), e);
		return new ResultBean<>(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION, e.getMessage());
	}
	@ExceptionHandler(TradeException.class)
	public ResultBean<?> handleTradeException(Exception e){
		log.error(e.getMessage(), e);
		return new ResultBean<>(ReturnCode.TRADE_EXCEPTION, e.getMessage());
	}
	@ExceptionHandler(AuthenticationException.class)
	public ResultBean<?> handleAuthenticationException(Exception e){
		log.error(e.getMessage(), e);
		return new ResultBean<>(ReturnCode.AUTH_ERR, e.getMessage());
	}

}
