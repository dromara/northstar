package org.dromara.northstar.ai;

import java.util.Arrays;

/**
 * 强化学习马可夫过程中的环境状态描述
 * @auth KevinHuangwl
 */

public record RLState(double...values) {

	public int dimension() {
		return values.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RLState other = (RLState) obj;
		return Arrays.equals(values, other.values);
	}

	@Override
	public String toString() {
		return "RLState [values=" + Arrays.toString(values) + "]";
	}
	
}
