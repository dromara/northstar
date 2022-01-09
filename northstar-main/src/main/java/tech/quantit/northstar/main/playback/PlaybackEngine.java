package tech.quantit.northstar.main.playback;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.domain.strategy.SandboxModuleManager;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.playback.PlaybackTask.DataType;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 回测引擎负责把原始的历史行情数据按时间规则重放来模拟真实行情
 * @author KevinHuangwl
 *
 */
@Slf4j
public class PlaybackEngine {
	
	private SimMarket simMarket;
	
	private SandboxModuleManager moduleMgr;
	
	public PlaybackEngine(SimMarket simMarket, SandboxModuleManager moduleMgr) {
		this.simMarket = simMarket;
		this.moduleMgr = moduleMgr;
	}

	@SuppressWarnings("unchecked")
	public void play(PlaybackTask task) throws InterruptedException {
		log.info("################# 开始回测 #################");
		
		while(task.hasMoreDayToPlay()) {
			Map<DataType, PriorityQueue<?>> batchDataMap;
			try {
				batchDataMap = task.nextBatchDataOfDay();
			} catch (InvalidProtocolBufferException e) {
				throw new IllegalStateException("历史行情数据加载异常", e);
			}
			PriorityQueue<TickField> tickQ = (PriorityQueue<TickField>) batchDataMap.get(DataType.TICK);
			PriorityQueue<BarField> barQ = (PriorityQueue<BarField>) batchDataMap.get(DataType.BAR);
			
			while(!barQ.isEmpty()) {
				BarField bar = barQ.poll();
				log.info("开始回放数据：{} {} {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime());
				while(!tickQ.isEmpty() && tickQ.peek().getActionTimestamp() < bar.getActionTimestamp() + 60000) {					
					TickField tick = tickQ.poll();
					moduleMgr.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick));
					Thread.sleep(5);
					simMarket.onTick(tick);
				}
				moduleMgr.onEvent(new NorthstarEvent(NorthstarEventType.BAR, bar));
				
			}
		}
		log.info("################# 回测结束 #################");
	}
	
	
	
}
