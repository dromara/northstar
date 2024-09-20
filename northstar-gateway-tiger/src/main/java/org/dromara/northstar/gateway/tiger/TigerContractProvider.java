package org.dromara.northstar.gateway.tiger;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.item.ContractItem;
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
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.tiger.util.CollUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TigerContractProvider {

    @Autowired
    private TigerGatewaySettings settings;

    private TigerDataServiceManager dataMgr;

    private IMarketCenter mktCenter;

    private Map<String, String> symbolNameMap = new HashMap<>();

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
        ClientConfig clientConfig = getTigerHttpClient(settings);
        TigerHttpClient client = TigerHttpClient.getInstance().clientConfig(clientConfig);
        doLoadContracts(Market.CN, client, true);
        doLoadContracts(Market.HK, client, true);
        doLoadContracts(Market.US, client, true);
    }

    static ClientConfig getTigerHttpClient(TigerGatewaySettings settings) {
        ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
        clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
        return clientConfig;
    }

    private List<ContractItem> doLoadContracts(Market market, TigerHttpClient client, boolean addInstrument) {
        List<ContractItem> allContracts = new ArrayList<>();
        QuoteSymbolNameResponse response = client.execute(QuoteSymbolNameRequest.newRequest(market));
        if (!response.isSuccess()) {
            log.warn("TIGER 加载 [{}] 市场合约失败", market);
            return allContracts;
        }

        Map<String, String> map = response.getSymbolNameItems().stream()
                .collect(Collectors.toMap(SymbolNameItem::getSymbol, SymbolNameItem::getName));
        symbolNameMap.putAll(map);

        // 缓存文件路径
        String cacheFilePath = "northstar-gateway-tiger/contracts_cache_" + market + ".json";


        // 先尝试从缓存文件中读取数据
        File cacheFile = new File(cacheFilePath);
        if (cacheFile.exists()) {
            try (Reader reader = new FileReader(cacheFile)) {
                Gson gson = new Gson();
                Type contractListType = new TypeToken<List<ContractItem>>() {
                }.getType();
                allContracts = gson.fromJson(reader, contractListType);
                log.info("从缓存加载了 [{}] 的合约，共 {} 个", market, allContracts.size());
            } catch (IOException e) {
                log.warn("读取缓存文件失败: {}", cacheFilePath, e);
            }
        }

        // 如果缓存为空，才从网络加载
        if (allContracts.isEmpty()) {
            List<String> symbols = response.getSymbolNameItems().stream()
                    .filter(x -> !x.getSymbol().contains(".SH"))
                    .map(SymbolNameItem::getSymbol)
                    .collect(Collectors.toList());

            List<List<String>> split = CollUtil.split(symbols, 50);
            AtomicInteger count = new AtomicInteger();

            // 创建一个RateLimiter实例，每秒允许1次请求（每分钟60次）
            RateLimiter rateLimiter = RateLimiter.create(1.0); // 每秒1次
            for (List<String> strings : split) {
                // 在进行每次请求前，调用rateLimiter.acquire()进行限流控制
                rateLimiter.acquire();
                ContractsRequest contractsRequest = ContractsRequest.newRequest(new ContractsModel(strings));
                ContractsResponse contractsResponse = client.execute(contractsRequest);

                // 将请求到的合约加入全局合约列表
                allContracts.addAll(contractsResponse.getItems());

                count.addAndGet(contractsResponse.getItems().size());
            }

            log.info("从网络加载TIGER网关 [{}] 的合约{}个", market, count.get());

            // 将收集到的合约保存到缓存文件
            try (Writer writer = new FileWriter(cacheFilePath)) {
                Gson gson = new Gson();
                gson.toJson(allContracts, writer);
                log.info("合约保存到缓存文件: {}", cacheFilePath);
            } catch (IOException e) {
                log.warn("保存合约到缓存文件失败: {}", cacheFilePath, e);
            }
        }
        if (addInstrument){
            // 注册合约
            allContracts.forEach(item -> {
                TigerContract contract = new TigerContract(item, dataMgr);
                mktCenter.addInstrument(contract);
            });
        }
        // 最后可以输出或使用 `allContracts` 数据
        log.info("合约加载完毕，市场 [{}] 的合约总数：{}", market, allContracts.size());
        return allContracts;
    }

    public List<ContractDefinition> get() {
        ClientConfig clientConfig = getTigerHttpClient(settings);
        TigerHttpClient client = TigerHttpClient.getInstance().clientConfig(clientConfig);

        // 获取不同市场的合约数据
        List<ContractItem> contractsCN = doLoadContracts(Market.CN, client, false);
        List<ContractItem> contractsHK = doLoadContracts(Market.HK, client, false);
        List<ContractItem> contractsUS = doLoadContracts(Market.US, client, false);

        // 合并所有市场的合约数据
        List<ContractItem> allContracts = new ArrayList<>();
        allContracts.addAll(contractsCN);
        allContracts.addAll(contractsHK);
        allContracts.addAll(contractsUS);


        // 转换 ContractItem 为 ContractDefinition 并返回
        List<ContractDefinition> contractDefs = allContracts.stream().map(item -> {
            item.setName(symbolNameMap.get(item.getSymbol()));
            TigerContract contract = new TigerContract(item, dataMgr);
            return ContractDefinition.builder()
                    .name(contract.name())
                    .exchange(contract.exchange())
                    .productClass(contract.productClass())
                    .symbolPattern(Pattern.compile(contract.name() + ".*"))
                    .commissionRate(3 / 10000D)
                    .dataSource(contract.dataSource())
                    .tradeTimeDef(TradeTimeDefinition.builder().timeSlots(List.of(allDay)).build())
                    .build();
        }).collect(Collectors.toList());
        return contractDefs;
    }
}
