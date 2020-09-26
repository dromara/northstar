package tech.xuanwu.northstar.exception;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.ResultBean;
import tech.xuanwu.northstar.common.ReturnCode;

@Slf4j
@ControllerAdvice(annotations = RestController.class)
public class CommonExceptionHandler {

	@ExceptionHandler
	@ResponseBody
	public ResultBean<Object> handleUnknowException(Throwable t){
		String errMsg = StringUtils.hasText(t.getMessage()) ? t.getMessage() : "服务端遇到未知异常";
		log.error(errMsg, t);
		return new ResultBean<>(ReturnCode.ERROR, errMsg);
	}
}
