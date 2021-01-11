package tech.xuanwu.northstar.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.springframework.beans.BeanUtils;

public class BeanUtil {

	public static <T> void copyProperties(T source, T target, CopyStrategy s) {
		switch(s) {
		case FULL_COPY:
			BeanUtils.copyProperties(source, target);
			break;
		case ONLY_NOT_NULL:
			copyOnlyNotNullProperties(source, target);
			break;
		default:
			throw new IllegalStateException();
		}
	}
	
	private static <T> void copyOnlyNotNullProperties(T source, T target) {
		Field[] fields = source.getClass().getDeclaredFields();
		try {
			for(Field f : fields) {
				if(Modifier.isStatic(f.getModifiers())) {
					continue;
				}
				boolean flag = f.canAccess(source);
				f.setAccessible(true);
				Object val = f.get(source);
				if(val != null) {
					f.set(target, val);
				}
				f.setAccessible(flag);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static enum CopyStrategy {
		FULL_COPY, ONLY_NOT_NULL
	}
}
