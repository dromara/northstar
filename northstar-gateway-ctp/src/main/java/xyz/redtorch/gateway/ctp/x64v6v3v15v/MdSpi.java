package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.common.utils.CommonUtils;
import tech.quantit.northstar.common.utils.MarketDateTimeUtil;
import tech.quantit.northstar.common.utils.MessagePrinter;
import tech.quantit.northstar.gateway.api.GatewayAbstract;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.contract.GatewayContract;
import xyz.redtorch.gateway.ctp.common.CtpDateTimeUtil;
import xyz.redtorch.gateway.ctp.common.GatewayConstants;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcDepthMarketDataField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcForQuoteRspField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcMdApi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcMdSpi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspInfoField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSpecificInstrumentField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcUserLogoutField;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.TickField;

public class MdSpi extends CThostFtdcMdSpi {

	private final static int CONNECTION_STATUS_DISCONNECTED = 0;
	private final static int CONNECTION_STATUS_CONNECTED = 1;
	private final static int CONNECTION_STATUS_CONNECTING = 2;
	private final static int CONNECTION_STATUS_DISCONNECTING = 3;

	private static final Logger logger = LoggerFactory.getLogger(MdSpi.class);

	private GatewayAbstract gatewayAdapter;
	private String logInfo;
	private String gatewayId;
	private String tradingDay;

	private volatile long lastUpdateTickTime = System.currentTimeMillis();

	private Map<Identifier, TickField> preTickMap = new HashMap<>();

	private Set<String> subscribedSymbolSet = ConcurrentHashMap.newKeySet();
	
	private MarketDateTimeUtil mktTimeUtil = new CtpDateTimeUtil();

	MdSpi(GatewayAbstract gatewayAdapter) {
		this.gatewayAdapter = gatewayAdapter;
		this.gatewayId = gatewayAdapter.getGatewaySetting().getGatewayId();
		this.logInfo = "行情网关ID-[" + this.gatewayId + "] [→] ";
		logger.info("当前MdApi版本号：{}", CThostFtdcMdApi.GetApiVersion());
	}

	private CThostFtdcMdApi cThostFtdcMdApi;

	private int connectionStatus = CONNECTION_STATUS_DISCONNECTED; // 避免重复调用
	private boolean loginStatus = false; // 登陆状态

	public void connect() {
		if (isConnected() || connectionStatus == CONNECTION_STATUS_CONNECTING) {
			return;
		}

		if (connectionStatus == CONNECTION_STATUS_CONNECTED) {
			login();
			return;
		}

		connectionStatus = CONNECTION_STATUS_CONNECTING;
		loginStatus = false;
		gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.CONNECTING, gatewayId);

		if (cThostFtdcMdApi != null) {
			try {
				logger.warn("{}行情接口检测到旧实例,准备释放", logInfo);
				CThostFtdcMdApi cThostFtdcMdApiForRelease = cThostFtdcMdApi;
				cThostFtdcMdApi = null;
				cThostFtdcMdApiForRelease.RegisterSpi(null);

				new Thread() {
					public void run() {
						Thread.currentThread().setName("GatewayId [" + gatewayId + "] MD API Release Thread, Start Time " + System.currentTimeMillis());
						try {
							logger.warn("行情接口异步释放启动！");
							cThostFtdcMdApiForRelease.Release();
							logger.warn("行情接口异步释放完成！");
						} catch (Throwable t) {
							logger.error("行情接口异步释放发生异常！", t);
						}
					}
				}.start();

				Thread.sleep(100);
			} catch (Throwable t) {
				logger.warn("{}交易接口连接前释放异常", logInfo, t);
			}
		}

		logger.warn("{}行情接口实例初始化", logInfo);

		String envTmpDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "gateway" + File.separator + "ctp" + File.separator + "jctpv6v3v15x64api"
				+ File.separator + "CTP_FLOW_TEMP" + File.separator + "MD_" + this.gatewayId;
		File tempFile = new File(tempFilePath);
		if (!tempFile.getParentFile().exists()) {
			try {
				FileUtils.forceMkdirParent(tempFile);
				logger.info("{}行情接口创建临时文件夹:{}", logInfo, tempFile.getParentFile().getAbsolutePath());
			} catch (IOException e) {
				logger.error("{}行情接口创建临时文件夹失败", logInfo, e);
			}
		}

		logger.warn("{}行情接口使用临时文件夹:{}", logInfo, tempFile.getParentFile().getAbsolutePath());

		try {
			String mdHost = GatewayConstants.SMART_CONNECTOR.bestEndpoint(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getBrokerId());
			String mdPort = gatewayAdapter.getGatewaySetting().getCtpApiSetting().getMdPort();
			logger.info("使用IP [{}] 连接行情网关", mdHost);
			cThostFtdcMdApi = CThostFtdcMdApi.CreateFtdcMdApi(tempFile.getAbsolutePath());
			cThostFtdcMdApi.RegisterSpi(this);
			cThostFtdcMdApi.RegisterFront("tcp://" + mdHost + ":" + mdPort);
			cThostFtdcMdApi.Init();
		} catch (Throwable t) {
			logger.error("{}行情接口连接异常", logInfo, t);
		}

		new Thread() {
			public void run() {
				try {
					Thread.sleep(15 * 1000);
					if (!isConnected()) {
						logger.error("{}行情接口连接超时,尝试断开", logInfo);
						gatewayAdapter.disconnect();
					}

				} catch (Throwable t) {
					logger.error("{}行情接口处理连接超时线程异常", logInfo, t);
				}
			}

		}.start();

	}

	// 关闭
	public void disconnect() {
		if (cThostFtdcMdApi != null && connectionStatus != CONNECTION_STATUS_DISCONNECTING) {
			logger.warn("{}行情接口实例开始关闭并释放", logInfo);
			loginStatus = false;
			connectionStatus = CONNECTION_STATUS_DISCONNECTING;
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.DISCONNECTING, gatewayId);
			if (cThostFtdcMdApi != null) {
				try {
					CThostFtdcMdApi cThostFtdcMdApiForRelease = cThostFtdcMdApi;
					cThostFtdcMdApi = null;
					cThostFtdcMdApiForRelease.RegisterSpi(null);
					new Thread() {
						public void run() {
							Thread.currentThread().setName("GatewayId " + gatewayId + " MD API Release Thread, Time " + System.currentTimeMillis());
							try {
								logger.warn("行情接口异步释放启动！");
								cThostFtdcMdApiForRelease.Release();
								logger.warn("行情接口异步释放完成！");
							} catch (Throwable t) {
								logger.error("行情接口异步释放发生异常", t);
							}
						}
					}.start();
					Thread.sleep(100);
				} catch (Throwable t) {
					logger.error("{}行情接口实例关闭并释放异常", logInfo, t);
				}
			}
			connectionStatus = CONNECTION_STATUS_DISCONNECTED;
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.DISCONNECTED, gatewayId);
			logger.warn("{}行情接口实例关闭并释放", logInfo);
		} else {
			logger.warn("{}行情接口实例不存在,无需关闭释放", logInfo);
		}

	}

	// 返回接口状态
	public boolean isConnected() {
		return connectionStatus == CONNECTION_STATUS_CONNECTED && loginStatus;
	}

	// 获取交易日
	public String getTradingDay() {
		return tradingDay;
	}

	// 订阅行情
	public boolean subscribe(String symbol) {
		logger.debug("订阅合约：{}", symbol);
		subscribedSymbolSet.add(symbol);
		if (isConnected()) {
			try {
				cThostFtdcMdApi.SubscribeMarketData(new String[]{symbol}, 1);
			} catch (Throwable t) {
				logger.error("{}订阅行情异常,合约代码{}", logInfo, symbol, t);
				return false;
			}
			return true;
		} else {
			logger.warn("{}无法订阅行情,行情服务器尚未连接成功,合约代码:{}", logInfo, symbol);
			return false;
		}
	}

	// 退订行情
	public boolean unsubscribe(String symbol) {
		subscribedSymbolSet.remove(symbol);
		if (isConnected()) {
			try {
				cThostFtdcMdApi.UnSubscribeMarketData(new String[]{symbol}, 1);
			} catch (Throwable t) {
				logger.error("{}行情退订异常,合约代码{}", logInfo, symbol, t);
				return false;
			}
			return true;
		} else {
			logger.warn("{}行情退订无效,行情服务器尚未连接成功,合约代码:{}", logInfo, symbol);
			return false;
		}
	}

	private void login() {
		if (StringUtils.isEmpty(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getBrokerId()) 
				|| StringUtils.isEmpty(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getUserId()) 
				|| StringUtils.isEmpty(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getPassword())) {
			logger.error("{}BrokerId UserID Password 不可为空", logInfo);
			return;
		}
		try {
			// 登录
			CThostFtdcReqUserLoginField userLoginField = new CThostFtdcReqUserLoginField();
			userLoginField.setBrokerID(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getBrokerId());
			userLoginField.setUserID(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getUserId());
			userLoginField.setPassword(gatewayAdapter.getGatewaySetting().getCtpApiSetting().getPassword());
			cThostFtdcMdApi.ReqUserLogin(userLoginField, 0);
		} catch (Throwable t) {
			logger.error("{}登录异常", logInfo, t);
		}

	}

	// 前置机联机回报
	public void OnFrontConnected() {
		try {
			logger.info(logInfo + "行情接口前置机已连接");
			// 修改前置机连接状态
			connectionStatus = CONNECTION_STATUS_CONNECTED;
			
			login();
			
		} catch (Throwable t) {
			logger.error("{} OnFrontConnected Exception", logInfo, t);
		}
	}

	// 前置机断开回报
	public void OnFrontDisconnected(int nReason) {
		try {
			logger.warn("{}行情接口前置机已断开, 原因:{}", logInfo, nReason);
			gatewayAdapter.disconnect();
			
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.DISCONNECTED, gatewayId);
			
		} catch (Throwable t) {
			logger.error("{} OnFrontDisconnected Exception", logInfo, t);
		}
	}

	// 登录回报
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo.getErrorID() == 0) {
				logger.info("{}OnRspUserLogin TradingDay:{},SessionID:{},BrokerId:{},UserID:{}", logInfo, pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
						pRspUserLogin.getUserID());
				tradingDay = pRspUserLogin.getTradingDay();
				// 修改登录状态为true
				this.loginStatus = true;
				logger.info("{}行情接口获取到的交易日为{}", logInfo, tradingDay);

				if (!subscribedSymbolSet.isEmpty()) {
					String[] symbolArray = subscribedSymbolSet.toArray(new String[subscribedSymbolSet.size()]);
					cThostFtdcMdApi.SubscribeMarketData(symbolArray, subscribedSymbolSet.size());
				}
				
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.CONNECTED, gatewayId);
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.GATEWAY_READY, gatewayId);
			} else {
				logger.warn("{}行情接口登录回报错误 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				// 不合法的登录
				if (pRspInfo.getErrorID() == 3) {
					gatewayAdapter.setAuthErrorFlag(true);
				}
			}

		} catch (Throwable t) {
			logger.error("{} OnRspUserLogin Exception", logInfo, t);
		}

	}

	// 心跳警告
	public void OnHeartBeatWarning(int nTimeLapse) {
		logger.warn(logInfo + "行情接口心跳警告 nTimeLapse:" + nTimeLapse);
	}

	// 登出回报
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {

			if (pRspInfo.getErrorID() != 0) {
				logger.error("{}OnRspUserLogout!错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			} else {
				logger.warn("{}OnRspUserLogout!BrokerId:{},UserID:{}", logInfo, pUserLogout.getBrokerID(), pUserLogout.getUserID());

			}

		} catch (Throwable t) {
			logger.error("{} OnRspUserLogout Exception", logInfo, t);
		}

		this.loginStatus = false;
	}

	// 错误回报
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			logger.error("{}行情接口错误回报!错误ID:{},错误信息:{},请求ID:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg(), nRequestID);
		} else {
			logger.error("{}行情接口错误回报!不存在错误回报信息", logInfo);
		}
	}

	// 订阅合约回报
	public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			if (pRspInfo.getErrorID() == 0) {
				if (pSpecificInstrument != null) {
					logger.debug("{}行情接口订阅合约成功:{}", logInfo, pSpecificInstrument.getInstrumentID());
				} else {
					logger.error("{}行情接口订阅合约成功,不存在合约信息", logInfo);
				}
			} else {
				logger.error("{}行情接口订阅合约失败,错误ID:{} 错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			}
		} else {
			logger.info("{}行情接口订阅回报，不存在回报信息", logInfo);
		}
	}

	// 退订合约回报
	public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			if (pRspInfo.getErrorID() == 0) {
				if (pSpecificInstrument != null) {
					logger.debug("{}行情接口退订合约成功:{}", logInfo, pSpecificInstrument.getInstrumentID());
				} else {
					logger.error("{}行情接口退订合约成功,不存在合约信息", logInfo);
				}
			} else {
				logger.error("{}行情接口退订合约失败,错误ID:{} 错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			}
		} else {
			logger.info("{}行情接口退订回报，不存在回报信息", logInfo);
		}
	}
	
	// 合约行情推送
	public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
		if (pDepthMarketData != null) {
			try {
				String symbol = pDepthMarketData.getInstrumentID();
				
				if(!mktTimeUtil.isOpeningTime(symbol, LocalTime.now())) {
					logger.trace("[{}] 收到非开市时间数据", symbol);
					return;
				}

				Contract contract = gatewayAdapter.mktCenter.getContract(gatewayId, symbol);

				String actionDay = pDepthMarketData.getActionDay();
				actionDay = StringUtils.isEmpty(actionDay) ? LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER) : actionDay;

				// 接口返回的updateTime数据格式为 HH:mm:ss
				String updateTimeStr = pDepthMarketData.getUpdateTime().replaceAll(":", "");
				Long updateTime = Long.valueOf(updateTimeStr);
				Long updateMillisec = (long) pDepthMarketData.getUpdateMillisec();
				
				/*
				 * 大商所获取的ActionDay可能是不正确的,因此这里采用本地时间修正 1.请注意，本地时间应该准确 2.使用 SimNow 7x24
				 * 服务器获取行情时,这个修正方式可能会导致问题
				 */
				if (contract.exchange() == ExchangeEnum.DCE) {
					// 只修正夜盘
					if (updateTime > 200000 && updateTime <= 235959) {
						actionDay = LocalDateTime.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
					}
				}

				String updateDateTimeWithMS = String.format("%s%s%03d", actionDay, updateTimeStr, updateMillisec);
				LocalDateTime dateTime;
				try {
					dateTime = LocalDateTime.parse(updateDateTimeWithMS, DateTimeConstant.DT_FORMAT_WITH_MS_INT_FORMATTER);
				} catch (Exception e) {
					logger.error("{}解析日期发生异常", logInfo, e);
					return;
				}
				
				String actionTime = dateTime.format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
				double lastPrice = pDepthMarketData.getLastPrice();
				long volume = pDepthMarketData.getVolume();	//该成交量为当日累计值
				long volumeDelta = 0;
				if (preTickMap.containsKey(contract.identifier())) {
					volumeDelta = volume - preTickMap.get(contract.identifier()).getVolume();
					volumeDelta = Math.max(0, volumeDelta);	//防止数据异常时为负数
				}

				Double turnover = pDepthMarketData.getTurnover();	//该金额为当日累计值
				double turnoverDelta = 0;
				if (preTickMap.containsKey(contract.identifier())) {
					turnoverDelta = turnover - preTickMap.get(contract.identifier()).getTurnover();
				}

				Long preOpenInterest = (long) pDepthMarketData.getPreOpenInterest();

				double openInterest = pDepthMarketData.getOpenInterest();
				int openInterestDelta = 0;
				if (preTickMap.containsKey(contract.identifier())) {
					openInterestDelta = (int) (openInterest - preTickMap.get(contract.identifier()).getOpenInterest());
				}

				Double preClosePrice = pDepthMarketData.getPreClosePrice();
				Double preSettlePrice = pDepthMarketData.getPreSettlementPrice();
				Double openPrice = pDepthMarketData.getOpenPrice();
				Double highPrice = pDepthMarketData.getHighestPrice();
				Double lowPrice = pDepthMarketData.getLowestPrice();
				Double upperLimit = pDepthMarketData.getUpperLimitPrice();
				Double lowerLimit = pDepthMarketData.getLowerLimitPrice();

				List<Double> bidPriceList = new ArrayList<>();
				
				bidPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getBidPrice1(), Double.MAX_VALUE) ? pDepthMarketData.getBidPrice1() : 0);
				bidPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getBidPrice2(), Double.MAX_VALUE) ? pDepthMarketData.getBidPrice2() : 0);
				bidPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getBidPrice3(), Double.MAX_VALUE) ? pDepthMarketData.getBidPrice3() : 0);
				bidPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getBidPrice4(), Double.MAX_VALUE) ? pDepthMarketData.getBidPrice4() : 0);
				bidPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getBidPrice5(), Double.MAX_VALUE) ? pDepthMarketData.getBidPrice5() : 0);
				List<Integer> bidVolumeList = new ArrayList<>();
				bidVolumeList.add(pDepthMarketData.getBidVolume1());
				bidVolumeList.add(pDepthMarketData.getBidVolume2());
				bidVolumeList.add(pDepthMarketData.getBidVolume3());
				bidVolumeList.add(pDepthMarketData.getBidVolume4());
				bidVolumeList.add(pDepthMarketData.getBidVolume5());

				List<Double> askPriceList = new ArrayList<>();
				askPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getAskPrice1(), Double.MAX_VALUE) ? pDepthMarketData.getAskPrice1(): 0);
				askPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getAskPrice2(), Double.MAX_VALUE) ? pDepthMarketData.getAskPrice2(): 0);
				askPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getAskPrice3(), Double.MAX_VALUE) ? pDepthMarketData.getAskPrice3(): 0);
				askPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getAskPrice4(), Double.MAX_VALUE) ? pDepthMarketData.getAskPrice4(): 0);
				askPriceList.add(!CommonUtils.isEquals(pDepthMarketData.getAskPrice5(), Double.MAX_VALUE) ? pDepthMarketData.getAskPrice5(): 0);
				List<Integer> askVolumeList = new ArrayList<>();
				askVolumeList.add(pDepthMarketData.getAskVolume1());
				askVolumeList.add(pDepthMarketData.getAskVolume2());
				askVolumeList.add(pDepthMarketData.getAskVolume3());
				askVolumeList.add(pDepthMarketData.getAskVolume4());
				askVolumeList.add(pDepthMarketData.getAskVolume5());

				Double averagePrice = pDepthMarketData.getAveragePrice();
				Double settlePrice = pDepthMarketData.getSettlementPrice();

				TickField.Builder tickBuilder = TickField.newBuilder();
				tickBuilder.setUnifiedSymbol(contract.identifier().value());
				tickBuilder.setActionDay(actionDay);
				tickBuilder.setActionTime(actionTime);
				long localDateTimeMillisec = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
				tickBuilder.setActionTimestamp(localDateTimeMillisec);
				tickBuilder.setAvgPrice(isReasonable(upperLimit, lowerLimit, averagePrice) ? averagePrice : preClosePrice);

				tickBuilder.setHighPrice(isReasonable(upperLimit, lowerLimit, highPrice) ? highPrice : preClosePrice);
				tickBuilder.setLowPrice(isReasonable(upperLimit, lowerLimit, lowPrice) ? lowPrice : preClosePrice);
				tickBuilder.setOpenPrice(isReasonable(upperLimit, lowerLimit, openPrice) ? openPrice : preClosePrice);
				tickBuilder.setLastPrice(lastPrice);

				tickBuilder.setSettlePrice(isReasonable(upperLimit, lowerLimit, settlePrice) ? settlePrice : preSettlePrice);

				tickBuilder.setOpenInterest(openInterest);
				tickBuilder.setOpenInterestDelta(openInterestDelta);
				tickBuilder.setVolume(volume);
				tickBuilder.setVolumeDelta(isReasonable(volume, 0, volumeDelta) ? volumeDelta : 0);
				tickBuilder.setTurnover(turnover);
				tickBuilder.setTurnoverDelta(turnoverDelta);
				
				LocalTime time = LocalTime.from(dateTime);
				tickBuilder.setStatus(mktTimeUtil.resolveTickType(symbol, time).getCode());

				tickBuilder.setTradingDay(tradingDay);

				tickBuilder.setLowerLimit(lowerLimit);
				tickBuilder.setUpperLimit(upperLimit);

				tickBuilder.setPreClosePrice(preClosePrice);
				tickBuilder.setPreSettlePrice(preSettlePrice);
				tickBuilder.setPreOpenInterest(preOpenInterest);

				tickBuilder.addAllAskPrice(askPriceList);
				tickBuilder.addAllAskVolume(askVolumeList);
				tickBuilder.addAllBidPrice(bidPriceList);
				tickBuilder.addAllBidVolume(bidVolumeList);
				tickBuilder.setGatewayId(gatewayId);

				TickField tick = tickBuilder.build();
				
				if(logger.isTraceEnabled()) {					
					logger.trace("{}", MessagePrinter.print(tick));
				}
				if(volumeDelta > volume && logger.isWarnEnabled()) {
					logger.warn("数据有效性检测：{}", isReasonable(volume, 0, volumeDelta));
					logger.warn("异常值：{}", volumeDelta);
					logger.warn("异常Tick数据：{}", MessagePrinter.print(tick));
				}
				
				preTickMap.put(contract.identifier(), tick);

				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.TICK, tick);
				GatewayContract mktContract = (GatewayContract) gatewayAdapter.mktCenter.getContract(tick.getGatewayId(), tick.getUnifiedSymbol());
				mktContract.onTick(tick);
				lastUpdateTickTime = System.currentTimeMillis();
				
			} catch (Throwable t) {
				logger.error("{} OnRtnDepthMarketData Exception", logInfo, t);
			}

		} else {
			logger.warn("{}行情接口收到行情数据为空", logInfo);
		}
	}
	
	private boolean isReasonable(double upperLimit, double lowerLimit, double actual) {
		return upperLimit >= actual && actual >= lowerLimit;
	}

	// 订阅期权询价
	public void OnRspSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		logger.info("{}OnRspSubForQuoteRsp", logInfo);
	}

	// 退订期权询价
	public void OnRspUnSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		logger.info("{}OnRspUnSubForQuoteRsp", logInfo);
	}

	// 期权询价推送
	public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField pForQuoteRsp) {
		logger.info("{}OnRspUnSubForQuoteRsp", logInfo);
	}
	
	public boolean isActive() {
		return System.currentTimeMillis() - lastUpdateTickTime < 1000;
	}

}