package common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.ThreadLocalRandom;

import org.assertj.core.data.Offset;
import org.junit.Test;

import tech.xuanwu.northstar.strategy.common.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

public abstract class CommonParamTest {
	
	protected DynamicParamsAware target;

	@Test
	public void shouldSuccessWhenParamsInit() throws Exception {
		DynamicParams params = target.getDynamicParams();
		String clzName = target.getClass().getName();
		String paramClzName = clzName + "$InitParams";
		Class<?> paramType = Class.forName(paramClzName);
		setField(paramType, params);
		target.initWithParams(params);
		for(Field paramField : paramType.getFields()) {
			Field targetField = target.getClass().getDeclaredField(paramField.getName());
			boolean flag1 = paramField.canAccess(params);
			boolean flag2 = targetField.canAccess(target);
			paramField.setAccessible(true);
			targetField.setAccessible(true);
			
			if(paramField.getType() == double.class || paramField.getType() == float.class) {				
				assertThat((double)paramField.get(params)).isCloseTo((double)targetField.get(target), Offset.offset(1e-4));
			}else {
				assertThat(paramField.get(params)).isEqualTo(targetField.get(target));
			}
			
			paramField.setAccessible(flag1);
			targetField.setAccessible(flag2);
			
		}
	}
	
	private static void setField(Class<?> paramType, Object obj) throws Exception {
		for(Field f : paramType.getDeclaredFields()) {
			boolean flag = f.canAccess(obj);
			f.setAccessible(true);
			if(f.getType() == int.class) {
				f.setInt(obj, ThreadLocalRandom.current().nextInt());
			} else if (f.getType() == long.class) {
				f.setLong(obj, ThreadLocalRandom.current().nextLong());
			} else if (f.getType() == float.class) {
				f.setFloat(obj, ThreadLocalRandom.current().nextFloat());
			} else if (f.getType() == double.class) {
				f.setDouble(obj, ThreadLocalRandom.current().nextDouble());
			} else if (f.getType() == short.class) {
				f.setShort(obj, (short)ThreadLocalRandom.current().nextInt());
			} else if (f.getType() == String.class) {
				f.set(obj, "this is mock content");
			}
			
			f.setAccessible(flag);
		}
	}
	
}
