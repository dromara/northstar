package tech.quantit.northstar.strategy.api.utils;

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
	
	public void update(T obj) {
		cursor = getIndex(1);
		array[cursor] = obj;
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
}
