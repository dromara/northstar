package org.dromara.northstar.gateway.tiger;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.item.ContractItem;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.model.ContractsModel;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.KlinePoint;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.SymbolNameItem;
import com.tigerbrokers.stock.openapi.client.https.request.contract.ContractsRequest;
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteSymbolNameRequest;
import com.tigerbrokers.stock.openapi.client.https.response.contract.ContractsResponse;
import com.tigerbrokers.stock.openapi.client.https.response.quote.QuoteSymbolNameResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.KType;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.Market;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.tiger.util.CollUtil;
import org.dromara.northstar.gateway.tiger.util.PageTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class TigerDataServiceManager implements IDataSource {
    @Autowired
    private TigerGatewaySettings settings;
    @Autowired
    private ObjectManager<Gateway> gatewayManager;

    private TigerHttpClient getTigerHttpClient() {
        ClientConfig clientConfig = TigerContractProvider.getTigerHttpClient(settings);
        return TigerHttpClient.getInstance().clientConfig(clientConfig);
    }

    @Override
    public List<Bar> getMinutelyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        TigerHttpClient client = getTigerHttpClient();
        LinkedList<Bar> barFieldList = new LinkedList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 将 LocalDate 转换为 LocalDateTime 并格式化为字符串
            String beginTime = startDate.atStartOfDay().format(formatter);
            String endTime = endDate.atStartOfDay().format(formatter);
            List<KlinePoint> klinePoints = PageTokenUtil.getKlineByPage(
                    client,
                    contract.symbol(),
                    KType.min1,
                    beginTime,
                    endTime);

            for (KlinePoint klinePoint : klinePoints) {
                Instant e = Instant.ofEpochMilli(klinePoint.getTime());
                LocalTime actionTime = e.atZone(ZoneId.systemDefault()).toLocalTime();
                LocalDate tradingDay = e.atZone(ZoneId.systemDefault()).toLocalDate();

                barFieldList.addFirst(Bar.builder()
                        .contract(contract)
                        .gatewayId(contract.gatewayId())
                        .tradingDay(tradingDay)
                        .actionDay(tradingDay)
                        .actionTime(actionTime)
                        .actionTimestamp(klinePoint.getTime())
                        .openPrice(klinePoint.getOpen())
                        .highPrice(klinePoint.getHigh())
                        .lowPrice(klinePoint.getLow())
                        .closePrice(klinePoint.getClose())
                        .volume(klinePoint.getVolume())
                        .volumeDelta(klinePoint.getVolume())
                        .turnover(klinePoint.getAmount())
                        .turnoverDelta(klinePoint.getAmount())
                        .channelType(ChannelType.TIGER)
                        .build());
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return barFieldList;
    }

    @Override
    public List<Bar> getQuarterlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<Bar> getHourlyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<Bar> getDailyData(Contract contract, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<LocalDate> getHolidays(ChannelType channelType, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public List<Contract> getAllContracts() {
        log.info("加载[TIGER]回测合约");

        TigerHttpClient client = getTigerHttpClient();
        List<Contract> contractsCN = doLoadContracts(Market.CN, client);
        List<Contract> contractsHK = doLoadContracts(Market.HK, client);
        List<Contract> contractsUS = doLoadContracts(Market.US, client);
        // 创建一个新的列表来存储所有合并后的合约
        List<Contract> allContracts = new ArrayList<>();

        // 合并三个列表
        allContracts.addAll(contractsCN);
        allContracts.addAll(contractsHK);
        allContracts.addAll(contractsUS);

        return allContracts;
    }

    public List<Contract> doLoadContracts(Market market, TigerHttpClient client) {
        LinkedList<Contract> resultList = new LinkedList<>();
        QuoteSymbolNameResponse response = client.execute(QuoteSymbolNameRequest.newRequest(market));
        if (!response.isSuccess()) {
            log.warn("TIGER 加载 [{}] 市场合约失败", market);
            return resultList;
        }

        Map<String, String> symbolNameMap = response.getSymbolNameItems().stream()
                .collect(Collectors.toMap(SymbolNameItem::getSymbol, SymbolNameItem::getName));

        // 缓存文件路径
        String cacheFilePath = "northstar-gateway-tiger/contracts_cache_" + market + ".json";
        List<ContractItem> allContracts = new ArrayList<>();

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

                // 注册合约
                contractsResponse.getItems().forEach(item -> {
                    TigerContract contract = new TigerContract(item, this);
                    resultList.add(contract.contract());
                });

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
        } else {
            // 注册合约
            allContracts.forEach(item -> {
                TigerContract contract = new TigerContract(item, this);
                resultList.add(contract.contract());
            });
        }

        // 最后可以输出或使用 `allContracts` 数据
        log.info("合约加载完毕，市场 [{}] 的合约总数：{}", market, allContracts.size());
        return resultList;
    }
}
