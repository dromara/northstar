package tech.quantit.northstar.gateway.tiger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
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
	
	private final Set<Long> pendingOrder = new HashSet<>();
	
	private TigerHttpClient client;
	private Timer timer;
	
	private TimerTask task = new TimerTask() {
		
		@Override
		public void run() {
			try {
				queryOrder(false);
				queryAccount();
				queryPosition();
			} catch(Exception e) {
				log.error("", e);
			}
		}
	};
	
	public TigerTradeGatewayAdapter(FastEventEngine feEngine, GatewayDescription gd, IContractManager contractMgr) {
		this.feEngine = feEngine;
		this.gd = gd;
		this.settings = (TigerGatewaySettings) gd.getSettings();
		this.contractMgr = contractMgr;
	}
	
	@Override
	public void connect() {
		ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
		clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
		client = TigerHttpClient.getInstance().clientConfig(clientConfig);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewayId());
		feEngine.emitEvent(NorthstarEventType.LOGGED_IN, gatewayId());
		timer = new Timer("TIGER_" + gatewayId(), true);
		CompletableFuture.runAsync(() -> {
			queryOrder(true);
			timer.scheduleAtFixedRate(task, 0, 1000);
		}, CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
	}

	@Override
	public void disconnect() {
		timer.cancel();
		client = null;
		feEngine.emitEvent(NorthstarEventType.LOGGED_OUT, gatewayId());
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewayId());
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
		for(int i=0; i<positions.size(); i++) {
			JSONObject json = positions.getJSONObject(i);
			ContractField contract = contractMgr.getContract("TIGER", json.getString("symbol")).contractField();
			String positionId = String.format("%s@%s@%s", contract.getUnifiedSymbol(), PositionDirectionEnum.PD_Long, gatewayId());
			feEngine.emitEvent(NorthstarEventType.POSITION, PositionField.newBuilder()
					.setAccountId(settings.getAccountId())
					.setGatewayId(gatewayId())
					.setPositionId(positionId)
					.setContract(contract)
					.setOpenPositionProfit(json.getDoubleValue("unrealizedPnl"))
					.setPosition(json.getIntValue("position"))
					.setOpenPrice(json.getDoubleValue("averageCost"))
					.setPositionDirection(PositionDirectionEnum.PD_Long)
					.setPrice(json.getDoubleValue("latestPrice"))
					.build());
		}
	}
	
	private void queryOrder(boolean showAll) {
		log.trace("查询TIGER订单信息");	
		TigerHttpRequest request = new TigerHttpRequest(MethodName.ORDERS);
		String bizContent = AccountParamBuilder.instance()
		        .account(settings.getAccountId())
		        .limit(100)
		        .buildJson();

		request.setBizContent(bizContent);
		TigerHttpResponse response = client.execute(request);
		if(!response.isSuccess()) {
			log.warn("查询订单返回异常：{}", response.getMessage());
			return;
		}
		JSONObject data = JSON.parseObject(response.getData());
		JSONArray items = data.getJSONArray("items");
		for(int i=0; i<items.size(); i++) {
			JSONObject json = items.getJSONObject(i);
			Long id = json.getLong("id");
			if(!pendingOrder.contains(id) && !showAll) {
				continue;
			}
			
			String orderId = id + "";
			DirectionEnum direction = switch(json.getString("action")) {
			case "BUY" -> DirectionEnum.D_Buy;
			case "SELL" -> DirectionEnum.D_Sell;
			default -> DirectionEnum.D_Unknown;
			};
			
			OffsetFlagEnum offset = switch(direction) {
			case D_Buy -> OffsetFlagEnum.OF_Open;
			case D_Sell -> OffsetFlagEnum.OF_Close;
			default -> throw new IllegalArgumentException("Unexpected value: " + direction);
			};
			
			OrderStatusEnum orderStatus = switch(json.getString("status")) {
			case "Filled" -> OrderStatusEnum.OS_AllTraded;
			case "Cancelled" -> OrderStatusEnum.OS_Canceled;
			case "Initial", "PendingSubmit", "Submitted" -> OrderStatusEnum.OS_Touched;
			case "Invalid", "Inactive" -> OrderStatusEnum.OS_Rejected;
			default -> throw new IllegalArgumentException("Unexpected value: " + json.getString("status"));
			};
			
			TimeConditionEnum timeCondition = switch(json.getString("timeInForce")) {
			case "DAY" -> TimeConditionEnum.TC_GFD;
			case "GTC" -> TimeConditionEnum.TC_GTC;
			case "GTD" -> TimeConditionEnum.TC_GTD;
			default -> throw new IllegalArgumentException("Unexpected value: " + json.getString(""));
			};
			
			Instant ins = Instant.ofEpochMilli(json.getLongValue("openTime"));
			String tradingDay = LocalDateTime.ofInstant(ins, ZoneOffset.ofHours(0)).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
			String orderDate = LocalDateTime.ofInstant(ins, ZoneId.systemDefault()).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
			String symbol = json.getString("symbol");
			int tradedVol = json.getIntValue("filledQuantity");
			int totalVol = json.getIntValue("totalQuantity");
			ContractField contract = contractMgr.getContract("TIGER", symbol).contractField();
			double price = (int) (json.getDoubleValue("limitPrice") / contract.getPriceTick()) * contract.getPriceTick();
			if(orderStatus == OrderStatusEnum.OS_Touched) {
				pendingOrder.add(id);
			}
			OrderField order = OrderField.newBuilder()
					.setAccountId(settings.getAccountId())
					.setGatewayId(gatewayId())
					.setContract(contract)
					.setOriginOrderId(orderId)
					.setOrderId(orderId)
					.setDirection(direction)
					.setOffsetFlag(offset)
					.setOrderDate(bizContent)
					.setTotalVolume(totalVol)
					.setTradedVolume(tradedVol)
					.setPrice(price)
					.setTradingDay(tradingDay)
					.setOrderDate(orderDate)
					.setOrderTime(LocalDateTime.ofInstant(ins, ZoneId.systemDefault()).toLocalTime().format(DateTimeConstant.T_FORMAT_FORMATTER))
					.setTimeCondition(timeCondition)
					.setOrderStatus(orderStatus)
					.build();
			feEngine.emitEvent(NorthstarEventType.ORDER, order);
			
			if(tradedVol > 0) {
				queryTrade(order, showAll);
			}
			if(tradedVol == totalVol && pendingOrder.contains(id)) {
				pendingOrder.remove(id);
			}
		}
	}
	
	private void queryTrade(OrderField order, boolean showAll) {
		log.trace("查询TIGER成交信息");	
		
		TigerHttpRequest request = new TigerHttpRequest(MethodName.ORDER_TRANSACTIONS);
		// FIXME 还是要根据OrderId去查询比较靠谱，但由于接口异常原因暂时无法按orderId查询。按名称查询如果结果集过大处理会比较麻烦
		String bizContent = AccountParamBuilder.instance()
		    .account(settings.getAccountId())
		    .secType(SecType.STK)
		    .symbol(order.getContract().getSymbol())
		    .limit(100)
		    .buildJson();
		request.setBizContent(bizContent);
		TigerHttpResponse response = client.execute(request);
		if(!response.isSuccess()) {
			log.warn("查询成交返回异常：{}", response.getMessage());
			return;
		}

		JSONArray data = JSON.parseObject(response.getData()).getJSONArray("items");
		for(int i=0; i<data.size(); i++) {
			JSONObject json = data.getJSONObject(i);
			String symbol = json.getString("symbol");
			long orderId = json.getLongValue("orderId");
			if(!showAll && orderId != Long.parseLong(order.getOrderId())) {
				continue;
			}
			
			feEngine.emitEvent(NorthstarEventType.TRADE, TradeField.newBuilder()
					.setAccountId(bizContent)
					.setGatewayId(gatewayId())
					.setTradeId(order.getOrderId())
					.setOrderId(order.getOrderId())
					.setOriginOrderId(order.getOriginOrderId())
					.setDirection(order.getDirection())
					.setOffsetFlag(order.getOffsetFlag())
					.setContract(order.getContract())
					.setPrice(json.getDoubleValue("filledPrice"))
					.setVolume(json.getIntValue("filledQuantity"))
					.setTradeDate(json.getString("transactedAt").split(" ")[0])
					.setTradeTime(json.getString("transactedAt").split(" ")[1])
					.setTradeTimestamp(json.getLongValue("transactionTime"))
					.setContract(contractMgr.getContract("TIGER", symbol).contractField())
					.build());
		}
	}
	
	@Override
	public boolean isConnected() {
		return client != null;
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		if(!isConnected()) {
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
		pendingOrder.add(id);
		return id + "";
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		OrderParameter params = TradeParamBuilder.instance().account(settings.getAccountId()).id(Long.valueOf(cancelOrderReq.getOriginOrderId())).secretKey(settings.getSecretKey()).build();
		String bizContent = JSON.toJSONString(params);
		TigerHttpRequest request = new TigerHttpRequest(MethodName.CANCEL_ORDER);
		request.setBizContent(bizContent);
		log.info("网关[{}] 撤单：{}", gatewayId(), bizContent);
		TigerHttpResponse response = client.execute(request);
		if(response.isSuccess()) {
			pendingOrder.remove(Long.valueOf(cancelOrderReq.getOriginOrderId()));
		}
		log.info("网关[{}] 撤单反馈：{}", gatewayId(), JSON.toJSONString(response));
		return response.isSuccess();
	}

	@Override
	public GatewayDescription gatewayDescription() {
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

}
