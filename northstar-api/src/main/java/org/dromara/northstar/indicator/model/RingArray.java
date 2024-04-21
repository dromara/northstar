package org.dromara.northstar.indicator.model;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 这是一个环型数组，能简单地实现一个滑动窗口的数据结构
 * @auth KevinHuangwl
 */
public class RingArray<T> {

	private T[] array;
	
	private int cursor;
	
	private boolean lastFlag = true;
	
	@SuppressWarnings("unchecked")
	public RingArray(int size) {
		this.array = (T[]) new Object[size];
	}
	
	public T get() {
		return get(0);
	}
	
	/**
	 * get(0) 是访问队头（最新的）数据
	 * get(-1) 是访问队头第二个数据
	 * get(1) 是访问队尾数据
	 * @param index
	 * @return
	 */
	public T get(int index) {
		return array[getIndex(index)];
	}
	
	/**
	 * 更新值
	 * @param obj	返回旧值
	 * @return
	 */
	public synchronized Optional<T> update(T obj, boolean unstable) {
		if(unstable) {
			int index = lastFlag != unstable ? 1 : 0;
			lastFlag = unstable;
			return Optional.ofNullable(update(obj, index));
		}
		int index = lastFlag == unstable ? 1 : 0;
		lastFlag = unstable;
		return Optional.ofNullable(update(obj, index));
	}
	
	private T update(T obj, int index) {
		cursor = getIndex(index);
		T old = array[cursor];
		array[cursor] = obj;
		return old;
	}
	
	private int getIndex(int incr) {
		return (cursor + array.length + incr) % array.length;
	}
	
	public Object[] toArray() {
		Object[] result = new Object[array.length];
		for(int i=0; i<array.length; i++) {
			result[i] = get(-i);
		}
		return result;
	}
	
	/**
	 * 实际大小
	 * @return
	 */
	public int size() {
		return (int) Stream.of(array).filter(Objects::nonNull).count();
	}
	
	/**
	 * 最大长度
	 * @return
	 */
	public int length() {
		return array.length;
	}
}
