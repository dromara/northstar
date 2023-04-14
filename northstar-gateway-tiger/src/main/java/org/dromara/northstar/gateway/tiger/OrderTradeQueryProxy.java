package org.dromara.northstar.gateway.tiger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.api.IContractManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest;
import com.tigerbrokers.stock.openapi.client.https.response.TigerHttpResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.MethodName;
import com.tigerbrokers.stock.openapi.client.struct.enums.SecType;
import com.tigerbrokers.stock.openapi.client.util.builder.AccountParamBuilder;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class OrderTradeQueryProxy {

	private static final long EXPIRY = 24 * 3600 * 1000;	// 一天以内
	private final TigerHttpClient client;
	private final IContractManager contractMgr;
	private final String accountId;
	private final String gatewayId;
	
	private Map<Long, OrderField> orderIds = new HashMap<>();
	private Set<Long> tradeIds = new HashSet<>();
	
	public OrderTradeQueryProxy(TigerHttpClient client, IContractManager contractMgr,String gatewayId, String accountId) {
		this.client = client;
		this.contractMgr = contractMgr;
		this.accountId = accountId;
		this.gatewayId = gatewayId;
	}
	
	public List<OrderField> getDeltaOrder() {
		TigerHttpRequest request = new TigerHttpRequest(MethodName.ORDERS);
		String bizContent = AccountParamBuilder.instance()
		        .account(accountId)
		        .limit(100)
		        .buildJson();

		request.setBizContent(bizContent);
		TigerHttpResponse response = client.execute(request);
		if(!response.isSuccess()) {
			log.warn("查询订单返回异常：{}", response.getMessage());
			throw new IllegalStateException(response.getMessage());
		}
		JSONObject data = JSON.parseObject(response.getData());
		JSONArray items = data.getJSONArray("items");
		List<OrderField> resultList = new ArrayList<>(100);
		for(int i=0; i<items.size(); i++) {
			JSONObject json = items.getJSONObject(i);
			long openTime = getOpenTime(json);
			if(expired(openTime)) {
				continue;
			}
			Long id = json.getLong("id");
			OrderField order = convertOrder(json);
			OrderField oldOrder = orderIds.get(id);
			if(Objects.isNull(oldOrder) || order.getTradedVolume() != oldOrder.getTradedVolume() || order.getOrderStatus() != oldOrder.getOrderStatus()) {				
				orderIds.put(id, order);
				resultList.add(order);
			}
		}
		return resultList;
	}
	
	private boolean expired(long time) {
		return System.currentTimeMillis() - time > EXPIRY;
	}
	
	private long getOpenTime(JSONObject json) {
		return json.getLongValue("openTime");
	}
	
	private OrderField convertOrder(JSONObject json) {
		Long id = json.getLong("id");
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
		
		String symbol = json.getString("symbol");
		ContractField contract = contractMgr.getContract("TIGER", symbol).contractField();
		String orderMsg = json.getString("remark");
		if(orderStatus == OrderStatusEnum.OS_Rejected && !orderIds.containsKey(id)) {
			if(StringUtils.isEmpty(orderMsg)) {
				log.warn("废单反馈：{}", json.toString(SerializerFeature.PrettyFormat));
			} else {
				long openTime = getOpenTime(json);
				LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneId.systemDefault());
				log.warn("废单信息：{} {} {}", ldt, contract.getName(), orderMsg);
			}
		}
		Instant ins = Instant.ofEpochMilli(getOpenTime(json));
		String tradingDay = LocalDateTime.ofInstant(ins, ZoneOffset.ofHours(0)).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		String orderDate = LocalDateTime.ofInstant(ins, ZoneId.systemDefault()).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
		int tradedVol = json.getIntValue("filledQuantity");
		int totalVol = json.getIntValue("totalQuantity");
		double price = (int) (json.getDoubleValue("limitPrice") / contract.getPriceTick()) * contract.getPriceTick();
		return OrderField.newBuilder()
				.setAccountId(accountId)
				.setGatewayId(gatewayId)
				.setContract(contract)
				.setOrderId(orderId)
				.setDirection(direction)
				.setOffsetFlag(offset)
				.setOrderDate(orderDate)
				.setTotalVolume(totalVol)
				.setTradedVolume(tradedVol)
				.setPrice(price)
				.setTradingDay(tradingDay)
				.setOrderDate(orderDate)
				.setOrderTime(LocalDateTime.ofInstant(ins, ZoneId.systemDefault()).toLocalTime().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setTimeCondition(timeCondition)
				.setOrderStatus(orderStatus)
				.build();
	}
	
	public List<TradeField> getTrades(String symbol) {
		log.trace("查询TIGER成交信息");	
		
		TigerHttpRequest request = new TigerHttpRequest(MethodName.ORDER_TRANSACTIONS);
		String bizContent = AccountParamBuilder.instance()
		    .account(accountId)
		    .secType(SecType.STK)
		    .symbol(symbol)
		    .limit(100)
		    .buildJson();
		request.setBizContent(bizContent);
		TigerHttpResponse response = client.execute(request);
		if(!response.isSuccess()) {
			log.warn("查询成交返回异常：{}", response.getMessage());
			throw new IllegalStateException(response.getMessage());
		}
		
		return resolveData(response);
	}
	
	public List<TradeField> getDeltaTrade(Long id) {
		log.trace("查询TIGER成交信息");	
		
		TigerHttpRequest request = new TigerHttpRequest(MethodName.ORDER_TRANSACTIONS);
		String bizContent = AccountParamBuilder.instance()
		    .account(accountId)
		    .secType(SecType.STK)
		    .orderId(id)
		    .limit(100)
		    .buildJson();
		request.setBizContent(bizContent);
		TigerHttpResponse response = client.execute(request);
		if(!response.isSuccess()) {
			log.warn("查询成交返回异常：{}", response.getMessage());
			throw new IllegalStateException(response.getMessage());
		}

		return resolveData(response);
	}
	
	private List<TradeField> resolveData(TigerHttpResponse response){
		JSONArray data = JSON.parseObject(response.getData()).getJSONArray("items");
		List<TradeField> resultList = new ArrayList<>();
		for(int i=0; i<data.size(); i++) {
			JSONObject json = data.getJSONObject(i);
			String symbol = json.getString("symbol");
			ContractField contract = contractMgr.getContract("TIGER", symbol).contractField();
			Long tradeId = json.getLong("id");
			Long tradeTime = json.getLongValue("transactionTime");
			if(tradeIds.contains(tradeId) || expired(tradeTime)) {
				continue;
			}
			tradeIds.add(tradeId);
			String orderId = json.getString("orderId");
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
			TradeField trade = TradeField.newBuilder()
					.setAccountId(accountId)
					.setGatewayId(gatewayId)
					.setTradeId(tradeId + "")
					.setOrderId(orderId)
					.setDirection(direction)
					.setOffsetFlag(offset)
					.setContract(contract)
					.setPrice(json.getDoubleValue("filledPrice"))
					.setVolume(json.getIntValue("filledQuantity"))
					.setTradeDate(json.getString("transactedAt").split(" ")[0])
					.setTradeTime(json.getString("transactedAt").split(" ")[1])
					.setTradeTimestamp(tradeTime)
					.setContract(contractMgr.getContract("TIGER", symbol).contractField())
					.build();
			resultList.add(trade);
		}
		return resultList;
	}
}
