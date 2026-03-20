package org.dromara.northstar.app.security;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MaliciousRequestFilter implements Filter{
	
	private MaliciousIPCollector ipColltr;
	
	public MaliciousRequestFilter(MaliciousIPCollector ipColltr) {
		this.ipColltr = ipColltr;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if(ipColltr.shouldBlock((HttpServletRequest) request)) {
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "疑似恶意请求被拒绝");
		} else {
			chain.doFilter(request, response);
		}
	}

}
