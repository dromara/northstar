package tech.quantit.northstar.strategy.api.utils.collection;

import java.util.Optional;

public class RingArray<T> {

	private Object[] array;
	
	private int size;
	
	private int cursor;
	
	public RingArray(int size) {
		this.array = new Object[size];
		this.size = size;
	}
	
	public T get() {
		return get(0);
	}
	
	@SuppressWarnings("unchecked")
	public T get(int index) {
		return (T) array[getIndex(index)];
	}
	
	/**
	 * 更新值
	 * @param obj	返回旧值
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Optional<T> update(T obj) {
		cursor = getIndex(1);
		T oldVal = (T) array[cursor]; 
		array[cursor] = obj;
		return Optional.ofNullable(oldVal);
	}
	
	private int getIndex(int i) {
		return (cursor + size + i) % size;
	}
	
	public Object[] toArray() {
		Object[] result = new Object[size];
		for(int i=0; i<size; i++) {
			result[i] = get(-(i+1));
		}
		return result;
	}
	
	public int size() {
		return size;
	}
}
