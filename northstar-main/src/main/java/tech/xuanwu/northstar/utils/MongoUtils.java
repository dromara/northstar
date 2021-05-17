package tech.xuanwu.northstar.utils;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

public interface MongoUtils {

	static Gson gson = new Gson();

	static <T> Document beanToDocument(T t) {
		String json = gson.toJson(t);
		return Document.parse(json);
	}

	static <T> T documentToBean(Document doc, Class<T> clz) {
		String realJson = doc.toJson(JsonWriterSettings.builder().build());
		return JSON.toJavaObject((JSON) JSON.parse(realJson), clz);
	}
}
