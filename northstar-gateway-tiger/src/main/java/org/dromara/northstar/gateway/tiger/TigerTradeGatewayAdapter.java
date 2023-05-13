package org.dromara.northstar.gateway.tiger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.TradeGateway;

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
import com.tigerbrokers.stock.openapi.client.struct.enums.ActionType;
import com.tigerbrokers.stock.openapi.client.struct.enums.Category;
import com.tigerbrokers.stock.openapi.client.struct.enums.Currency;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.MethodName;
import com.tigerbrokers.stock.openapi.client.struct.enums.OrderType;
import com.tigerbrokers.stock.openapi.client.struct.enums.SecType;
import com.tigerbrokers.stock.openapi.client.struct.param.OrderParameter;
import com.tigerbrokers.stock.openapi.client.util.builder.AccountParamBuilder;
import com.tigerbrokers.stock.openapi.client.util.builder.TradeParamBuilder;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 老虎证券交易网关适配器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TigerTradeGatewayAdapter implements TradeGateway{
	
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
	
	private Map<String, PositionField> lastPositions = new HashMap<>();
	
	public TigerTradeGatewayAdapter(FastEventEngine feEngine, GatewayDescription gd, IContractManager contractMgr) {
		this.feEngine = feEngine;
		this.gd = gd;
		this.settings = (TigerGatewaySettings) gd.getSettings();
		this.contractMgr = contractMgr;
	}
	
	@Override
	public synchronized void connect() {
		ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
		clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
		client = TigerHttpClient.getInstance().clientConfig(clientConfig);
		proxy = new OrderTradeQueryProxy(client, contractMgr, gatewayId(), settings.getAccountId());
		List<OrderField> orders = proxy.getDeltaOrder();
		List<String> symbols = orders.stream().map(OrderField::getContract).map(ContractField::getSymbol).distinct().toList();
		
		connState = ConnectionState.CONNECTED;
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewayId());
		
		orders.forEach(order -> feEngine.emitEvent(NorthstarEventType.ORDER, order));
		symbols.forEach(symbol -> {
			List<TradeField> trades = proxy.getTrades(symbol);
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
				} catch(Exception e) {
					log.error("", e);
				}
			}
		}, 3000, 1500);
	}
	
	private void doQueryOrderAndTrade() {
		proxy.getDeltaOrder().forEach(order -> {
			Long id = Long.valueOf(order.getOrderId());
			String originOrderId = orderIdMap.get(id);
			feEngine.emitEvent(NorthstarEventType.ORDER, order.toBuilder().setOriginOrderId(Optional.ofNullable(originOrderId).orElse("")).build());
			if(order.getTradedVolume() > 0) {
				proxy.getDeltaTrade(Long.valueOf(order.getOrderId()))
					.forEach(trade -> feEngine.emitEvent(NorthstarEventType.TRADE, trade.toBuilder().setOriginOrderId(Optional.ofNullable(originOrderId).orElse("")).build()));
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
		if(!primeAssetResponse.isSuccess()) {
			log.warn("查询账户返回异常：{}", primeAssetResponse.getMessage());
			return;
		}
		//查询证券相关资产信息
		PrimeAssetItem.Segment segment = primeAssetResponse.getSegment(Category.S);	// 暂不实现期货资产查询 
		//查询账号中美元相关资产信息
		if (segment != null) {
		  PrimeAssetItem.CurrencyAssets assetByCurrency = segment.getAssetByCurrency(Currency.USD);
		  feEngine.emitEvent(NorthstarEventType.ACCOUNT, AccountField.newBuilder()
				  .setAccountId(settings.getAccountId())
				  .setGatewayId(gatewayId())
				  .setAvailable(assetByCurrency.getCashBalance())
				  .setMargin(segment.getGrossPositionValue())
				  .setBalance(segment.getNetLiquidation())
				  .setPositionProfit(segment.getUnrealizedPL())
				  .setCloseProfit(segment.getRealizedPL())
				  .setCurrency(CurrencyEnum.USD)
				  .build());
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
		if(!response.isSuccess()) {
			log.warn("查询持仓返回异常：{}", response.getMessage());
			return;
		}
		// 解析具体字段
		JSONArray positions = JSON.parseObject(response.getData()).getJSONArray("items");
		Map<String, PositionField> positionMap = new HashMap<>();
		for(int i=0; i<positions.size(); i++) {
			JSONObject json = positions.getJSONObject(i);
			ContractField contract = contractMgr.getContract(ChannelType.TIGER, json.getString("symbol")).contractField();
			String positionId = String.format("%s@%s@%s", contract.getUnifiedSymbol(), PositionDirectionEnum.PD_Long, gatewayId());
			double openPrice = (int)(json.getDoubleValue("averageCost") / contract.getPriceTick()) * contract.getPriceTick();
			PositionField pf = PositionField.newBuilder()
					.setAccountId(settings.getAccountId())
					.setGatewayId(gatewayId())
					.setPositionId(positionId)
					.setContract(contract)
					.setOpenPositionProfit(json.getDoubleValue("unrealizedPnl"))
					.setPosition(json.getIntValue("position"))
					.setOpenPrice(openPrice)
					.setPrice(openPrice)
					.setPositionDirection(PositionDirectionEnum.PD_Long)
					.setPriceDiff(json.getDoubleValue("latestPrice") - openPrice)
					.build();
			positionMap.put(positionId, pf);
			feEngine.emitEvent(NorthstarEventType.POSITION, pf);
			lastPositions.remove(positionId);
		}
	
		lastPositions.values().forEach(pf -> {
			feEngine.emitEvent(NorthstarEventType.POSITION, pf.toBuilder().setPosition(0).build());
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
	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		if(connState != ConnectionState.CONNECTED) {
			throw new IllegalStateException("网关未连线");
		}
		
		SecType secType = switch(submitOrderReq.getContract().getProductClass()) {
		case EQUITY -> SecType.STK;
		case FUTURES -> SecType.FUT;
		default -> throw new IllegalArgumentException("Unexpected value: " + submitOrderReq.getContract().getProductClass());
		};
		
		OrderType orderType = switch(submitOrderReq.getOrderPriceType()) {
		case OPT_LimitPrice -> OrderType.LMT;
		case OPT_AnyPrice -> OrderType.MKT;
		default -> throw new IllegalArgumentException("老虎证券仅支持限价与市价两种订单类型");
		};
		
		ActionType actionType = switch(submitOrderReq.getDirection()) {
		case D_Buy -> ActionType.BUY;
		case D_Sell -> ActionType.SELL;
		default -> throw new IllegalArgumentException("Unexpected value: " + submitOrderReq.getDirection());
		};
		
		TradeOrderModel model = new TradeOrderModel();
		model.setAccount(settings.getAccountId());
		model.setSymbol(submitOrderReq.getContract().getSymbol());
		model.setSecType(secType);
		model.setOrderType(orderType);
		model.setAction(actionType);
		model.setLimitPrice(orderType == OrderType.MKT ? null : submitOrderReq.getPrice());
		model.setTotalQuantity(submitOrderReq.getVolume());
		model.setSecretKey(settings.getSecretKey());
		log.info("网关[{}] 下单：{}", gatewayId(), model);
		TradeOrderResponse response = client.execute(TradeOrderRequest.newRequest(model));
		log.info("网关[{}] 下单反馈：{}", gatewayId(), JSON.toJSONString(response));
		Long id = response.getItem().getId();
		orderIdMap.put(id, submitOrderReq.getOriginOrderId());
		exec.execute(this::doQueryOrderAndTrade);
		return id + "";
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if(connState != ConnectionState.CONNECTED) {
			throw new IllegalStateException("网关未连线");
		}
		for(Entry<Long, String> e : orderIdMap.entrySet()) {
			if(StringUtils.isNotEmpty(cancelOrderReq.getOriginOrderId()) && StringUtils.equals(cancelOrderReq.getOriginOrderId(), e.getValue())) {
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
