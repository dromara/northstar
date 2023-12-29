package org.dromara.northstar.rl.agent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.ai.rl.model.RLExperience;

/**
 * 保存经验集
 * @auth KevinHuangwl
 */
public class ReplayBuffer {

	private List<RLExperience> buffer;
	
	private final int maxSize;
	
	private int cursor;
	
	public ReplayBuffer(int size) {
		buffer = new ArrayList<>(size);
		maxSize = size;
	}
	
	/**
	 * 增加经验样本
	 * @param exp
	 */
	public void add(RLExperience exp) {
		if(cursor < maxSize) {
			buffer.add(exp);
		} else {
			buffer.set(cursor % maxSize, exp);
		}
		cursor++;
	}
	
	/**
	 * 随机采样 
	 * @param batchSize
	 * @return
	 */
	public List<RLExperience> sample(int batchSize){
		Set<RLExperience> results = new HashSet<>();
		int startIndex = ThreadLocalRandom.current().nextInt(maxSize);	// 随机起点
		while(results.size() < batchSize) {
			if(ThreadLocalRandom.current().nextBoolean()) {				// 随机选中		
				results.add(buffer.get(startIndex % maxSize));	
			}
			startIndex++;
		}
		return results.stream().toList();
	}
	
	/**
	 * 经验集大小
	 * @return
	 */
	public int size() {
		return buffer.size();
	}
}
