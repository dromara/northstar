package org.dromara.northstar.common.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.dromara.northstar.common.SettingOptionsProvider;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.utils.ReflectionUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public abstract class DynamicParams {

	public DynamicParams resolveFromSource(Collection<ComponentField> fields) throws NoSuchFieldException, IllegalAccessException {
		if(fields == null) {
			throw new IllegalStateException("属性描述为空");
		}
		
		for(ComponentField cf : fields) {
			if(Objects.nonNull(cf.getValue())) {
				ReflectionUtil.setFieldValue(this, cf.getName(), cf.getValue());
			}
		}
		
		return this;
	}
	
	public Collection<ComponentField> getMetaInfo() {
		Field[] fs = this.getClass().getDeclaredFields();
		List<ComponentField> result = new ArrayList<>();
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
				Object value;
				try {
					value = Optional.ofNullable(ReflectionUtil.getFieldValue(this, f.getName())).orElse("").toString();
				} catch (NoSuchFieldException | IllegalAccessException e) {
					log.warn(f.getName() + "字段获取失败", e);
					value = null;
				}
				result.add(new ComponentField(label,fieldName, order, type, value, unit, options, optionsVal, placeholder, required));
			}
		}
		return result;
	}
}
