package org.dromara.northstar.app.restful;

import java.io.IOException;

import org.dromara.northstar.app.security.MaliciousIPCollector;
import org.dromara.northstar.common.exception.LoginException;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.exception.QuantitDataSourceException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.exception.UnauthorizedException;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 统一处理器
 * @author KevinHuangwl
 *
 */
@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice {
	
	@Autowired
	private MaliciousIPCollector ipColltr;
	
	@ExceptionHandler(IOException.class)
	public void handleIOException(Exception e) {
		// 处理sse断开后的异常信息，不用返回前端控制台
		log.warn("{}", e.getLocalizedMessage());
	}
	
	@ExceptionHandler(LoginException.class)
	public ResponseEntity<ResultBean<Void>> handleUserException(Exception e) {
		if(log.isDebugEnabled()) {
			log.error("", e);
		} else {			
			log.error(e.getLocalizedMessage());
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ResultBean<>(HttpStatus.UNAUTHORIZED.value(), e.getLocalizedMessage()));
	}
	
	@ExceptionHandler({UnauthorizedException.class, QuantitDataSourceException.class})
	public ResponseEntity<ResultBean<Void>> handleUnauthException(Exception e) {
		if(log.isDebugEnabled()) {
			log.error("", e);
		} else {			
			log.error(e.getLocalizedMessage());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultBean<>(HttpStatus.BAD_REQUEST.value(), e.getLocalizedMessage()));
	}
	
	@ExceptionHandler({OperationException.class, TradeException.class, IllegalArgumentException.class})
	public ResponseEntity<ResultBean<Void>> handleOprException(Exception e) {
		if(log.isDebugEnabled()) {
			log.error("", e);
		} else {			
			log.error(e.getLocalizedMessage());
		}
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResultBean<>(HttpStatus.NOT_ACCEPTABLE.value(), e.getLocalizedMessage()));
	}
	
	/** 状态冲突（如 RL fork 双重校验失败）：透传具体原因，避免被兜底成 500 通用文案 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ResultBean<Void>> handleStateException(Exception e) {
		if(log.isDebugEnabled()) {
			log.error("", e);
		} else {			
			log.error(e.getLocalizedMessage());
		}
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ResultBean<>(HttpStatus.CONFLICT.value(), e.getLocalizedMessage()));
	}
	
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Void> handleNoResourceException(Exception e, HttpServletRequest req){
		ipColltr.log(req);
		return ResponseEntity.notFound().build();
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResultBean<Void>> handleException(Exception e) {
		log.error("", e);
		return ResponseEntity
				.internalServerError()
				.body(new ResultBean<>(500, MessageSourceHolder.getMessageSource().getMessage("error.internal_error", null, LocaleContextHolder.getLocale())));
	}
}
