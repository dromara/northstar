package tech.quantit.northstar.gateway.tiger;

import com.alibaba.fastjson2.JSON;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.trade.model.TradeOrderModel;
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest;
import com.tigerbrokers.stock.openapi.client.https.request.trade.TradeOrderRequest;
import com.tigerbrokers.stock.openapi.client.https.response.TigerHttpResponse;
import com.tigerbrokers.stock.openapi.client.https.response.trade.TradeOrderResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.ActionType;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.MethodName;
import com.tigerbrokers.stock.openapi.client.struct.enums.OrderType;
import com.tigerbrokers.stock.openapi.client.struct.enums.SecType;
import com.tigerbrokers.stock.openapi.client.util.builder.TradeParamBuilder;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.TradeGateway;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

/**
 * 老虎证券交易网关适配器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class TigerTradeGatewayAdapter implements TradeGateway{
	
	private FastEventEngine feEngine;
	private GatewayDescription gd;
	private TigerHttpClient client;
	
	public TigerTradeGatewayAdapter(FastEventEngine feEngine, GatewayDescription gd) {
		this.feEngine = feEngine;
		this.gd = gd;
	}
	
	@Override
	public void connect() {
		TigerGatewaySettings settings = (TigerGatewaySettings) gd.getSettings();
		ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
		clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
		client = TigerHttpClient.getInstance().clientConfig(clientConfig);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewayId());
	}

	@Override
	public void disconnect() {
		client = null;
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewayId());
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
		
		TigerGatewaySettings settings = (TigerGatewaySettings) gd.getSettings();
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
		return response.getItem().getOrderId() + "";
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if(!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		TigerGatewaySettings settings = (TigerGatewaySettings) gd.getSettings();
		String bizContent = TradeParamBuilder.instance().account(settings.getAccountId()).id(Long.valueOf(cancelOrderReq.getOriginOrderId())).secretKey(settings.getSecretKey()).buildJson();
		TigerHttpRequest request = new TigerHttpRequest(MethodName.CANCEL_ORDER);
		request.setBizContent(bizContent);
		log.info("网关[{}] 撤单：{}", gatewayId(), bizContent);
		TigerHttpResponse response = client.execute(request);
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
