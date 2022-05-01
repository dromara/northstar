package tech.quantit.northstar.gateway.playback;

import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.common.model.SimAccountDescription;
import tech.quantit.northstar.data.IContractRepository;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import xyz.redtorch.pb.CoreField;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 准备市场数据
 *
 * @author changsong
 */
public class MarketDataSim {

    private Map<String, PriorityQueue<CoreField.TickField>> tickData = new HashMap<String, PriorityQueue<CoreField.TickField>>();

    private final PlaybackDescription playbackDescription;

    private final IMarketDataRepository marketDataRepository;

    private final ISimAccountRepository simAccountRepository;

    private final IGatewayRepository gatewayRepository;

    private final IContractRepository contractRepository;

    public MarketDataSim(PlaybackDescription playbackDescription,
                         IMarketDataRepository marketDataRepository, ISimAccountRepository simAccountRepository,
                         IGatewayRepository gatewayRepository, IContractRepository contractRepository) {
        this.playbackDescription = playbackDescription;
        this.marketDataRepository = marketDataRepository;
        this.simAccountRepository = simAccountRepository;
        this.gatewayRepository = gatewayRepository;
        this.contractRepository = contractRepository;
    }

    /**
     * 按照一定的周期，从数据库中读取数据，并将数据放入队列中
     *
     * @param accountId 账户ID
     * @param curDate 加载日期
     * @param parallelLevel 并行数据加载
     */
    public void loadData(String accountId, LocalDateTime curDate, int parallelLevel) {
        // 从模拟账号取得网关Id
        SimAccountDescription simAccountDescription = simAccountRepository.findById(accountId);
        String gateWayId = simAccountDescription.getGatewayId();

        GatewayDescription gatewayDescription = gatewayRepository.selectById(gateWayId);
        String bindedMktGateWayId = gatewayDescription.getBindedMktGatewayId();

        // 取得合约的日ticket数据
        contractRepository.getByGateWayId(bindedMktGateWayId).forEach(contract -> {
            List<CoreField.TickField> data = marketDataRepository.loadTicksByDateTime(bindedMktGateWayId, contract.getUnifiedSymbol(),
                    curDate);
            PriorityQueue<CoreField.TickField> tickQ = new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );
            for(CoreField.TickField tickData : data) {
                tickQ.offer(tickData);
            }

            tickData.put(contract.getUnifiedSymbol(), tickQ);
       });
    }

    public Map<String, PriorityQueue<CoreField.TickField>> getTickData() {
        return tickData;
    }

}
