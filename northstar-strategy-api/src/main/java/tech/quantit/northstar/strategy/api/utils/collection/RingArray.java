package tech.quantit.northstar.strategy.api.utils.collection;

public class RingArray<T> {

	private Object[] array;
	
	private int size;
	
	private int cursor;
	
	private boolean lastFlag = true;
	
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
		return (cursor + size + incr) % size;
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
