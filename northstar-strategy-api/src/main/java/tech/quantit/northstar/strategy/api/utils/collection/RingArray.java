package tech.quantit.northstar.strategy.api.utils.collection;

import java.util.Objects;
import java.util.stream.Stream;

public class RingArray<T> {

	private Object[] array;
	
	private int cursor;
	
	private boolean lastFlag = true;
	
	public RingArray(int size) {
		this.array = new Object[size];
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
	public void update(T obj, boolean unsettled) {
		if(unsettled) {
			if(lastFlag != unsettled) {
				cursor = getIndex(1);
			}
			array[cursor] = obj;
			lastFlag = unsettled;
			return;
		}
		int incr = lastFlag == unsettled ? 1 : 0;
		cursor = getIndex(incr);
		array[cursor] = obj;	
		lastFlag = unsettled;
	}
	
	private int getIndex(int incr) {
		return (cursor + array.length + incr) % array.length;
	}
	
	public Object[] toArray() {
		Object[] result = new Object[array.length];
		for(int i=0; i<array.length; i++) {
			result[i] = get(-(i+1));
		}
		return result;
	}
	
	public int size() {
		return (int) Stream.of(array).filter(Objects::nonNull).count();
	}
}
