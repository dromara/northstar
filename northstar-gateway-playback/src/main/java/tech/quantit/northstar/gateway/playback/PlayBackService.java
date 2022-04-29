package tech.quantit.northstar.gateway.playback;

import org.springframework.scheduling.TaskScheduler;
import xyz.redtorch.pb.CoreField;

import java.util.PriorityQueue;

public class PlayBackService {

    private final TaskScheduler taskScheduler;

    private PriorityQueue<CoreField.TickField> tickQ = new PriorityQueue<>(100000, (t1, t2) -> t1.getActionTimestamp() < t2.getActionTimestamp() ? -1 : 1 );

    private PriorityQueue<CoreField.BarField> barQ = new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );

    public PlayBackService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * 按照一定的周期，启动任务
     *
     * @param period
     */
    public void start(int period) {
        taskScheduler.scheduleAtFixedRate(()-> {
            // 装载数据 TODO

        }, period);
    }


    /**
     * 回放数据
     *
     * @param inputTables
     * @param outputTables
     * @param replayRate 每秒钟回放的数据条数 正常、快速。正常速度为每秒 2 个TICK，快速则每秒 20 个TICK；
     * @param parallelLevel 指定多个读取数据的线程数可提升数据读取速度。默认为 1
     */
    public void replay( PriorityQueue<CoreField.TickField> inputTables,  PriorityQueue<CoreField.TickField> outputTables,
                        int replayRate, int parallelLevel) {

    }


}
