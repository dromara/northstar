package org.dromara.northstar.indicator.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
		if(size < 1) {
			throw new IllegalArgumentException("非法参数：" + size);
		}
		this.array = (T[]) new Object[size];
	}
	
	public synchronized T get() {
		return get(0);
	}
	
	/**
	 * get(0) 是访问队头（最新的）数据
	 * get(-1) 是访问队头第二个数据
	 * get(1) 是访问队尾数据
	 * @param index
	 * @return
	 */
	public synchronized T get(int index) {
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
	
	public synchronized Object[] toArray() {
		Object[] result = new Object[array.length];
		for(int i=0; i<array.length; i++) {
			result[i] = get(-i);
		}
		return result;
	}
	
	/**
	 * 返回的列表按自然顺序排列
	 * @return
	 */
	public synchronized List<T> toList(){
		List<T> result = new ArrayList<>(array.length);
		for(int i=0; i<array.length; i++) {
			if(get(-i) != null) {				
				result.add(get(-i));
			}
		}
		return result.reversed();
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
	
	/**
	 * 克隆出一个独立的副本，副本与源对象的状态一致（包括游标位置与更新标记），
	 * 之后对两者的修改互不影响，效果类似 List.copyOf
	 * @return
	 */
	@Override
	public synchronized RingArray<T> clone() {
		RingArray<T> copy = new RingArray<>(array.length);
		copy.array = Arrays.copyOf(array, array.length);
		copy.cursor = cursor;
		copy.lastFlag = lastFlag;
		return copy;
	}
}
