package org.dromara.northstar.gateway.tiger;

import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback;
import com.tigerbrokers.stock.openapi.client.socket.WebSocketClient;
import com.tigerbrokers.stock.openapi.client.socket.data.TradeTick;
import com.tigerbrokers.stock.openapi.client.socket.data.pb.*;
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.util.ApiLogger;
import com.tigerbrokers.stock.openapi.client.util.ProtoMessageUtil;
import io.netty.handler.ssl.SslProvider;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.TickField;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 老虎证券行情网关适配器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TigerMarketGatewayAdapter implements MarketGateway {

    private GatewayDescription gd;

    private FastEventEngine feEngine;

    private WebSocketClient client;

    private TigerSpi spi;

    private ConnectionState connState = ConnectionState.DISCONNECTED;

    public TigerMarketGatewayAdapter(GatewayDescription gd, FastEventEngine feEngine, IContractManager contractMgr) {
        this.gd = gd;
        this.feEngine = feEngine;
        this.spi = new TigerSpi(feEngine, contractMgr);

        TigerGatewaySettings settings = (TigerGatewaySettings) gd.getSettings();
        ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
        clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
        clientConfig.setSslProvider(SslProvider.JDK);
        this.client = WebSocketClient.getInstance().clientConfig(clientConfig).apiComposeCallback(spi);
        ApiLogger.setEnabled(true, "logs/");
    }

    @Override
    public void connect() {
        client.connect();
        connState = ConnectionState.CONNECTED;
        feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId());
    }

    @Override
    public void disconnect() {
        client.disconnect();
        connState = ConnectionState.DISCONNECTED;
    }

    @Override
    public ConnectionState getConnectionState() {
        return connState;
    }

    @Override
    public boolean getAuthErrorFlag() {
        return false;
    }

    @Override
    public boolean subscribe(Contract contract) {
        if (contract.productClass() == ProductClassEnum.EQUITY) {
            client.subscribeQuote(Set.of(contract.symbol()));
            log.info("TIGER网关订阅合约 {} {}", contract.name(), contract.unifiedSymbol());
        }
        // TODO 期货期权暂没实现
        return true;
    }

    @Override
    public boolean unsubscribe(Contract contract) {
        if (contract.productClass() == ProductClassEnum.EQUITY) {
            client.cancelSubscribeQuote(Set.of(contract.symbol()));
        }
        // TODO 期货期权暂没实现
        return true;
    }

    @Override
    public boolean isActive() {
        return spi.isActive();
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.TIGER;
    }

    @Override
    public GatewayDescription gatewayDescription() {
        gd.setConnectionState(getConnectionState());
        return gd;
    }

    @Override
    public String gatewayId() {
        return gd.getGatewayId();
    }

    class TigerSpi implements ApiComposeCallback {

        static final String MKT_STAT = "marketStatus";
        static final String ASK_P = "askPrice";
        static final String ASK_V = "askSize";
        static final String BID_P = "bidPrice";
        static final String BID_V = "bidSize";

        private ConcurrentMap<String, Tick.TickBuilder> tickBuilderMap = new ConcurrentHashMap<>();

        private FastEventEngine feEngine;
        private IContractManager contractMgr;

        private long lastActive;

        public TigerSpi(FastEventEngine feEngine, IContractManager contractMgr) {
            this.feEngine = feEngine;
            this.contractMgr = contractMgr;
        }

        @Override
        public void orderStatusChange(OrderStatusData orderStatusData) {
            // TODO Auto-generated method stub

        }

        @Override
        public void orderTransactionChange(OrderTransactionData orderTransactionData) {

        }

        @Override
        public void positionChange(PositionData positionData) {
            // TODO Auto-generated method stub

        }

        @Override
        public void assetChange(AssetData assetData) {
            // TODO Auto-generated method stub

        }
/**
 * symbol: "300454"
 * type: BASIC
 * timestamp: 1722393313358
 * serverTimestamp: 1722393313348
 * latestPrice: 48.87
 * latestPriceTimestamp: 1722393313358
 * latestTime: "07-31 10:35:12"
 * preClose: 47.55
 * volume: 0
 */

        @Override
        public void quoteChange(QuoteBasicData quoteBasicData) {
            lastActive = System.currentTimeMillis();
            if (log.isTraceEnabled()) {
                log.trace("数据回报：{}", quoteBasicData);
            }
            if (quoteBasicData.getHourTradingTag() != null && !quoteBasicData.getHourTradingTag().isEmpty()) {
                return; // 忽略盘前盘后数据
            }
            try {
                String symbol = quoteBasicData.getSymbol();
                Contract contract = contractMgr.getContract(ChannelType.TIGER, symbol).contract();
                long timestamp = quoteBasicData.getTimestamp();
                LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                Instant e = Instant.ofEpochMilli(timestamp);
                LocalTime actionTime = e.atZone(ZoneId.systemDefault()).toLocalTime();
                LocalDate tradingDay = e.atZone(ZoneId.systemDefault()).toLocalDate();
                ZoneId londonTimeZone = ZoneId.of(ZoneOffset.UTC.getId());
                tickBuilderMap.computeIfAbsent(symbol, key -> Tick.builder()
                        .contract(contract)
                        .gatewayId(gd.getGatewayId())
                        .tradingDay(tradingDay)
                        .actionDay(tradingDay)
                        .actionTime(actionTime)
                        .actionTimestamp(timestamp)
                        .avgPrice(quoteBasicData.getAvgPrice())
                        .highPrice(quoteBasicData.getHigh())
                        .lowPrice(quoteBasicData.getLow())
                        .openPrice(quoteBasicData.getOpen())
                        .lastPrice(quoteBasicData.getLatestPrice())
                        .volume(quoteBasicData.getVolume())
                        .channelType(ChannelType.TIGER)
                        .type(TickType.MARKET_TICK)
                        /*.allAskPrice(List.of(0D))
                        .allAskVolume(List.of(0))
                        .allBidPrice(List.of(0D))
                        .allBidVolume(List.of(0))*/

                );

                if (quoteBasicData.getMarketStatus() != null && quoteBasicData.getMarketStatus().equals("Trading")) {
                    // 交易数据更新
                    feEngine.emitEvent(NorthstarEventType.TICK, tickBuilderMap.get(symbol)
                            .preClosePrice(quoteBasicData.getPreClose())
                            .lastPrice(quoteBasicData.getLatestPrice())
                            .highPrice(quoteBasicData.getHigh())
                            .lowPrice(quoteBasicData.getLow())
                            .openPrice(quoteBasicData.getOpen())
                            .volume(quoteBasicData.getVolume())
                            .build());
                } else if (quoteBasicData.getMarketStatus() == null || quoteBasicData.getMarketStatus().isEmpty()) {
                    // 盘口数据更新
                    feEngine.emitEvent(NorthstarEventType.TICK, tickBuilderMap.get(symbol)
                            .actionTimestamp(timestamp)
                            .build());
                }
            } catch (Exception e) {
                log.warn("异常数据：{}", quoteBasicData);
                log.error("", e);
            }
        }

        /**
         * symbol: "600588"
         * type: BBO
         * timestamp: 1725949796603
         * askPrice: 8.63
         * askSize: 313
         * askTimestamp: 1725949795000
         * bidPrice: 8.61
         * bidSize: 193
         * bidTimestamp: 1725949795000
         */

        /*股票最优买卖价行情回调*/
        @Override
        public void quoteAskBidChange(QuoteBBOData data) {
            String symbol = data.getSymbol();
            if (tickBuilderMap.get(symbol) != null){
                // Example: replace with actual fields
                // Assuming `quoteBasicData` has methods like `getAskPrice`, `getBidPrice`, etc.
                tickBuilderMap.get(symbol).askPrice(Collections.singletonList(data.getAskPrice())); // Example field
                tickBuilderMap.get(symbol).bidPrice(Collections.singletonList(data.getBidPrice())); // Example field
                tickBuilderMap.get(symbol).askVolume(Collections.singletonList(Math.toIntExact(data.getAskSize()))); // Example field
                tickBuilderMap.get(symbol).bidVolume(Collections.singletonList(Math.toIntExact(data.getBidSize()))); // Example field
            }
        }


        @Override
        public void tradeTickChange(TradeTick tradeTick) {
            // TODO Auto-generated method stub

        }

        @Override
        public void fullTickChange(TickData tickData) {

        }

        @Override
        public void optionChange(QuoteBasicData quoteBasicData) {
            lastActive = System.currentTimeMillis();
        }

        @Override
        public void optionAskBidChange(QuoteBBOData quoteBBOData) {

        }

        @Override
        public void futureChange(QuoteBasicData quoteBasicData) {
            lastActive = System.currentTimeMillis();
        }

        @Override
        public void futureAskBidChange(QuoteBBOData quoteBBOData) {

        }

        @Override
        public void depthQuoteChange(QuoteDepthData quoteDepthData) {
            ApiLogger.info("depthQuoteChange:" + ProtoMessageUtil.toJson(quoteDepthData));
        }

        @Override
        public void klineChange(KlineData klineData) {

        }

        @Override
        public void stockTopPush(StockTopData stockTopData) {

        }

        @Override
        public void optionTopPush(OptionTopData optionTopData) {

        }

        @Override
        public void subscribeEnd(int id, String subject, String jsonObject) {
            log.info("成功订阅 [{} {} {}]", id, subject, jsonObject);
        }

        @Override
        public void cancelSubscribeEnd(int id, String subject, String jsonObject) {
            log.info("取消订阅 [{} {} {}]", id, subject, jsonObject);
        }

        @Override
        public void getSubscribedSymbolEnd(SubscribedSymbol subscribedSymbol) {
        }

        @Override
        public void error(String errorMsg) {
            NoticeField notice = NoticeField.newBuilder()
                    .setStatus(CommonStatusEnum.COMS_WARN)
                    .setContent(errorMsg)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            feEngine.emitEvent(NorthstarEventType.NOTICE, notice);
        }

        @Override
        public void error(int id, int errorCode, String errorMsg) {
            log.error("TIGER网关出错 [{} {} {}]", id, errorCode, errorMsg);
        }

        @Override
        public void connectionClosed() {
            log.info("TIGER网关断开");
        }

        @Override
        public void connectionKickout(int errorCode, String errorMsg) {
            NoticeField notice = NoticeField.newBuilder()
                    .setStatus(CommonStatusEnum.COMS_WARN)
                    .setContent(errorMsg)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            feEngine.emitEvent(NorthstarEventType.NOTICE, notice);
        }

        @Override
        public void connectionAck() {
            log.info("TIGER网关应答");
        }

        @Override
        public void connectionAck(int serverSendInterval, int serverReceiveInterval) {
        }

        @Override
        public void hearBeat(String heartBeatContent) {
        }

        @Override
        public void serverHeartBeatTimeOut(String channelIdAsLongText) {
            log.info("TIGER网关服务响应超时：{}", channelIdAsLongText);
        }

        public boolean isActive() {
            return System.currentTimeMillis() - lastActive < 3000;
        }

    }

}
