package tech.xuanwu.northstar.strategy.annotation.impl;

import java.lang.reflect.Field;
import java.net.MalformedURLException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.caucho.hessian.client.HessianProxyFactory;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.annotation.NorthstarService;
import tech.xuanwu.northstar.strategy.config.common.HessianServiceConfig;

@Slf4j
@Component
public class HessianProxyBeanPostProcessor implements BeanPostProcessor{
	
	@Value("${northstar.server.endpoint}")
	private String northstarServerEndpoint;
	
	private HessianProxyFactory hessianProxyFactory;
	
	public HessianProxyBeanPostProcessor() {
		hessianProxyFactory = new HessianProxyFactory();
		hessianProxyFactory.setConnectTimeout(1000L);
		hessianProxyFactory.setOverloadEnabled(true);
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Field[] fields = bean.getClass().getDeclaredFields();
		if(bean instanceof HessianServiceConfig) {
			log.info("{}", beanName);
		}
		for(Field f : fields) {
			if(f.isAnnotationPresent(NorthstarService.class)) {
				boolean accFlag = f.canAccess(bean);
				f.setAccessible(true);
				//注入Hessian代理
				String serviceName = f.getType().getSimpleName();
				String serviceUrl = northstarServerEndpoint + "/" + serviceName;
				try {
					f.set(bean, hessianProxyFactory.create(f.getType(), serviceUrl));
				} catch (IllegalArgumentException | IllegalAccessException | MalformedURLException e) {
					log.error("", e);
					throw new RuntimeException(e);
				}
				f.setAccessible(accFlag);
			}
		}
		
		return bean;
	}

}
