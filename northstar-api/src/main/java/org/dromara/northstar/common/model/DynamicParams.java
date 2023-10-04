package org.dromara.northstar.common.model;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.dromara.northstar.common.SettingOptionsProvider;
import org.dromara.northstar.common.constant.FieldType;

import cn.hutool.core.bean.BeanUtil;
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
			if(Objects.nonNull(cf.getValue())) {
				BeanUtil.setFieldValue(this, f.getName(), cf.getValue());
			}
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
				Object value = BeanUtil.getFieldValue(this, f.getName());
				fieldMap.put(fieldName, new ComponentField(label,fieldName, order, type, value, unit, options, optionsVal, placeholder, required));
			}
		}
		return fieldMap;
	}
}
