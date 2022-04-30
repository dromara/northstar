package tech.quantit.northstar.gateway.playback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.domain.strategy.SandboxModuleManager;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import xyz.redtorch.pb.CoreField;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PlayBackService {

    private SimMarket simMarket;

    private SandboxModuleManager moduleMgr;

    private final TaskScheduler taskScheduler;

    private MarketDataSim marketDataSim;

    public PlayBackService(TaskScheduler taskScheduler, SimMarket simMarket, SandboxModuleManager moduleMgr) {
        this.taskScheduler = taskScheduler;
        this.simMarket = simMarket;
        this.moduleMgr = moduleMgr;
    }

    /**
     * 回放数据
     *
     * @param replayRate 每秒钟回放的数据条数 正常、快速。正常速度为每秒 2 个TICK，快速则每秒 20 个TICK；
     */
    public void replay( String unifiedSymbol, int replayRate) {
        PlaybackDescription playbackDescription = new PlaybackDescription();

        taskScheduler.scheduleAtFixedRate(()-> {
            PriorityQueue<CoreField.TickField> tickQ = (PriorityQueue<CoreField.TickField>) tickData.get(unifiedSymbol);

            while(!tickQ.isEmpty()) {
                CoreField.TickField bar = tickQ.poll();
                log.info("开始回放数据：{} {} {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime());
                while(!tickQ.isEmpty() && tickQ.peek().getActionTimestamp() < bar.getActionTimestamp() + 60000) {
                    CoreField.TickField tick = tickQ.poll();
                    moduleMgr.onEvent(new NorthstarEvent(NorthstarEventType.TICK, tick));
                    simMarket.onTick(tick);
                }
                moduleMgr.onEvent(new NorthstarEvent(NorthstarEventType.BAR, bar));
            }
        }, (long)1000/replayRate);
    }

}
