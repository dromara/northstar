package tech.xuanwu.northstar.strategy.common.model.meta;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import tech.xuanwu.northstar.strategy.common.annotation.Label;

@Getter
@Setter
public abstract class DynamicParams {

	public DynamicParams resolveFromSource(Map<String, ComponentField> fieldMap) throws Exception {
		if(fieldMap == null) {
			throw new IllegalStateException("属性描述为空");
		}
		
		for(Entry<String, ComponentField> e : fieldMap.entrySet()) {
			Field f = this.getClass().getDeclaredField(e.getKey());
			boolean flag = f.canAccess(this);
			f.set(this, e.getValue().getValue());
			f.setAccessible(flag);
		}
		
		return this;
	}
	
	public Map<String, ComponentField> getMetaInfo() {
		Field[] fs = this.getClass().getDeclaredFields();
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(Field f : fs) {
			if(f.isAnnotationPresent(Label.class)) {
				String fieldName = f.getName();
				String label = f.getAnnotation(Label.class).value();
				int order = f.getAnnotation(Label.class).order();
				String unit = f.getAnnotation(Label.class).unit();
				boolean isNum = f.getType().isAssignableFrom(Number.class) 
						|| f.getType().equals(int.class)
						|| f.getType().equals(long.class)
						|| f.getType().equals(double.class)
						|| f.getType().equals(float.class);
				String type = isNum ? "Number" : "String";
				fieldMap.put(fieldName, new ComponentField(label,fieldName, order, type, null, unit));
			}
		}
		return fieldMap;
	}
}
