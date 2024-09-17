package org.dromara.northstar.gateway.tiger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.trade.item.PrimeAssetItem;
import com.tigerbrokers.stock.openapi.client.https.domain.trade.model.TradeOrderModel;
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest;
import com.tigerbrokers.stock.openapi.client.https.request.trade.PrimeAssetRequest;
import com.tigerbrokers.stock.openapi.client.https.request.trade.TradeOrderRequest;
import com.tigerbrokers.stock.openapi.client.https.response.TigerHttpResponse;
import com.tigerbrokers.stock.openapi.client.https.response.trade.PrimeAssetResponse;
import com.tigerbrokers.stock.openapi.client.https.response.trade.TradeOrderResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.Currency;
import com.tigerbrokers.stock.openapi.client.struct.enums.*;
import com.tigerbrokers.stock.openapi.client.struct.param.OrderParameter;
import com.tigerbrokers.stock.openapi.client.util.builder.AccountParamBuilder;
import com.tigerbrokers.stock.openapi.client.util.builder.TradeParamBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.*;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.TradeGateway;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 老虎证券交易网关适配器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TigerTradeGatewayAdapter implements TradeGateway {

    private final FastEventEngine feEngine;
    private final GatewayDescription gd;
    private final TigerGatewaySettings settings;
    private final IContractManager contractMgr;

    private final ConcurrentMap<Long, String> orderIdMap = new ConcurrentHashMap<>();

    private TigerHttpClient client;
    private OrderTradeQueryProxy proxy;
    private Timer timer;

    private ConnectionState connState = ConnectionState.DISCONNECTED;

    private Executor exec = Executors.newSingleThreadExecutor();

    private Map<String, Position> lastPositions = new HashMap<>();

    public TigerTradeGatewayAdapter(FastEventEngine feEngine, GatewayDescription gd, IContractManager contractMgr) {
        this.feEngine = feEngine;
        this.gd = gd;
        this.settings = (TigerGatewaySettings) gd.getSettings();
        this.contractMgr = contractMgr;
    }

    @Override
    public synchronized void connect() {
        ClientConfig clientConfig = TigerContractProvider.getTigerHttpClient(settings);
        client = TigerHttpClient.getInstance().clientConfig(clientConfig);
        proxy = new OrderTradeQueryProxy(client, contractMgr, gatewayId(), settings.getAccountId());
        List<Order> orders = proxy.getDeltaOrder();
        List<String> symbols = orders.stream().map(Order::contract).map(Contract::symbol).distinct().toList();

        connState = ConnectionState.CONNECTED;
        feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewayId());

        orders.forEach(order -> feEngine.emitEvent(NorthstarEventType.ORDER, order));
        symbols.forEach(symbol -> {
            List<Trade> trades = proxy.getTrades(symbol);
            trades.forEach(trade -> feEngine.emitEvent(NorthstarEventType.TRADE, trade));
        });

        timer = new Timer("TIGER_" + gatewayId(), true);
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    queryAccount();
                    queryPosition();
                    TigerTradeGatewayAdapter.this.doQueryOrderAndTrade();
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }, 3000, 2000);
    }

    private void doQueryOrderAndTrade() {
        proxy.getDeltaOrder().forEach(order -> {
            Long id = Long.valueOf(order.orderId());
            String originOrderId = orderIdMap.get(id);
            feEngine.emitEvent(NorthstarEventType.ORDER, order.toBuilder().originOrderId(Optional.ofNullable(originOrderId).orElse("")).build());
            if (order.tradedVolume() > 0) {
                proxy.getDeltaTrade(Long.valueOf(order.orderId()))
                        .forEach(trade -> feEngine.emitEvent(NorthstarEventType.TRADE, trade.toBuilder().originOrderId(Optional.ofNullable(originOrderId).orElse("")).build()));
            }
        });
    }

    @Override
    public synchronized void disconnect() {
        timer.cancel();
        timer = null;
        client = null;
        proxy = null;
        feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, gatewayId());
        connState = ConnectionState.DISCONNECTED;
    }

    private void queryAccount() {
        log.trace("查询TIGER账户信息");
        PrimeAssetRequest assetRequest = PrimeAssetRequest.buildPrimeAssetRequest(settings.getAccountId());
        PrimeAssetResponse primeAssetResponse = client.execute(assetRequest);
        if (!primeAssetResponse.isSuccess()) {
            log.warn("查询账户返回异常：{}", primeAssetResponse.getMessage());
            return;
        }
        //查询证券相关资产信息
        PrimeAssetItem.Segment segment = primeAssetResponse.getSegment(Category.S);    // 暂不实现期货资产查询
        //查询账号中美元相关资产信息
        if (segment != null) {
            PrimeAssetItem.CurrencyAssets assetByCurrency = segment.getAssetByCurrency(Currency.USD);
            Account build = Account.builder()
                    .accountId(settings.getAccountId())
                    .gatewayId(gatewayId())
                    .available(assetByCurrency.getCashBalance())
                    .margin(segment.getGrossPositionValue())
                    .balance(segment.getNetLiquidation())
                    .positionProfit(segment.getUnrealizedPL())
                    .closeProfit(segment.getRealizedPL())
                    .currency(CurrencyEnum.USD)
                    .build();
            feEngine.emitEvent(NorthstarEventType.ACCOUNT, build);
        }
    }

    private void queryPosition() {
        log.trace("查询TIGER持仓信息");
        TigerHttpRequest request = new TigerHttpRequest(MethodName.POSITIONS);
        String bizContent = AccountParamBuilder.instance()
                .account(settings.getAccountId())
                .secType(SecType.STK)
                .buildJson();

        request.setBizContent(bizContent);
        TigerHttpResponse response = client.execute(request);
        if (!response.isSuccess()) {
            log.warn("查询持仓返回异常：{}", response.getMessage());
            return;
        }
        // 解析具体字段
        JSONArray positions = JSON.parseObject(response.getData()).getJSONArray("items");
        Map<String, Position> positionMap = new HashMap<>();
        for (int i = 0; i < positions.size(); i++) {
            JSONObject json = positions.getJSONObject(i);
            Contract contract = contractMgr.getContract(ChannelType.TIGER, json.getString("symbol")).contract();
            String positionId = String.format("%s@%s@%s", contract.unifiedSymbol(), PositionDirectionEnum.PD_Long, gatewayId());
            double openPrice = (int) (json.getDoubleValue("averageCost") / contract.priceTick()) * contract.priceTick();
            Position pos = Position.builder()
                    .positionId(positionId)
                    .gatewayId(gd.getGatewayId())
                    .positionDirection(PositionDirectionEnum.PD_Long)
                    .position(json.getIntValue("position"))
                    .contract(contract)
                    .positionProfit(json.getDoubleValue("unrealizedPnl"))
                    //.frozen(frozen)
                    //.tdFrozen(tdFrozen)
                    //.ydFrozen(ydFrozen)
                    .openPrice(openPrice)
                    .openPriceDiff(json.getDoubleValue("latestPrice") - openPrice)
                    .build();

            positionMap.put(positionId, pos);
            feEngine.emitEvent(NorthstarEventType.POSITION, pos);
            lastPositions.remove(positionId);
        }

        lastPositions.values().forEach(pf -> {
            feEngine.emitEvent(NorthstarEventType.POSITION, pf.toBuilder().position(0).build());
        });
        lastPositions = positionMap;
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
    public String submitOrder(SubmitOrderReq submitOrderReq) {
        if (connState != ConnectionState.CONNECTED) {
            throw new IllegalStateException("网关未连线");
        }

        SecType secType = switch (submitOrderReq.contract().productClass()) {
            case EQUITY -> SecType.STK;
            case FUTURES -> SecType.FUT;
            default ->
                    throw new IllegalArgumentException("Unexpected value: " + submitOrderReq.contract().productClass());
        };

        OrderType orderType = switch (submitOrderReq.orderPriceType()) {
            case OPT_LimitPrice -> OrderType.LMT;
            case OPT_AnyPrice -> OrderType.MKT;
            default -> throw new IllegalArgumentException("老虎证券仅支持限价与市价两种订单类型");
        };

        ActionType actionType = switch (submitOrderReq.direction()) {
            case D_Buy -> ActionType.BUY;
            case D_Sell -> ActionType.SELL;
            default -> throw new IllegalArgumentException("Unexpected value: " + submitOrderReq.direction());
        };

        TradeOrderModel model = new TradeOrderModel();
        model.setAccount(settings.getAccountId());
        model.setSymbol(submitOrderReq.contract().symbol());
        model.setSecType(secType);
        model.setOrderType(orderType);
        model.setAction(actionType);
        model.setLimitPrice(orderType == OrderType.MKT ? null : submitOrderReq.price());
        model.setTotalQuantity((long) submitOrderReq.volume());
        model.setSecretKey(settings.getSecretKey());
        log.info("网关[{}] 下单：{}", gatewayId(), model);
        TradeOrderResponse response = client.execute(TradeOrderRequest.newRequest(model));
        log.info("网关[{}] 下单反馈：{}", gatewayId(), JSON.toJSONString(response));
        Long id = response.getItem().getId();
        orderIdMap.put(id, submitOrderReq.originOrderId());
        exec.execute(this::doQueryOrderAndTrade);
        return id + "";
    }

    @Override
    public boolean cancelOrder(String originOrderId) {
        if (connState != ConnectionState.CONNECTED) {
            throw new IllegalStateException("网关未连线");
        }
        for (Entry<Long, String> e : orderIdMap.entrySet()) {
            if (StringUtils.isNotEmpty(originOrderId) && StringUtils.equals(originOrderId, e.getValue())) {
                Long id = e.getKey();
                OrderParameter params = TradeParamBuilder.instance().account(settings.getAccountId()).id(id).secretKey(settings.getSecretKey()).build();
                String bizContent = JSON.toJSONString(params);
                TigerHttpRequest request = new TigerHttpRequest(MethodName.CANCEL_ORDER);
                request.setBizContent(bizContent);
                log.info("网关[{}] 撤单：{}", gatewayId(), bizContent);
                TigerHttpResponse response = client.execute(request);
                log.info("网关[{}] 撤单反馈：{}", gatewayId(), JSON.toJSONString(response));
                return response.isSuccess();
            }
        }

        return false;
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

}
