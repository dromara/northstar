package org.dromara.northstar.app.auth;

import java.util.UUID;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.utils.InternalLoopCallUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthCheckerInterceptor implements HandlerInterceptor{
	
	@Autowired
	private HttpSession session;
	
	private static final String SSE_PATH = "/northstar/sse/stream";
	
	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse response, Object handler)
			throws Exception {
		if(req.getServletPath().equals(SSE_PATH)) {
			return true;
		}
		String sessCSRF = (String) session.getAttribute(Constants.SESS_CSRF);
		String reqCSRF = req.getHeader(Constants.CSRF_HEADER);
		if(StringUtils.hasText(sessCSRF) && StringUtils.hasText(reqCSRF) 
				&& (sessCSRF.equals(reqCSRF) || InternalLoopCallUtils.consume(UUID.fromString(reqCSRF)))) {
			return true;
		}
		
		response.sendError(HttpStatus.FORBIDDEN.value());
		return false;
	}
}
