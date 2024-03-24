package org.dromara.northstar.gateway.playback.model;

import java.util.HashSet;
import java.util.Set;

import org.dromara.northstar.common.Timed;

/**
 * 一份时间帧相同的数据
 * @auth KevinHuangwl
 */
public class DataFrame<T extends Timed> implements Timed{

	private final Set<T> buf;
	
	private final long timestamp;
	
	private T sample;
	
	public DataFrame(int bufSize, long timestamp) {
		this.buf = HashSet.newHashSet(bufSize);
		this.timestamp = timestamp;
	}
	
	public void add(T item) {
		if(item.getTimestamp() == timestamp) {
			buf.add(item);
			sample = item;
		}
	}
	
	public Set<T> items(){
		return buf;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	public T getSample() {
		return sample;
	}
	
}
