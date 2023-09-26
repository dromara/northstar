package org.dromara.northstar.common.model;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dromara.northstar.common.SettingOptionsProvider;
import org.dromara.northstar.common.constant.FieldType;

import com.alibaba.fastjson2.JSON;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
			} else if(f.getType() == boolean.class) {
				f.set(this, Boolean.parseBoolean((String) cf.getValue()));
			} else if(f.getType() == String.class) {
				f.set(this, cf.getValue().toString());
			} else {
				try {
					f.set(this, JSON.parseObject(cf.getValue().toString(), f.getType()));
				} catch (Exception ex) {
					log.warn("{} 不能转换为 {}", cf.getValue(), f.getType());
					log.error("", ex);
				}
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
				String label = anno.label();
				int order = anno.order();
				String unit = anno.unit();
				String[] options = anno.options();
				String[] optionsVal = anno.optionsVal();
				Class<? extends SettingOptionsProvider> pvdClz = anno.optionProvider();
				if(!pvdClz.isInterface()) {
					try {
						SettingOptionsProvider pvd = pvdClz.getDeclaredConstructor().newInstance();
						List<OptionItem> items = pvd.optionVals();
						if(!items.isEmpty()) {
							options = new String[items.size()];
							optionsVal = new String[items.size()];
							for(int i=0; i < items.size(); i++) {
								options[i] = items.get(i).label();
								optionsVal[i] = items.get(i).value();
							}
						}
					} catch (Exception e) {
						log.error("", e);
					}
				}
				FieldType type = anno.type();
				String placeholder = anno.placeholder();
				boolean required = anno.required();
				fieldMap.put(fieldName, new ComponentField(label,fieldName, order, type, null, unit, options, optionsVal, placeholder, required));
			}
		}
		return fieldMap;
	}
}
