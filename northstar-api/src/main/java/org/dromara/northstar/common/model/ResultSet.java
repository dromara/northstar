package org.dromara.northstar.common.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSONObject;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResultSet {

	private String[] fields;
	
	private String[][] items;
	
	private int fieldIndex(String fieldName) {
		Assert.hasText(fieldName, "字段名不能为空");
		for(int i=0; i<fields.length; i++) {
			if(fieldName.equals(fields[i]))
				return i;
		}
		throw new IllegalArgumentException("找不到字段：" + fieldName);
	}
	
	public String getFieldValue(String fieldName, int row) {
		int idx = fieldIndex(fieldName);
		return items[row][idx];
	}
	
	public void setFieldValue(String fieldName, int row, String value) {
		int idx = fieldIndex(fieldName);
		items[row][idx] = value;
	}
	
	public void setFieldName(String fromName, String toName) {
		fields[fieldIndex(fromName)] = toName;
	}
	
	public int size() {
		return items.length;
	}
	
	public List<JSONObject> toJSONList(){
		List<JSONObject> results = new ArrayList<>(size());
		for(int i=0; i<size(); i++) {
			JSONObject json = new JSONObject();
			for(String field : getFields()) {
				json.put(field, getFieldValue(field, i));
			}
			results.add(json);
		}
		
		return results;
	}
}
