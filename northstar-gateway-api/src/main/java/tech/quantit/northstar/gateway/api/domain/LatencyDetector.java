package tech.quantit.northstar.gateway.api.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 延时检测器
 * @author KevinHuangwl
 *
 */
@Deprecated
@Slf4j
public class LatencyDetector {

	private final List<Checkpoint> checkpoints = new ArrayList<>();
	
	public LatencyDetector(int samplingInterval, int numOfCheckpoints) {
		for(int i=0; i<numOfCheckpoints; i++) {
			checkpoints.add(new Checkpoint(samplingInterval, i));
		}
	}
	
	public Checkpoint getCheckpoint(int index) {
		if(index < 0 || index > checkpoints.size()) {
			throw new IllegalArgumentException("检查点获取参数异常");
		}
		return checkpoints.get(index);
	}
	
	public class Checkpoint {
		
		private final int samplingInterval;
		
		private final int indexOfCheckpoint;
		
		private long lastStatTimestamp;
		
		private ConcurrentMap<String, Long> sumOfTickLatencyMap = new ConcurrentHashMap<>();
		
		private ConcurrentMap<String, Integer> sumOfTickCountMap = new ConcurrentHashMap<>();
		
		public Checkpoint(int samplingInterval, int index) {
			this.samplingInterval = samplingInterval;
			this.indexOfCheckpoint = index;
		}
		
		public void sampling(TickField tick) {
			if(tick.getUnifiedSymbol().contains(Constants.INDEX_SUFFIX)) {
				// 忽略对指数TICK的统计
				return;
			}
			long latency = System.currentTimeMillis() - tick.getActionTimestamp();
			sumOfTickLatencyMap.putIfAbsent(tick.getUnifiedSymbol(), 0L);
			sumOfTickLatencyMap.computeIfPresent(tick.getUnifiedSymbol(), (key, sumOfLatency) -> sumOfLatency + latency);
			sumOfTickCountMap.putIfAbsent(tick.getUnifiedSymbol(), 0);
			sumOfTickCountMap.computeIfPresent(tick.getUnifiedSymbol(), (key, sumOfTickCount) -> sumOfTickCount + 1);
			doStatistic();
		}
		
		private void doStatistic() {
			if(System.currentTimeMillis() - lastStatTimestamp > samplingInterval * 1000) {
				long sumLatency = sumOfTickLatencyMap.values().stream().mapToLong(sum -> sum).sum();
				long count = sumOfTickCountMap.values().stream().mapToInt(cnt -> cnt).sum();
				Entry<String, Long> maxLatencyEntry = sumOfTickLatencyMap.entrySet()
						.stream()
						.max((a,b) -> a.getValue() < b.getValue() ? -1 : 1)
						.get();
				
				if(count == 0) {
					return;
				}
				log.debug("行情延时探测器，检查点[{}] 统计汇总：统计合约数 [{}]个，统计间隔 [{}]秒，平均延时 [{}]毫秒",
						indexOfCheckpoint, sumOfTickCountMap.size(), samplingInterval, sumLatency / count);
				log.debug("行情延时探测器，检查点[{}] 统计明细：最大延时合约 [{}]，平均延时 [{}]毫秒",
						indexOfCheckpoint, maxLatencyEntry.getKey(), maxLatencyEntry.getValue() / sumOfTickCountMap.get(maxLatencyEntry.getKey()));
				
				lastStatTimestamp = System.currentTimeMillis();
				sumOfTickCountMap.clear();
				sumOfTickLatencyMap.clear();
			}
		}
	}
}
