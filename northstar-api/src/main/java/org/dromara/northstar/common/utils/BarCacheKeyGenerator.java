package org.dromara.northstar.common.utils;

import java.lang.reflect.Method;
import java.time.LocalDate;

import org.dromara.northstar.common.model.core.Contract;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component
public class BarCacheKeyGenerator implements KeyGenerator{

	@Override
	public Object generate(Object target, Method method, Object... params) {
		Contract contract = (Contract) params[0];
		LocalDate startDate = (LocalDate) params[1];
		LocalDate endDate = (LocalDate) params[2];
		return String.format("%s:%s:%s:%s", method.getName(), contract.unifiedSymbol(), startDate, endDate);
	}

}
