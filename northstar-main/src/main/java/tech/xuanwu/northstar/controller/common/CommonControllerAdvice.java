package tech.xuanwu.northstar.controller.common;

import java.io.File;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.exception.AuthenticationException;
import tech.xuanwu.northstar.common.exception.InsufficientException;
import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.exception.ValueMismatchException;

/**
 * 统一处理器
 * @author KevinHuangwl
 *
 */
@Slf4j
@RestControllerAdvice
public class CommonControllerAdvice implements ResponseBodyAdvice<Object>{

	@ExceptionHandler
	public ResultBean<?> handleException(Exception e) {
		String msg = StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "遇到未知异常";
		log.error(msg, e);
		int code = e instanceof InsufficientException ? ReturnCode.INSUFFICIENT_EXCEPTION : 
			e instanceof ValueMismatchException ? ReturnCode.VALUE_MISMATCH_EXCEPTION :
				e instanceof NoSuchElementException ? ReturnCode.NO_SUCH_ELEMENT_EXCEPTION :
					e instanceof TradeException ? ReturnCode.TRADE_EXCEPTION :
							e instanceof AuthenticationException ? ReturnCode.AUTH_ERR : ReturnCode.ERROR;
		return new ResultBean<>(code, msg);
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body == null || body instanceof ResultBean || body instanceof File) {
            return body;
        }

        return new ResultBean<>(body);
	}

}
