package tech.quantit.northstar.main.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;

@Slf4j
public class AuthorizationInterceptor implements HandlerInterceptor{

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpServletRequest req = (HttpServletRequest) request;
		String path = req.getServletPath();
		if(path.startsWith("/northstar/auth/login") || !path.startsWith("/northstar")) {
			return true;
		}
		
		Object user = req.getSession().getAttribute(Constants.KEY_USER);
		if(user != null) {
			return true;
		}
		
		String msg = "token校验失败";
		response.sendError(HttpStatus.UNAUTHORIZED.value(), msg);
		log.warn(msg);
		return false;
	}

	
}
