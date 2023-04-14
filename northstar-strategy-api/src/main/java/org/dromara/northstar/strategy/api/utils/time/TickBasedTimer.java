package org.dromara.northstar.strategy.api.utils.time;

import java.util.Objects;
import java.util.PriorityQueue;
import java.util.TimerTask;

import lombok.AllArgsConstructor;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 基于Tick时间为时间尺度的计时器
 * @author KevinHuangwl
 *
 */
public class TickBasedTimer {

	private long currentTimestamp;
	
	private PriorityQueue<TaskWrapper> taskQ = new PriorityQueue<>();

	public void onTick(TickField tick) {
		currentTimestamp = tick.getActionTimestamp();
		while(!taskQ.isEmpty() && taskQ.peek().dueTimestamp < currentTimestamp) {
			taskQ.poll().task.run();
		}
	}
	
	public void schedule(TimerTask task, long delay) {
		if(currentTimestamp == 0) {
			throw new IllegalStateException("计时器未正确更新时间");
		}
		long dueTimestamp = currentTimestamp + delay;
		taskQ.offer(new TaskWrapper(dueTimestamp, task));
	}
	
	@AllArgsConstructor
	private static class TaskWrapper implements Comparable<TaskWrapper>{
		
		private long dueTimestamp;
		
		private TimerTask task;
		
		@Override
		public int compareTo(TaskWrapper o) {
			return this.dueTimestamp < o.dueTimestamp ? -1 : 1; 
		}

		@Override
		public int hashCode() {
			return Objects.hash(dueTimestamp, task);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TaskWrapper other = (TaskWrapper) obj;
			return dueTimestamp == other.dueTimestamp && Objects.equals(task, other.task);
		}
	}
}
