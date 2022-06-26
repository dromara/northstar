package tech.quantit.northstar.gateway.playback;

import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.gateway.playback.ticker.TickerGnerator;
import xyz.redtorch.pb.CoreField;

import java.time.LocalDate;
import java.util.*;

/**
 * 准备市场数据
 *
 * @author changsong
 */
public class MarketDataLocal {

    private Map<String, PriorityQueue<CoreField.TickField>> tickData = new HashMap<String, PriorityQueue<CoreField.TickField>>();

    private final IMarketDataRepository marketDataRepository;

    public MarketDataLocal(IMarketDataRepository marketDataRepository, IContractRepository contractRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * 放入处理表弟
     *
     * @param unifiedSymbol 账户ID
     */
    public void put(String unifiedSymbol) {
        PriorityQueue<CoreField.TickField> tickQ = new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );
        tickData.put(unifiedSymbol, tickQ);
    }

    /**
     * 去除处理
     *
     * @param unifiedSymbol 标的
     */
    public void remove(String unifiedSymbol) {
        tickData.remove(unifiedSymbol);
    }


    /**
     * 按照一定的周期，从数据库中读取数据，并将数据放入队列中
     *
     * @param gateWayId 网关ID
     * @param startDate 加载开始日期
     * @param endDate 加载结束日期
     * @param parallelLevel 并行数据加载
     */
    public void loadData(String gateWayId, LocalDate startDate, LocalDate endDate,  int parallelLevel) {
        Set<String> symbols = tickData.keySet();
        for(String symbol : symbols){
            PriorityQueue<CoreField.TickField> tickQ = tickData.get(symbol);
            if(null == tickQ){
                tickQ= new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );
            }

            // 取得合约的日ticket数据
            List<CoreField.BarField> data = marketDataRepository.loadBars(gateWayId, symbol, startDate,
                    endDate);
            List<CoreField.TickField> tickFieldList = TickerGnerator.recordsToTicks(1000, data);
            for(CoreField.TickField field: tickFieldList){
                tickQ.offer(field);
            }
            tickData.put(symbol, tickQ);
        }
    }

    public Map<String, PriorityQueue<CoreField.TickField>> getTickData() {
        return tickData;
    }

}
