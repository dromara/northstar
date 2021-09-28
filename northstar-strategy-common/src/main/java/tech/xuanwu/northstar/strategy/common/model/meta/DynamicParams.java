package tech.xuanwu.northstar.strategy.common.model.meta;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import tech.xuanwu.northstar.strategy.common.annotation.Setting;

@Getter
@Setter
public abstract class DynamicParams {

	public DynamicParams resolveFromSource(Map<String, ComponentField> fieldMap) throws Exception {
		if(fieldMap == null) {
			throw new IllegalStateException("属性描述为空");
		}
		
		for(Entry<String, ComponentField> e : fieldMap.entrySet()) {
			Field f = this.getClass().getDeclaredField(e.getKey());
			ComponentField cf = e.getValue();
			boolean flag = f.canAccess(this);
			f.setAccessible(true);
			if("Number".equals(cf.getType())) {
				if(f.getType() == int.class) {
					f.setInt(this,cf.getValue() == null ? 0 : cf.getValue() instanceof String ? Integer.parseInt((String) cf.getValue()) : (int)cf.getValue());
				} else if (f.getType() == long.class) {
					f.setLong(this, cf.getValue() == null ? 0 : cf.getValue() instanceof String ? Long.parseLong((String) cf.getValue()) : (long)cf.getValue());
				} else if (f.getType() == float.class) {
					f.setFloat(this, cf.getValue() == null ? 0 : cf.getValue() instanceof String ? Float.parseFloat((String) cf.getValue()) : (float)cf.getValue());
				} else if (f.getType() == double.class) {
					f.setDouble(this, cf.getValue() == null ? 0 : cf.getValue() instanceof String ? Double.parseDouble((String) cf.getValue()) : (double)cf.getValue());
				} else if (f.getType() == short.class) {
					f.setShort(this, cf.getValue() == null ? 0 : cf.getValue() instanceof String ? Short.parseShort((String) cf.getValue()) : (short)cf.getValue());
				}
			} else if(f.getType() == boolean.class) {
				f.set(this, Boolean.parseBoolean((String) cf.getValue()));
			} else {
				f.set(this, cf.getValue());
			}
			
			f.setAccessible(flag);
		}
		
		return this;
	}
	
	public Map<String, ComponentField> getMetaInfo() {
		Field[] fs = this.getClass().getDeclaredFields();
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(Field f : fs) {
			if(f.isAnnotationPresent(Setting.class)) {
				Setting anno = f.getAnnotation(Setting.class);
				String fieldName = f.getName();
				String label = anno.value();
				int order = anno.order();
				String unit = anno.unit();
				String[] options = anno.options();
				boolean isNum = f.getType().isAssignableFrom(Number.class) 
						|| f.getType().equals(int.class)
						|| f.getType().equals(long.class)
						|| f.getType().equals(double.class)
						|| f.getType().equals(float.class);
				String type = options.length > 0 ? "Options" : isNum ? "Number" : "String";
				fieldMap.put(fieldName, new ComponentField(label,fieldName, order, type, null, unit, options));
			}
		}
		return fieldMap;
	}
}
