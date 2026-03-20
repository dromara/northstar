package org.dromara.northstar.common;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.model.Identifier;

public abstract class ObjectManager<T> {
	
	protected ConcurrentMap<Identifier, T> objectMap = new ConcurrentHashMap<>();

	public void add(Identifier id, T t) {
		objectMap.put(id, t);
	}
	
	public void remove(Identifier id) {
		objectMap.remove(id);
	}
	
	public T get(Identifier id) {
		return objectMap.get(id);
	}
	
	public boolean contains(Identifier id) {
		return objectMap.containsKey(id);
	}
	
	public List<T> findAll(){
		return objectMap.values().stream().toList();
	}
}
