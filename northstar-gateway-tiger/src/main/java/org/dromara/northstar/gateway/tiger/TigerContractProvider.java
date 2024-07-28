package org.dromara.northstar.gateway.tiger;

import com.google.common.util.concurrent.RateLimiter;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.model.ContractsModel;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.SymbolNameItem;
import com.tigerbrokers.stock.openapi.client.https.request.contract.ContractsRequest;
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteSymbolNameRequest;
import com.tigerbrokers.stock.openapi.client.https.response.contract.ContractsResponse;
import com.tigerbrokers.stock.openapi.client.https.response.quote.QuoteSymbolNameResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.Market;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.tiger.util.CollUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class TigerContractProvider {

    private TigerGatewaySettings settings;

    private TigerDataServiceManager dataMgr;

    private IMarketCenter mktCenter;

    private TimeSlot allDay = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(0, 0)).end(DateTimeUtils.fromCacheTime(0, 0)).build();

    public TigerContractProvider(TigerGatewaySettings settings, IMarketCenter mktCenter, TigerDataServiceManager dataMgr) {
        this.settings = settings;
        this.mktCenter = mktCenter;
        this.dataMgr = dataMgr;
    }

    /**
     * 加载可用合约
     */
    public void loadContractOptions() {
        ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
        clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
        TigerHttpClient client = TigerHttpClient.getInstance().clientConfig(clientConfig);
        doLoadContracts(Market.CN, client);
        //doLoadContracts(Market.HK, client);
        //doLoadContracts(Market.US, client);
    }

    private void doLoadContracts(Market market, TigerHttpClient client) {
        QuoteSymbolNameResponse response = client.execute(QuoteSymbolNameRequest.newRequest(market));
        if (!response.isSuccess()) {
            log.warn("TIGER 加载 [{}] 市场合约失败", market);
            return;
        }
        Map<String, String> symbolNameMap = response.getSymbolNameItems().stream().collect(Collectors.toMap(SymbolNameItem::getSymbol, SymbolNameItem::getName));
        List<String> symbols = response.getSymbolNameItems().stream().map(SymbolNameItem::getSymbol).collect(Collectors.toList());
        List<List<String>> split = CollUtil.split(symbols, 50);
        AtomicInteger count = new AtomicInteger();
        // 创建一个RateLimiter实例，每秒允许1次请求（每分钟60次）
        RateLimiter rateLimiter = RateLimiter.create(1.0); // 每秒1次
        split.stream().forEach(strings -> {
            // 在进行每次请求前，调用rateLimiter.acquire()进行限流控制
            rateLimiter.acquire();
            ContractsRequest contractsRequest = ContractsRequest.newRequest(new ContractsModel(strings));
            ContractsResponse contractsResponse = client.execute(contractsRequest);
            // 创建合约定义列表
            List<ContractDefinition> contractDefs = contractsResponse.getItems().stream().map(item -> {
                item.setName(symbolNameMap.get(item.getSymbol()));
                TigerContract contract = new TigerContract(item, dataMgr);
                return ContractDefinition.builder()
                        .name(contract.name())
                        .exchange(contract.exchange())
                        .productClass(contract.productClass())
                        .symbolPattern(Pattern.compile(contract.name() + "@[A-Z]+@[A-Z]+@[A-Z]+$"))
                        .commissionRate(3 / 10000D)
                        .dataSource(contract.dataSource())
                        .tradeTimeDef(TradeTimeDefinition.builder().timeSlots(List.of(allDay)).build())
                        .build();
            }).collect(Collectors.toList());

            // 增加合约定义
            mktCenter.addDefinitions(contractDefs);

            // 注册合约
            contractsResponse.getItems().forEach(item -> {
                TigerContract contract = new TigerContract(item, dataMgr);
                mktCenter.addInstrument(contract);
            });
            count.addAndGet(contractsResponse.getItems().size());
        });
        log.info("加载TIGER网关 [{}] 的合约{}个", market, count.get());
    }

}
