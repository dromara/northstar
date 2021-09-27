package tech.xuanwu.northstar.strategy.common.model.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 环形列表
 * @author KevinHuangwl
 *
 * @param <T>
 */
public class RingBuffer<T> {
	
	protected final Object[] buf;
	
	protected int cursor;

	public RingBuffer(int size) {
		buf = new Object[size];
	}
	
	public RingBuffer(List<T> list) {
		buf = list.toArray();
	}
	
	public void offer(T t) {
		buf[cursor++] = t;
		cursor = cursor % buf.length;
	}
	
	@SuppressWarnings("unchecked")
	public List<T> toList() {
		return Stream.of(buf).map(obj -> (T)obj).collect(Collectors.toList());
	}
	
	@SuppressWarnings("unchecked")
	public T[] toArray(T[] a) {
		int size = buf.length;
		if (a.length < size)
            return (T[]) Arrays.copyOf(buf, size, a.getClass());
        System.arraycopy(buf, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
	}
	
}
