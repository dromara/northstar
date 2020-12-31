package tech.xuanwu.northstar.utils;


import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MongoDBUtils {

	private static Gson gson = new Gson();
	
	public static <T> Document beanToDocument(T t) {
		try {
			String json = gson.toJson(t);
			
			return Document.parse(json);
		}catch(Exception e) {
			log.warn("{}", t);
			throw e;
		}
	}
	
	public static <T> T documentToBean(Document doc, Class<T> clz) {
		String realJson = doc.toJson(JsonWriterSettings.builder().build());
		return JSON.toJavaObject((JSON)JSON.parse(realJson), clz);
	}
}
