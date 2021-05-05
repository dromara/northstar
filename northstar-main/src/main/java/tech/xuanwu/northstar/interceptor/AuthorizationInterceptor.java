package tech.xuanwu.northstar.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import tech.xuanwu.northstar.common.constant.Constants;
import tech.xuanwu.northstar.common.exception.AuthenticationException;

public class AuthorizationInterceptor implements HandlerInterceptor{

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpServletRequest req = (HttpServletRequest) request;
		String path = req.getServletPath();
		if(path.startsWith("/auth/login")) {
			return true;
		}
		
//		String token = req.getHeader("Authorization");
//		if(StringUtils.isNotBlank(token) && JwtUtil.verity(token)) {
//			return true;
//		}
		Object user = req.getSession().getAttribute(Constants.KEY_USER);
		if(user != null) {
			return true;
		}
		throw new AuthenticationException("token校验失败");
	}

	
}
