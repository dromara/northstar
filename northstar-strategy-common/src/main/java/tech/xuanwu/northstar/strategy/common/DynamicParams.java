package tech.xuanwu.northstar.strategy.common;

import java.lang.reflect.Field;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class DynamicParams<T> {

	private JSONObject source;
	
	public abstract T resolveFromSource();
	
	public void metaToSource() {
		Field[] fields = this.getClass().getDeclaredFields();
		JSONObject json = new JSONObject();
		for(Field f : fields) {
			if(f.isAnnotationPresent(Label.class)) {
				String fieldName = f.getName();
				String label = f.getAnnotation(Label.class).value();
				int order = f.getAnnotation(Label.class).order();
				boolean isNum = f.getType().isAssignableFrom(Number.class) 
						|| f.getType().equals(int.class)
						|| f.getType().equals(long.class)
						|| f.getType().equals(double.class)
						|| f.getType().equals(float.class);
				String type = isNum ? "Number" : "String";
				JSONObject nestedObj = new JSONObject();
				nestedObj.put("type", type);
				nestedObj.put("label", label);
				nestedObj.put("field", fieldName);
				nestedObj.put("order", order);
				json.put(fieldName, nestedObj);
			}
		}
		
		source = json;
	}
}
