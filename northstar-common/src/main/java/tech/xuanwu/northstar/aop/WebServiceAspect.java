package tech.xuanwu.northstar.aop;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.utils.JwtUtil;

/**
 * webservice服务切面
 * @author kevinhuangwl
 *
 */
@Slf4j
@Aspect
//@Component
@ConditionalOnProperty(prefix="spring.profiles", name="active", havingValue="prod")
public class WebServiceAspect implements InitializingBean{

	@Pointcut("@annotation(io.swagger.annotations.ApiOperation)")
	public void allWebServiceMethod() {}
	
	@Before("allWebServiceMethod()")
	public void incomingRequestLogger(JoinPoint joinPoint) {
		//获取RequestAttributes  
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();  
        //从获取RequestAttributes中获取HttpServletRequest的信息  
        HttpServletRequest req = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        
        String token = req.getHeader("token");
        
        if(token == null || !JwtUtil.verity(token)) {
        	throw new IllegalAccessError("未获得接口访问权限");
        }
        
		Signature sign = joinPoint.getSignature();
		String name = sign.getName();
		
		log.info("【日志审计】 监控接口 [ {} ] 被IP：{} 调用", name, req.getRemoteAddr());
		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("启用接口权限监控");
	}
}
