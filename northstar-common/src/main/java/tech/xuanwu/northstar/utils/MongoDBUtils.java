package tech.xuanwu.northstar.utils;


import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

public abstract class MongoDBUtils {

	private static Gson gson = new Gson();
	
	public static <T> Document beanToDocument(T t) {
		return Document.parse(gson.toJson(t));
	}
	
	public static <T> T documentToBean(Document doc, Class<T> clz) {
		String realJson = doc.toJson(JsonWriterSettings.builder().build());
		return JSON.toJavaObject((JSON)JSON.parse(realJson), clz);
	}
}
