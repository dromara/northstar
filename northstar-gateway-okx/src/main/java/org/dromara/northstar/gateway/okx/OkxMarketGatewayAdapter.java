package org.dromara.northstar.gateway.okx;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.text.StrPool;
import io.github.ztnozdormu.common.utils.WebSocketCallback;
import io.github.ztnozdormu.okx.impl.OKXWebsocketClientImpl;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum;
import xyz.redtorch.pb.CoreField;
import xyz.redtorch.pb.CoreField.ContractField;

@Slf4j
public class OkxMarketGatewayAdapter implements MarketGateway {

    private FastEventEngine feEngine;

    private GatewayDescription gd;

    private OKXWebsocketClientImpl client;

    private OKXSpi spi;

    private final Map<String, Integer> subscribeConnectionIds = new HashMap<>();

    private ConnectionState connState = ConnectionState.DISCONNECTED;

    public OkxMarketGatewayAdapter(GatewayDescription gd, FastEventEngine feEngine, IContractManager contractMgr) {
        this.gd = gd;
        this.feEngine = feEngine;
        this.spi = new OKXSpi(feEngine, contractMgr);
        this.client = new OKXWebsocketClientImpl();
    }

    @Override
    public void connect() {
        spi.lastActive = System.currentTimeMillis(); // 默认连接
        connState = ConnectionState.CONNECTED;
        feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId());
    }

    @Override
    public void disconnect() {
        client.closeAllConnections();
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
    public boolean subscribe(ContractField contract) {
        String symbolFull = contract.getFullName() + StrPool.DASHED + CoreEnum.ProductClassEnum.SWAP.name();
        if (contract.getProductClass() == CoreEnum.ProductClassEnum.SWAP) {
            // 交易订阅
            int connectId = client.symbolTicker(symbolFull, spi);
            subscribeConnectionIds.put(symbolFull, connectId);
            // 分钟K线订阅
            int connectKId = client.klineStream(symbolFull, "MIN1", spi);
            subscribeConnectionIds.put(symbolFull + "kline", connectKId);
            log.info("OKX网关订阅合约 {} {}", symbolFull, contract.getUnifiedSymbol());
        }
        return true;
    }

    @Override
    public boolean unsubscribe(ContractField contract) {
//        String symbolFull = contract.getFullName()+StrPool.DASHED+CoreEnum.ProductClassEnum.SWAP.name();
        if (contract.getProductClass() == CoreEnum.ProductClassEnum.SWAP) {
            client.closeAllConnections();
//            Integer connectId = subscribeConnectionIds.get(symbolFull);
//            if (connectId!= null) {
//                client.closeConnection(connectId);
//            }
        }
        return true;
    }

    @Override
    public boolean isActive() {
        return spi.isActive();
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.OKX;
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

    class OKXSpi implements WebSocketCallback {

        //  "sodUtc0":"21778.3",  零时开盘价
        //  "sodUtc8":"21565",   UTC+8 时开盘价
        static final String MKT_STAT = "channel";
        static final String ASK_P = "askPx"; // 卖一价
        static final String ASK_V = "askSz"; // 卖一价对应的数量
        static final String BID_P = "bidPx"; // 买一价
        static final String BID_V = "bidSz"; // 买一价对应的数量

        private ConcurrentMap<String, CoreField.TickField.Builder> tickBuilderMap = new ConcurrentHashMap<>();
        private ConcurrentMap<String, CoreField.BarField.Builder> barBuilderMap = new ConcurrentHashMap<>();
        private FastEventEngine feEngine;
        private IContractManager contractMgr;

        private long lastActive;

        public OKXSpi(FastEventEngine feEngine, IContractManager contractMgr) {
            this.feEngine = feEngine;
            this.contractMgr = contractMgr;
        }

        @Override
        public void onReceive(String data) {
            JSONObject jsonObject = JSONObject.parseObject(data);
            if (jsonObject.containsKey("event")) {
                return;
            }
            JSONObject arg = jsonObject.getJSONObject("arg");
            JSONArray arry = jsonObject.getJSONArray("data");
            String symbol = arg.getString("instId");
            if (arg.containsKey("channel") && arg.getString("channel").equals("tickers")) {
                try {
                    JSONObject dj = arry.getJSONObject(0);
                    long timestamp = dj.getLongValue("ts");
                    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                    ZoneId londonTimeZone = ZoneId.of(ZoneOffset.ofHours(0).getId()); // 伦敦时区正好可以让所有时区的交易计算在同一天
                    tickBuilderMap.computeIfAbsent(symbol, key -> CoreField.TickField.newBuilder()
                            .setGatewayId(gd.getGatewayId())
                            .addAllAskPrice(List.of(0D))
                            .addAllAskVolume(List.of(0))
                            .addAllBidPrice(List.of(0D))
                            .addAllBidVolume(List.of(0))
                            .setUnifiedSymbol(contractMgr.getContract(ChannelType.OKX, "BTC").contractField().getUnifiedSymbol()));
                    if (dj.containsKey(ASK_P)) tickBuilderMap.get(symbol).setAskPrice(0, dj.getDoubleValue(ASK_P));
                    if (dj.containsKey(BID_P)) tickBuilderMap.get(symbol).setBidPrice(0, dj.getDoubleValue(BID_P));
                    if (dj.containsKey(ASK_V)) tickBuilderMap.get(symbol).setAskVolume(0, dj.getIntValue(ASK_V));
                    if (dj.containsKey(BID_V)) tickBuilderMap.get(symbol).setBidVolume(0, dj.getIntValue(BID_V));


                    if (arg.containsKey("channel") && arg.getString("channel").equals("tickers")) {
                        // 交易数据更新
                        feEngine.emitEvent(NorthstarEventType.TICK, tickBuilderMap.get(symbol)
                                .setActionDay(ldt.toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                                .setActionTime(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
                                .setTradingDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), londonTimeZone).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                                .setActionTimestamp(timestamp)
                                .setPreClosePrice(jsonObject.getDoubleValue("sodUtc0")) // 前收盘价为0时开盘价
                                .setLastPrice(jsonObject.getDoubleValue("last"))
                                .setHighPrice(jsonObject.getDoubleValue("high24h"))
                                .setLowPrice(jsonObject.getDoubleValue("low24h"))
                                .setOpenPrice(jsonObject.getDoubleValue("open24h"))
                                .setVolume(jsonObject.getLongValue("volume"))
                                .build());
                    } else if (!jsonObject.containsKey(MKT_STAT)) {
                        // 盘口数据更新
                        feEngine.emitEvent(NorthstarEventType.TICK, tickBuilderMap.get(symbol)
                                .setActionDay(ldt.toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                                .setActionTime(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
                                .setTradingDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), londonTimeZone).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                                .setActionTimestamp(timestamp)
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("异常数据：{}", jsonObject);
                    log.error("", e);
                }
            }

            if (arg.containsKey("channel") && arg.getString("channel").equals("candle1m")) {
                try {
                    JSONArray dj = arry.getJSONArray(0);
                    long timestamp = dj.getLongValue(0);
                    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                    ZoneId londonTimeZone = ZoneId.of(ZoneOffset.ofHours(0).getId()); // 伦敦时区正好可以让所有时区的交易计算在同一天
                    barBuilderMap.computeIfAbsent(symbol, key -> CoreField.BarField.newBuilder()
                            .setGatewayId(gd.getGatewayId()));

                    feEngine.emitEvent(NorthstarEventType.BAR, barBuilderMap.get(symbol)
                            .setActionDay(ldt.toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                            .setActionTime(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
                            .setTradingDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), londonTimeZone).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
                            .setActionTimestamp(timestamp)
                            .setPreClosePrice(dj.getDoubleValue(1)) // 前收盘价为0时开盘价
                            .setClosePrice(dj.getDoubleValue(4))
                            .setHighPrice(dj.getDoubleValue(2))
                            .setLowPrice(dj.getDoubleValue(3))
                            .setOpenPrice(dj.getDoubleValue(1))
                            .setVolume(dj.getLongValue(5))
                            .build());

                } catch (Exception e) {
                    log.warn("异常数据：{}", jsonObject);
                    log.error("", e);
                }
            }

        }

        public boolean isActive() {
            return System.currentTimeMillis() - lastActive < 3000;
        }
    }

}
