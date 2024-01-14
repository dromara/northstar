package org.dromara.northstar.web.restful.common;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ReturnCode;
import org.dromara.northstar.common.exception.AuthenticationException;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.exception.ValueMismatchException;
import org.dromara.northstar.common.model.ResultBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * 统一处理器
 * @author KevinHuangwl
 *
 */
@Slf4j
@RestControllerAdvice
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class CommonControllerAdvice {

	@ExceptionHandler
	public ResultBean<?> handleException(Exception e, HttpServletResponse reponse) {
		String msg = StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "遇到未知异常";
		log.error(msg, e);
		return new ResultBean<>(ReturnCode.ERROR, msg);
	}
	
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Void> handleException(Exception e) {
		return ResponseEntity.notFound().build();
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
