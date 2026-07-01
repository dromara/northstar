package org.dromara.northstar.app.security;

import java.io.IOException;

import org.dromara.northstar.app.security.LoginIPLockService.LockStatus;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MaliciousRequestFilter implements Filter{
	
	private static final String LOGIN_PATH = "/northstar/user/login";
	
	private MaliciousIPCollector ipColltr;
	
	private LoginIPLockService ipLockService;
	
	public MaliciousRequestFilter(MaliciousIPCollector ipColltr, LoginIPLockService ipLockService) {
		this.ipColltr = ipColltr;
		this.ipLockService = ipLockService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		if(ipColltr.shouldBlock(req)) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "疑似恶意请求被拒绝");
			return;
		}
		
		// 登录接口自身由 UserController 返回精确错误信息，Filter 仅拦截其他被冻结/封禁 IP 的请求
		if(!isLoginPath(req)) {
			LockStatus status = ipLockService.checkStatus(req.getRemoteAddr());
			if(status != LockStatus.NORMAL) {
				String msgKey = status == LockStatus.BANNED ? "error.ip_banned" : "error.ip_frozen";
				long remainingSeconds = ipLockService.getRemainingSeconds(req.getRemoteAddr());
				long displayValue = status == LockStatus.BANNED ? remainingSeconds / 3600 : remainingSeconds / 60;
				String errorMsg = MessageSourceHolder.getMessageSource().getMessage(msgKey, new Object[] { displayValue }, LocaleContextHolder.getLocale());
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMsg);
				return;
			}
		}
		
		chain.doFilter(request, response);
	}
	
	private boolean isLoginPath(HttpServletRequest req) {
		return LOGIN_PATH.equals(req.getRequestURI());
	}

}
