package tech.xuanwu.northstar.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import tech.xuanwu.northstar.common.exception.AuthenticationException;
import tech.xuanwu.northstar.utils.JwtUtil;

public class AuthorizationInterceptor implements HandlerInterceptor{

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpServletRequest req = (HttpServletRequest) request;
		String path = req.getServletPath();
		if(path.startsWith("/auth")) {
			return true;
		}
		
		String token = req.getHeader("Authorization");
		if(StringUtils.isNotBlank(token) && JwtUtil.verity(token)) {
			return true;
		}
		
		throw new AuthenticationException("token校验失败");
	}

	
}
