package tech.quantit.northstar.strategy.api.utils.collection;

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
	public void update(T obj, boolean unsettled) {
		if(unsettled) {
			array[cursor] = obj;
			return;
		}
		array[cursor] = obj;	// 最终回溯步长为1的值
		cursor = getIndex(1);
		array[cursor] = obj;	// 最终回溯步长为0的值
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
