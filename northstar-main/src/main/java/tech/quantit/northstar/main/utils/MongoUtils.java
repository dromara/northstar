package tech.quantit.northstar.main.utils;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

public class MongoUtils {

	private static Gson gson = new Gson();
	
	private MongoUtils() {}

	public static <T> Document beanToDocument(T t) {
		String json = gson.toJson(t);
		return Document.parse(json);
	}

	public static <T> T documentToBean(Document doc, Class<T> clz) {
		String realJson = doc.toJson(JsonWriterSettings.builder().build());
		return JSON.toJavaObject((JSON) JSON.parse(realJson), clz);
	}
}
