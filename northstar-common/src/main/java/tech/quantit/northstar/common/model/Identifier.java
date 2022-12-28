package tech.quantit.northstar.common.model;

import java.util.concurrent.ConcurrentHashMap;

public record Identifier(String value) {

	private static ConcurrentHashMap<String, Identifier> cache = new ConcurrentHashMap<>();
	
	public static Identifier of(String value) {
		cache.computeIfAbsent(value, Identifier::new);
		return cache.get(value);
	}
	
	public static void resetCache() {
		cache.clear();
	}
}
