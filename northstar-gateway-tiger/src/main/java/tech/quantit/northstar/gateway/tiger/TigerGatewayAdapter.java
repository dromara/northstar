package tech.quantit.northstar.gateway.tiger;

import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback;
import com.tigerbrokers.stock.openapi.client.socket.WebSocketClient;
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.QuoteKeyType;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.TickField;


@Slf4j
public class TigerGatewayAdapter implements MarketGateway {

	private GatewayDescription gd;
	
	private FastEventEngine feEngine;
	
	private WebSocketClient client;
	
	private TigerSpi spi;
	
	public TigerGatewayAdapter(GatewayDescription gd, FastEventEngine feEngine) {
		this.gd = gd;
		this.feEngine = feEngine;
		this.spi = new TigerSpi(feEngine); 
		
		TigerGatewaySettings settings = (TigerGatewaySettings) gd.getSettings();
		ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
		clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
		this.client = WebSocketClient.getInstance().clientConfig(clientConfig).apiComposeCallback(spi);
	}
	
	@Override
	public GatewaySettingField getGatewaySetting() {
		return GatewaySettingField.newBuilder()
				.setGatewayId(gd.getGatewayId())
				.setGatewayType(GatewayTypeEnum.GTE_MarketData)
				.build();
	}

	@Override
	public void connect() {
		client.connect();
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gd.getGatewayId());
	}

	@Override
	public void disconnect() {
		client.disconnect();
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gd.getGatewayId());
	}

	@Override
	public boolean isConnected() {
		return client.isConnected();
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public boolean subscribe(ContractField contract) {
		if(contract.getProductClass() == ProductClassEnum.EQUITY) {
			client.subscribeQuote(Set.of(contract.getSymbol()), QuoteKeyType.ALL);
		}
		// TODO 期货期权暂没实现
		return true;
	}

	@Override
	public boolean unsubscribe(ContractField contract) {
		if(contract.getProductClass() == ProductClassEnum.EQUITY) {
			client.cancelSubscribeQuote(Set.of(contract.getSymbol()));
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

	class TigerSpi implements ApiComposeCallback {
		
		private FastEventEngine feEngine;
		
		private long lastActive;
		
		public TigerSpi(FastEventEngine feEngine) {
			this.feEngine = feEngine;
		}

		@Override
		public void orderStatusChange(JSONObject jsonObject) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void positionChange(JSONObject jsonObject) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void assetChange(JSONObject jsonObject) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void quoteChange(JSONObject jsonObject) {
			lastActive = System.currentTimeMillis();
			if(!jsonObject.containsKey("avgPrice")) {
				return;	//忽略盘前盘后数据
			}
			long timestamp = jsonObject.getLongValue("timestamp");
			feEngine.emitEvent(NorthstarEventType.TICK, TickField.newBuilder()
					.setGatewayId(gd.getGatewayId())
					.setActionDay("")		// FIXME
					.setActionTime("")		// FIXME
					.setTradingDay("")		// FIXME
					.setActionTimestamp(timestamp)
					.setPreClosePrice(jsonObject.getDoubleValue("preClose"))
					.setPreOpenInterest(lastActive)
					.setPreSettlePrice(lastActive)
					.setLastPrice(jsonObject.getDoubleValue("latestPrice"))
					.setHighPrice(jsonObject.getDoubleValue("high"))
					.setLowPrice(jsonObject.getDoubleValue("low"))
					.setOpenPrice(jsonObject.getDoubleValue("open"))
					.setVolumeDelta(jsonObject.getLongValue("volume"))
					.addAllAskPrice(List.of(jsonObject.getDouble("askPrice")))
					.addAllAskVolume(List.of(jsonObject.getIntValue("askSize")))
					.addAllBidPrice(List.of(jsonObject.getDouble("bidPrice")))
					.addAllBidVolume(List.of(jsonObject.getIntValue("bidSize")))
					.build());
		}

		@Override
		public void tradeTickChange(JSONObject jsonObject) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void optionChange(JSONObject jsonObject) {
			lastActive = System.currentTimeMillis();
		}

		@Override
		public void futureChange(JSONObject jsonObject) {
			lastActive = System.currentTimeMillis();			
		}

		@Override
		public void depthQuoteChange(JSONObject jsonObject) {
		}

		@Override
		public void subscribeEnd(String id, String subject, JSONObject jsonObject) {
			log.info("成功订阅 [{} {} {}]", id, subject, jsonObject);
		}

		@Override
		public void cancelSubscribeEnd(String id, String subject, JSONObject jsonObject) {
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
		public void connectionKickoff(int errorCode, String errorMsg) {
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
