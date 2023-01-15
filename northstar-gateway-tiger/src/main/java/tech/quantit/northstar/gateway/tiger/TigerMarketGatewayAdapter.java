package tech.quantit.northstar.gateway.tiger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fastjson.JSONObject;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.socket.ApiComposeCallback;
import com.tigerbrokers.stock.openapi.client.socket.WebSocketClient;
import com.tigerbrokers.stock.openapi.client.struct.SubscribedSymbol;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.QuoteKeyType;
import com.tigerbrokers.stock.openapi.client.util.ApiLogger;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.MarketGateway;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.TickField;


@Slf4j
public class TigerMarketGatewayAdapter implements MarketGateway {

	private GatewayDescription gd;
	
	private FastEventEngine feEngine;
	
	private WebSocketClient client;
	
	private TigerSpi spi;
	
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
		this.client = WebSocketClient.getInstance().clientConfig(clientConfig).apiComposeCallback(spi);
		ApiLogger.setEnabled(true, "logs/");
	}
	
	@Override
	public void connect() {
		client.connect();
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gd.getGatewayId());
		feEngine.emitEvent(NorthstarEventType.GATEWAY_READY, gd.getGatewayId());
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
			log.info("TIGER网关订阅合约 {} {}", contract.getName(), contract.getUnifiedSymbol());
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
	
	@Override
	public GatewayDescription gatewayDescription() {
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
		
		private ConcurrentMap<String, TickField.Builder> tickBuilderMap = new ConcurrentHashMap<>();
		
		private FastEventEngine feEngine;
		private IContractManager contractMgr;
		
		private long lastActive;
		
		public TigerSpi(FastEventEngine feEngine, IContractManager contractMgr) {
			this.feEngine = feEngine;
			this.contractMgr = contractMgr;
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
			if(log.isTraceEnabled()) {
				log.trace("数据回报：{}", jsonObject);
			}
			if(jsonObject.containsKey("hourTradingTag")) {
				return;	//忽略盘前盘后数据
			}
			try {
				
				String symbol = jsonObject.getString("symbol");
				long timestamp = jsonObject.getLongValue("timestamp");
				LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
				ZoneId londonTimeZone = ZoneId.of(ZoneOffset.ofHours(0).getId()); // 伦敦时区正好可以让所有时区的交易计算在同一天
				tickBuilderMap.computeIfAbsent(symbol, key -> TickField.newBuilder()
						.setGatewayId(gd.getGatewayId())
						.addAllAskPrice(List.of(0D))
						.addAllAskVolume(List.of(0))
						.addAllBidPrice(List.of(0D))
						.addAllBidVolume(List.of(0))
						.setUnifiedSymbol(contractMgr.getContract("TIGER", symbol).contractField().getUnifiedSymbol()));
				if(jsonObject.containsKey(ASK_P))	tickBuilderMap.get(symbol).setAskPrice(0, jsonObject.getDoubleValue(ASK_P));
				if(jsonObject.containsKey(BID_P))	tickBuilderMap.get(symbol).setBidPrice(0, jsonObject.getDoubleValue(BID_P));
				if(jsonObject.containsKey(ASK_V))	tickBuilderMap.get(symbol).setAskVolume(0, jsonObject.getIntValue(ASK_V));
				if(jsonObject.containsKey(BID_V))	tickBuilderMap.get(symbol).setBidVolume(0, jsonObject.getIntValue(BID_V));
				
				if(jsonObject.containsKey(MKT_STAT) && jsonObject.getString(MKT_STAT).equals("交易中")) {		
					// 交易数据更新
					feEngine.emitEvent(NorthstarEventType.TICK, tickBuilderMap.get(symbol)
							.setActionDay(ldt.toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
							.setActionTime(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))		
							.setTradingDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), londonTimeZone).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
							.setActionTimestamp(timestamp)
							.setPreClosePrice(jsonObject.getDoubleValue("preClose"))
							.setLastPrice(jsonObject.getDoubleValue("latestPrice"))
							.setHighPrice(jsonObject.getDoubleValue("high"))
							.setLowPrice(jsonObject.getDoubleValue("low"))
							.setOpenPrice(jsonObject.getDoubleValue("open"))
							.setVolume(jsonObject.getLongValue("volume"))
							.build());
				} else if(!jsonObject.containsKey(MKT_STAT)){
					// 盘口数据更新
					feEngine.emitEvent(NorthstarEventType.TICK, tickBuilderMap.get(symbol)
							.setActionDay(ldt.toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
							.setActionTime(ldt.toLocalTime().format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))		
							.setTradingDay(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), londonTimeZone).toLocalDate().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
							.setActionTimestamp(timestamp)
							.build());
				}
			} catch(Exception e) {
				log.warn("异常数据：{}", jsonObject);
				log.error("", e);
			}
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
