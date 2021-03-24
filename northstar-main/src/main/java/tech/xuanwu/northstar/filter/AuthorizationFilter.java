package tech.xuanwu.northstar.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import tech.xuanwu.northstar.common.exception.AuthenticationException;
import tech.xuanwu.northstar.utils.JwtUtil;

/**
 * 简单的权限控制
 * @author KevinHuangwl
 *
 */
@WebFilter(urlPatterns = "/*")
@Order(Integer.MIN_VALUE)
@Component
public class AuthorizationFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		String path = req.getServletPath();
		if(path.startsWith("/auth")) {
			chain.doFilter(request, response);
			return;
		}
		
		String token = req.getHeader("Authorization");
		if(StringUtils.isNotBlank(token) && JwtUtil.verity(token)) {
			chain.doFilter(request, response);
			return;
		}
		
		throw new AuthenticationException("token校验失败");
	}

}
