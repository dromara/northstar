package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.GatewayAbstract;
import tech.quantit.northstar.gateway.api.domain.ContractFactory;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import xyz.redtorch.gateway.ctp.common.CtpContractNameResolver;
import xyz.redtorch.gateway.ctp.common.GatewayConstants;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcAccountregisterField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcBatchOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcBrokerTradingAlgosField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcBrokerTradingParamsField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcBulletinField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcCFMMCTradingAccountKeyField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcCFMMCTradingAccountTokenField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcCancelAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcChangeAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcCombActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcCombInstrumentGuardField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcContractBankField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcDepthMarketDataField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcEWarrantOffsetField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcErrorConditionalOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcExchangeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcExchangeMarginRateAdjustField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcExchangeMarginRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcExchangeRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcExecOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcExecOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcForQuoteField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcForQuoteRspField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputBatchOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputCombActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputExecOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputExecOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputForQuoteField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputOptionSelfCloseActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputOptionSelfCloseField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputQuoteActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputQuoteField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInstrumentCommissionRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInstrumentField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInstrumentMarginRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInstrumentOrderCommRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInstrumentStatusField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestUnitField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorPositionCombineDetailField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorPositionDetailField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorPositionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorProductGroupMarginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcMMInstrumentCommissionRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcMMOptionInstrCommRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcNoticeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcNotifyQueryAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOpenAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOptionInstrCommRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOptionInstrTradeCostField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOptionSelfCloseActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOptionSelfCloseField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcParkedOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcParkedOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcProductExchRateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcProductField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcProductGroupField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryInstrumentField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryInvestorField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryInvestorPositionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryTradingAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQueryCFMMCTradingAccountTokenField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQueryMaxOrderVolumeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQuoteActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQuoteField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRemoveParkedOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRemoveParkedOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqAuthenticateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqQueryAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqRepealField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqTransferField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspAuthenticateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspInfoField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspRepealField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspTransferField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSecAgentACIDMapField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSecAgentCheckModeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSettlementInfoConfirmField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSettlementInfoField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTraderApi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTraderSpi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradingAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradingAccountPasswordUpdateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradingCodeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradingNoticeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradingNoticeInfoField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTransferBankField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTransferSerialField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcUserLogoutField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcUserPasswordUpdateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.jctpv6v3v15x64apiConstants;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OptionsTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

public class TdSpi extends CThostFtdcTraderSpi {

	private static int CONNECTION_STATUS_DISCONNECTED = 0;
	private static int CONNECTION_STATUS_CONNECTED = 1;
	private static int CONNECTION_STATUS_CONNECTING = 2;
	private static int CONNECTION_STATUS_DISCONNECTING = 3;

	private static final Logger logger = LoggerFactory.getLogger(TdSpi.class);

	private GatewayAbstract gatewayAdapter;
	private String userId;
	private String password;
	private String brokerId;
	private String logInfo;
	private String gatewayId;

	private String investorName = "";

	private HashMap<String, PositionField.Builder> positionBuilderMap = new HashMap<>();

	private Map<String, String> orderIdToAdapterOrderIdMap = new ConcurrentHashMap<>(50000);
	private Map<String, String> orderIdToOrderRefMap = new ConcurrentHashMap<>(50000);
	private Map<String, String> orderIdToOriginalOrderIdMap = new HashMap<>();
	private Map<String, String> originalOrderIdToOrderIdMap = new HashMap<>();

	private Map<String, String> exchangeIdAndOrderSysIdToOrderIdMap = new ConcurrentHashMap<>(50000);

	private Map<String, SubmitOrderReqField> orderIdToSubmitOrderReqMap = new HashMap<>();
	private Map<String, OrderField> orderIdToOrderMap = new ConcurrentHashMap<>(50000);

	private Lock submitOrderLock = new ReentrantLock();

	private Thread intervalQueryThread;

	TdSpi(GatewayAbstract gatewayAdapter) {
		this.gatewayAdapter = gatewayAdapter;
		this.userId = gatewayAdapter.getGatewaySetting().getCtpApiSetting().getUserId();
		this.password = gatewayAdapter.getGatewaySetting().getCtpApiSetting().getPassword();
		this.brokerId = gatewayAdapter.getGatewaySetting().getCtpApiSetting().getBrokerId();
		this.gatewayId = gatewayAdapter.getGatewaySetting().getGatewayId();
		this.logInfo = "????????????ID-[" + this.gatewayId + "] [???] ";
		logger.info("??????TdApi????????????{}", CThostFtdcTraderApi.GetApiVersion());
	}
	
	
	private CThostFtdcTraderApi cThostFtdcTraderApi;

	private int connectionStatus = CONNECTION_STATUS_DISCONNECTED; // ??????????????????
	private boolean loginStatus = false; // ????????????
	private String tradingDay;

	private boolean instrumentQueried = false;
	private boolean investorNameQueried = false;

	private Random random = new Random();
	private AtomicInteger reqId = new AtomicInteger(random.nextInt(1800) % (1800 - 200 + 1) + 200); // ??????????????????
	private volatile int orderRef = random.nextInt(1800) % (1800 - 200 + 1) + 200; // ????????????

	private boolean loginFailed = false; // ????????????????????????????????????????????????

	private int frontId = 0; // ???????????????
	private int sessionId = 0; // ????????????

	private List<OrderField.Builder> orderBuilderCacheList = new LinkedList<>(); // ????????????????????????Order
	private List<TradeField.Builder> tradeBuilderCacheList = new LinkedList<>(); // ????????????????????????Trade

	private void startIntervalQuery() {
		if (this.intervalQueryThread != null) {
			logger.error("{}???????????????????????????,????????????", logInfo);
			stopQuery();
		}
		this.intervalQueryThread = new Thread() {
			public void run() {
				Thread.currentThread().setName("CTP Gateway Interval Query Thread, " + gatewayId + " " + System.currentTimeMillis());
				while (!Thread.currentThread().isInterrupted()) {
					try {
						if (cThostFtdcTraderApi == null) {
							logger.error("{}???????????????????????????API???????????????,??????", logInfo);
							break;
						}

						if (loginStatus) {
							queryAccount();
							Thread.sleep(1250);
							queryPosition();
							Thread.sleep(1250);
						} else {
							logger.warn("{}????????????,????????????", logInfo);
						}

					} catch (InterruptedException e) {
						logger.warn("{}??????????????????????????????????????????,????????????", logInfo, e);
						break;
					} catch (Exception e) {
						logger.error("{}??????????????????????????????", logInfo, e);
					}
				}
			};
		};
		this.intervalQueryThread.start();

	}

	private void stopQuery() {
		try {
			if (intervalQueryThread != null && !intervalQueryThread.isInterrupted()) {
				intervalQueryThread.interrupt();
				intervalQueryThread = null;
			}
		} catch (Exception e) {
			logger.error(logInfo + "????????????????????????", e);
		}
	}

	public void connect() {
		if (isConnected() || connectionStatus == CONNECTION_STATUS_CONNECTING) {
			logger.warn("{}????????????????????????????????????????????????????????????", logInfo);
			return;
		}
		
		if (connectionStatus == CONNECTION_STATUS_CONNECTED) {
			reqAuth();
			return;
		}
		
		connectionStatus = CONNECTION_STATUS_CONNECTING;
		loginStatus = false;
		instrumentQueried = false;
		investorNameQueried = false;
		gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.CONNECTING, gatewayId);

		if (cThostFtdcTraderApi != null) {
			try {
				CThostFtdcTraderApi cThostFtdcTraderApiForRelease = cThostFtdcTraderApi;
				cThostFtdcTraderApi = null;
				cThostFtdcTraderApiForRelease.RegisterSpi(null);

				new Thread() {
					public void run() {
						Thread.currentThread().setName("GatewayId " + gatewayId + " TD API Release Thread, Time " + System.currentTimeMillis());

						try {
							logger.warn("?????????????????????????????????");
							cThostFtdcTraderApiForRelease.Release();
							logger.warn("?????????????????????????????????");
						} catch (Throwable t) {
							logger.error("???????????????????????????????????????", t);
						}
					}
				}.start();

				Thread.sleep(100);
			} catch (Throwable t) {
				logger.warn("{}?????????????????????????????????", logInfo, t);
			}

		}

		logger.warn("{}???????????????????????????", logInfo);
		String envTmpDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "gateway" + File.separator + "ctp" + File.separator + "jctpv6v3v15x64api"
				+ File.separator + "CTP_FLOW_TEMP" + File.separator + "TD_" + gatewayAdapter.getGatewaySetting().getGatewayId();
		File tempFile = new File(tempFilePath);
		if (!tempFile.getParentFile().exists()) {
			try {
				FileUtils.forceMkdirParent(tempFile);
				logger.info("{}????????????????????????????????? {}", logInfo, tempFile.getParentFile().getAbsolutePath());
			} catch (IOException e) {
				logger.error("{}???????????????????????????????????????{}", logInfo, tempFile.getParentFile().getAbsolutePath(), e);
			}
		}

		logger.warn("{}?????????????????????????????????{}", logInfo, tempFile.getParentFile().getAbsolutePath());

		try {
			String tdHost = GatewayConstants.SMART_CONNECTOR.bestEndpoint(brokerId);
			String tdPort = GatewayConstants.TRADER_PORT;
			logger.info("??????IP [{}] ??????????????????", tdHost);
			cThostFtdcTraderApi = CThostFtdcTraderApi.CreateFtdcTraderApi(tempFile.getAbsolutePath());
			cThostFtdcTraderApi.RegisterSpi(this);
			cThostFtdcTraderApi.RegisterFront("tcp://" + tdHost + ":" + tdPort);
			cThostFtdcTraderApi.Init();
		} catch (Throwable t) {
			logger.error("{}????????????????????????", logInfo, t);
		}

		new Thread() {
			public void run() {
				try {
					Thread.sleep(60 * 1000);
					if (!(isConnected() && investorNameQueried && instrumentQueried)) {
						logger.error("{}????????????????????????,????????????", logInfo);
						gatewayAdapter.disconnect();
					}
				} catch (Throwable t) {
					logger.error("{}??????????????????????????????????????????", logInfo, t);
				}
			}

		}.start();

	}

	public void disconnect() {
		try {
			this.stopQuery();
			if (cThostFtdcTraderApi != null && connectionStatus != CONNECTION_STATUS_DISCONNECTING) {
				logger.warn("{}???????????????????????????????????????", logInfo);
				loginStatus = false;
				instrumentQueried = false;
				investorNameQueried = false;
				connectionStatus = CONNECTION_STATUS_DISCONNECTING;
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.DISCONNECTING, gatewayId);
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGING_OUT, gatewayId);
				try {
					if (cThostFtdcTraderApi != null) {
						CThostFtdcTraderApi cThostFtdcTraderApiForRelease = cThostFtdcTraderApi;
						cThostFtdcTraderApi = null;
						cThostFtdcTraderApiForRelease.RegisterSpi(null);

						new Thread() {
							public void run() {
								Thread.currentThread().setName("GatewayId " + gatewayId + " TD API Release Thread,Start Time " + System.currentTimeMillis());

								try {
									logger.warn("?????????????????????????????????");
									cThostFtdcTraderApiForRelease.Release();
									logger.warn("?????????????????????????????????");
								} catch (Throwable t) {
									logger.error("???????????????????????????????????????", t);
								}
							}
						}.start();

					}
					Thread.sleep(100);
				} catch (Throwable t) {
					logger.error("{}???????????????????????????????????????", logInfo, t);
				}

				connectionStatus = CONNECTION_STATUS_DISCONNECTED;
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.DISCONNECTED, gatewayId);
				logger.warn("{}???????????????????????????????????????", logInfo);
			} else {
				logger.warn("{}????????????????????????????????????????????????,????????????", logInfo);
			}
		} catch (Throwable t) {
			logger.error("{}???????????????????????????????????????", logInfo, t);
		}

	}

	public boolean isConnected() {
		return connectionStatus == CONNECTION_STATUS_CONNECTED && loginStatus;
	}

	public String getTradingDay() {
		return tradingDay;
	}

	public void queryAccount() {
		if (cThostFtdcTraderApi == null) {
			logger.warn("{}???????????????????????????,??????????????????", logInfo);
			return;
		}
		if (!loginStatus) {
			logger.warn("{}????????????????????????,??????????????????", logInfo);
			return;
		}
		if (!instrumentQueried) {
			logger.warn("{}???????????????????????????????????????,??????????????????", logInfo);
			return;
		}
		if (!investorNameQueried) {
			logger.warn("{}??????????????????????????????????????????,??????????????????", logInfo);
			return;
		}
		try {
			CThostFtdcQryTradingAccountField cThostFtdcQryTradingAccountField = new CThostFtdcQryTradingAccountField();
			cThostFtdcTraderApi.ReqQryTradingAccount(cThostFtdcQryTradingAccountField, reqId.incrementAndGet());
		} catch (Throwable t) {
			logger.error("{}??????????????????????????????", logInfo, t);
		}

	}

	public void queryPosition() {
		if (cThostFtdcTraderApi == null) {
			logger.warn("{}???????????????????????????,??????????????????", logInfo);
			return;
		}
		if (!loginStatus) {
			logger.warn("{}????????????????????????,??????????????????", logInfo);
			return;
		}

		if (!instrumentQueried) {
			logger.warn("{}???????????????????????????????????????,??????????????????", logInfo);
			return;
		}
		if (!investorNameQueried) {
			logger.warn("{}??????????????????????????????????????????,??????????????????", logInfo);
			return;
		}

		try {
			CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField = new CThostFtdcQryInvestorPositionField();
			cThostFtdcQryInvestorPositionField.setBrokerID(brokerId);
			cThostFtdcQryInvestorPositionField.setInvestorID(userId);
			cThostFtdcTraderApi.ReqQryInvestorPosition(cThostFtdcQryInvestorPositionField, reqId.incrementAndGet());
		} catch (Throwable t) {
			logger.error("{}??????????????????????????????", logInfo, t);
		}

	}

	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		if (cThostFtdcTraderApi == null) {
			logger.warn("{}???????????????????????????,????????????", logInfo);
			return null;
		}

		if (!loginStatus) {
			logger.warn("{}????????????????????????,????????????", logInfo);
			return null;
		}

		CThostFtdcInputOrderField cThostFtdcInputOrderField = new CThostFtdcInputOrderField();
		cThostFtdcInputOrderField.setInstrumentID(submitOrderReq.getContract().getSymbol());
		cThostFtdcInputOrderField.setLimitPrice(submitOrderReq.getPrice());
		cThostFtdcInputOrderField.setVolumeTotalOriginal(submitOrderReq.getVolume());
		cThostFtdcInputOrderField.setOrderPriceType(CtpConstant.orderPriceTypeMap.getOrDefault(submitOrderReq.getOrderPriceType(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setDirection(CtpConstant.directionMap.getOrDefault(submitOrderReq.getDirection(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setCombOffsetFlag(String.valueOf(CtpConstant.offsetFlagMap.getOrDefault(submitOrderReq.getOffsetFlag(), Character.valueOf('\0'))));
		cThostFtdcInputOrderField.setInvestorID(userId);
		cThostFtdcInputOrderField.setUserID(userId);
		cThostFtdcInputOrderField.setBrokerID(brokerId);
		cThostFtdcInputOrderField.setExchangeID(CtpConstant.exchangeMap.getOrDefault(submitOrderReq.getContract().getExchange(), ""));
		cThostFtdcInputOrderField.setCombHedgeFlag(CtpConstant.hedgeFlagMap.get(submitOrderReq.getHedgeFlag()));
		cThostFtdcInputOrderField.setContingentCondition(CtpConstant.contingentConditionMap.get(submitOrderReq.getContingentCondition()));
		cThostFtdcInputOrderField.setForceCloseReason(CtpConstant.forceCloseReasonMap.get(submitOrderReq.getForceCloseReason()));
		cThostFtdcInputOrderField.setIsAutoSuspend(submitOrderReq.getAutoSuspend());
		cThostFtdcInputOrderField.setIsSwapOrder(submitOrderReq.getSwapOrder());
		cThostFtdcInputOrderField.setMinVolume(submitOrderReq.getMinVolume());
		cThostFtdcInputOrderField.setTimeCondition(CtpConstant.timeConditionMap.getOrDefault(submitOrderReq.getTimeCondition(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setVolumeCondition(CtpConstant.volumeConditionMap.getOrDefault(submitOrderReq.getVolumeCondition(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setStopPrice(submitOrderReq.getStopPrice());

		// ????????????????????????,???????????????,???????????????????????????,????????????????????????
		submitOrderLock.lock();
		try {

			int orderRef = ++this.orderRef;

			String adapterOrderId = this.frontId + "_" + this.sessionId + "_" + orderRef;
			String orderId = gatewayId + "@" + adapterOrderId;

			if (StringUtils.isNotBlank(submitOrderReq.getOriginOrderId())) {
				orderIdToOriginalOrderIdMap.put(orderId, submitOrderReq.getOriginOrderId());
				originalOrderIdToOrderIdMap.put(submitOrderReq.getOriginOrderId(), orderId);
			}

			orderIdToSubmitOrderReqMap.put(orderId, submitOrderReq);
			orderIdToAdapterOrderIdMap.put(orderId, adapterOrderId);
			orderIdToOrderRefMap.put(orderId, orderRef + "");

			cThostFtdcInputOrderField.setOrderRef(orderRef + "");

			logger.info("{}???????????????????????????InstrumentID:{}, LimitPrice:{}, VolumeTotalOriginal:{}, OrderPriceType:{}, Direction:{}, CombOffsetFlag:{},\n" //
					+ "OrderRef:{}, InvestorID:{}, UserID:{}, BrokerID:{}, ExchangeID:{}, CombHedgeFlag:{}, ContingentCondition:{}, ForceCloseReason:{},\n" //
					+ "IsAutoSuspend:{}, IsSwapOrder:{}, MinVolume:{}, TimeCondition:{}, VolumeCondition:{}, StopPrice:{}", //
					logInfo, //
					cThostFtdcInputOrderField.getInstrumentID(), //
					cThostFtdcInputOrderField.getLimitPrice(), //
					cThostFtdcInputOrderField.getVolumeTotalOriginal(), //
					cThostFtdcInputOrderField.getOrderPriceType(), //
					cThostFtdcInputOrderField.getDirection(), //
					cThostFtdcInputOrderField.getCombOffsetFlag(), //
					cThostFtdcInputOrderField.getOrderRef(), //
					cThostFtdcInputOrderField.getInvestorID(), //
					cThostFtdcInputOrderField.getUserID(), //
					cThostFtdcInputOrderField.getBrokerID(), //
					cThostFtdcInputOrderField.getExchangeID(), //
					cThostFtdcInputOrderField.getCombHedgeFlag(), //
					cThostFtdcInputOrderField.getContingentCondition(), //
					cThostFtdcInputOrderField.getForceCloseReason(), //
					cThostFtdcInputOrderField.getIsAutoSuspend(), //
					cThostFtdcInputOrderField.getIsSwapOrder(), //
					cThostFtdcInputOrderField.getMinVolume(), //
					cThostFtdcInputOrderField.getTimeCondition(), //
					cThostFtdcInputOrderField.getVolumeCondition(), //
					cThostFtdcInputOrderField.getStopPrice());
			cThostFtdcTraderApi.ReqOrderInsert(cThostFtdcInputOrderField, reqId.incrementAndGet());

			return orderId;
		} catch (Throwable t) {
			logger.error("{}????????????????????????", logInfo, t);
			return null;
		} finally {
			submitOrderLock.unlock();
		}

	}

	// ??????
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {

		if (cThostFtdcTraderApi == null) {
			logger.warn("{}???????????????????????????,????????????", logInfo);
			return false;
		}

		if (!loginStatus) {
			logger.warn("{}????????????????????????,????????????", logInfo);
			return false;
		}

		if (StringUtils.isBlank(cancelOrderReq.getOrderId()) && StringUtils.isBlank(cancelOrderReq.getOriginOrderId())) {
			logger.error("{}????????????,????????????", logInfo);
			return false;
		}

		String orderId = cancelOrderReq.getOrderId();
		if (StringUtils.isBlank(orderId)) {
			orderId = originalOrderIdToOrderIdMap.get(cancelOrderReq.getOriginOrderId());
			if (StringUtils.isBlank(orderId)) {
				logger.error("{}???????????????????????????????????????,????????????", logInfo);
				return false;
			}
		}

		try {
			CThostFtdcInputOrderActionField cThostFtdcInputOrderActionField = new CThostFtdcInputOrderActionField();
			if (orderIdToSubmitOrderReqMap.containsKey(orderId)) {

				cThostFtdcInputOrderActionField.setInstrumentID(orderIdToSubmitOrderReqMap.get(orderId).getContract().getSymbol());
				cThostFtdcInputOrderActionField.setExchangeID(CtpConstant.exchangeMap.getOrDefault(orderIdToSubmitOrderReqMap.get(orderId).getContract().getExchange(), ""));
				cThostFtdcInputOrderActionField.setOrderRef(orderIdToOrderRefMap.get(orderId));
				cThostFtdcInputOrderActionField.setFrontID(frontId);
				cThostFtdcInputOrderActionField.setSessionID(sessionId);

				cThostFtdcInputOrderActionField.setActionFlag(jctpv6v3v15x64apiConstants.THOST_FTDC_AF_Delete);
				cThostFtdcInputOrderActionField.setBrokerID(brokerId);
				cThostFtdcInputOrderActionField.setInvestorID(userId);
				cThostFtdcInputOrderActionField.setUserID(userId);
				cThostFtdcInputOrderActionField.setExchangeID(CtpConstant.exchangeMap.getOrDefault(orderIdToSubmitOrderReqMap.get(orderId).getContract().getExchange(), ""));
				cThostFtdcTraderApi.ReqOrderAction(cThostFtdcInputOrderActionField, reqId.incrementAndGet());
				return true;

			} else if (orderIdToOrderMap.containsKey(orderId)) {
				cThostFtdcInputOrderActionField.setInstrumentID(orderIdToOrderMap.get(orderId).getContract().getSymbol());
				cThostFtdcInputOrderActionField.setExchangeID(CtpConstant.exchangeMap.getOrDefault(orderIdToOrderMap.get(orderId).getContract().getExchange(), ""));
				cThostFtdcInputOrderActionField.setOrderRef(orderIdToOrderRefMap.get(orderId));
				cThostFtdcInputOrderActionField.setFrontID(orderIdToOrderMap.get(orderId).getFrontId());
				cThostFtdcInputOrderActionField.setSessionID(orderIdToOrderMap.get(orderId).getSessionId());

				cThostFtdcInputOrderActionField.setActionFlag(jctpv6v3v15x64apiConstants.THOST_FTDC_AF_Delete);
				cThostFtdcInputOrderActionField.setBrokerID(brokerId);
				cThostFtdcInputOrderActionField.setInvestorID(userId);
				cThostFtdcInputOrderActionField.setUserID(userId);
				cThostFtdcInputOrderActionField.setExchangeID(CtpConstant.exchangeMap.getOrDefault(orderIdToOrderMap.get(orderId).getContract().getExchange(), ""));
				cThostFtdcTraderApi.ReqOrderAction(cThostFtdcInputOrderActionField, reqId.incrementAndGet());
				return true;
			} else {
				logger.error("{}????????????????????????????????????,????????????", logInfo);
				return false;
			}
		} catch (Throwable t) {
			logger.error("{}????????????", logInfo, t);
			return false;
		}

	}

	private void reqAuth() {
		if (loginFailed) {
			logger.warn("{}?????????????????????????????????,????????????,????????????", logInfo);
			return;
		}

		if (cThostFtdcTraderApi == null) {
			logger.warn("{}?????????????????????????????????,???????????????????????????", logInfo);
			return;
		}

		if (StringUtils.isEmpty(brokerId)) {
			logger.error("{}BrokerID???????????????", logInfo);
			return;
		}

		if (StringUtils.isEmpty(userId)) {
			logger.error("{}UserId???????????????", logInfo);
			return;
		}

		if (StringUtils.isEmpty(password)) {
			logger.error("{}Password???????????????", logInfo);
			return;
		}

		if (StringUtils.isEmpty(GatewayConstants.APP_ID)) {
			logger.error("{}AppId???????????????", logInfo);
			return;
		}
		if (StringUtils.isEmpty(GatewayConstants.AUTH_CODE)) {
			logger.error("{}AuthCode???????????????", logInfo);
			return;
		}

		try {
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGING_IN, gatewayId);
			CThostFtdcReqAuthenticateField authenticateField = new CThostFtdcReqAuthenticateField();
			authenticateField.setAppID(GatewayConstants.APP_ID);
			authenticateField.setAuthCode(GatewayConstants.AUTH_CODE);
			authenticateField.setBrokerID(brokerId);
			authenticateField.setUserProductInfo(GatewayConstants.APP_ID);
			authenticateField.setUserID(userId);
			cThostFtdcTraderApi.ReqAuthenticate(authenticateField, reqId.incrementAndGet());
		} catch (Throwable t) {
			logger.error("{}???????????????????????????", logInfo, t);
			gatewayAdapter.disconnect();
		}

	}

	// ?????????????????????
	public void OnFrontConnected() {
		try {
			logger.info("{}??????????????????????????????", logInfo);
			// ???????????????????????????
			connectionStatus = CONNECTION_STATUS_CONNECTED;
			
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.CONNECTED, gatewayId);
			
			reqAuth();
			
		} catch (Throwable t) {
			logger.error("{}OnFrontConnected Exception", logInfo, t);
		}
	}

	// ?????????????????????
	public void OnFrontDisconnected(int nReason) {
		try {
			logger.warn("{}??????????????????????????????, ??????:{}", logInfo, nReason);
			gatewayAdapter.disconnect();
			
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.DISCONNECTED, gatewayId);
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_OUT, gatewayId);
			
		} catch (Throwable t) {
			logger.error("{}OnFrontDisconnected Exception", logInfo, t);
		}
	}

	// ????????????
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo.getErrorID() == 0) {
				logger.info("{}???????????????????????? TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", logInfo, pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
						pRspUserLogin.getUserID());
				sessionId = pRspUserLogin.getSessionID();
				frontId = pRspUserLogin.getFrontID();
				// ?????????????????????true
				loginStatus = true;
				tradingDay = pRspUserLogin.getTradingDay();
				logger.info("{}????????????????????????????????????{}", logInfo, tradingDay);

				// ???????????????
				CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new CThostFtdcSettlementInfoConfirmField();
				settlementInfoConfirmField.setBrokerID(brokerId);
				settlementInfoConfirmField.setInvestorID(userId);
				cThostFtdcTraderApi.ReqSettlementInfoConfirm(settlementInfoConfirmField, reqId.incrementAndGet());

				// ??????????????????
				if (pRspInfo.getErrorID() == 3) {
					gatewayAdapter.setAuthErrorFlag(true);
					return;
				}

				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_IN, gatewayId);
			} else {
				logger.error("{}?????????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				loginFailed = true;
			}
		} catch (Throwable t) {
			logger.error("{}????????????????????????????????????", logInfo, t);
			loginFailed = true;
		}

	}

	// ????????????
	public void OnHeartBeatWarning(int nTimeLapse) {
		logger.warn("{}????????????????????????, Time Lapse:{}", logInfo, nTimeLapse);
	}

	// ????????????
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo.getErrorID() != 0) {
				logger.error("{}OnRspUserLogout!??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			} else {
				logger.info("{}OnRspUserLogout!BrokerID:{},UserId:{}", logInfo, pUserLogout.getBrokerID(), pUserLogout.getUserID());

			}
		} catch (Throwable t) {
			logger.error("{}????????????????????????????????????", logInfo, t);
		}

		loginStatus = false;
	}

	// ????????????
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			logger.error("{}????????????????????????!??????ID:{},????????????:{},??????ID:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg(), nRequestID);
			if (instrumentQueried) {
				if (pRspInfo.getErrorID() == 0) {
					NoticeField notice  = NoticeField.newBuilder()
						.setContent(logInfo + "????????????????????????:" + pRspInfo.getErrorMsg() + "?????????ID:" + pRspInfo.getErrorID())
						.setStatus(CommonStatusEnum.COMS_INFO)
						.setTimestamp(System.currentTimeMillis())
						.build();
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
				} else {
					NoticeField notice = NoticeField.newBuilder()
							.setContent(logInfo + "????????????????????????:" + pRspInfo.getErrorMsg() + "?????????ID:" + pRspInfo.getErrorID())
							.setStatus(CommonStatusEnum.COMS_ERROR)
							.setTimestamp(System.currentTimeMillis())
							.build();
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
				}
			}
			// CTP??????????????????,??????
			if (pRspInfo.getErrorID() == 90) {
				gatewayAdapter.disconnect();
			}
		} catch (Throwable t) {
			logger.error("{}OnRspError Exception", logInfo, t);
		}
	}

	// ?????????????????????
	public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo != null) {
				if (pRspInfo.getErrorID() == 0) {
					logger.info("{}{}", logInfo, "?????????????????????????????????");
					CThostFtdcReqUserLoginField reqUserLoginField = new CThostFtdcReqUserLoginField();
					reqUserLoginField.setBrokerID(brokerId);
					reqUserLoginField.setUserID(this.userId);
					reqUserLoginField.setPassword(this.password);
					cThostFtdcTraderApi.ReqUserLogin(reqUserLoginField, reqId.incrementAndGet());
					
				} else {

					logger.error("{}????????????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
					loginFailed = true;

					// ?????????????????????
					if (pRspInfo.getErrorID() == 63) {
						gatewayAdapter.setAuthErrorFlag(true);
					}
				}
			} else {
				loginFailed = true;
				logger.error("{}?????????????????????????????????????????????,??????????????????", logInfo);
			}
		} catch (Throwable t) {
			loginFailed = true;
			logger.error("{}?????????????????????????????????????????????", logInfo, t);
		}
	}

	public void OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField pUserPasswordUpdate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspTradingAccountPasswordUpdate(CThostFtdcTradingAccountPasswordUpdateField pTradingAccountPasswordUpdate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// ????????????
	public void OnRspOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pInputOrder != null) {

				String symbol = pInputOrder.getInstrumentID();

				// ????????????????????????,??????userId????????????ID
				String accountCode = userId;
				// ???????????????????????????????????????CNY
				String accountId = accountCode + "@" + gatewayId;

				int frontId = this.frontId;
				int sessionId = this.sessionId;
				String orderRef = StringUtils.trim(pInputOrder.getOrderRef());

				String adapterOrderId = frontId + "_" + sessionId + "_" + orderRef;
				String orderId = gatewayId + "@" + adapterOrderId;

				DirectionEnum direction = CtpConstant.directionMapReverse.getOrDefault(pInputOrder.getDirection(), DirectionEnum.D_Unknown);
				OffsetFlagEnum offsetflag = CtpConstant.offsetMapReverse.getOrDefault(pInputOrder.getCombOffsetFlag().toCharArray()[0], OffsetFlagEnum.OF_Unknown);

				double price = pInputOrder.getLimitPrice();
				int totalVolume = pInputOrder.getVolumeTotalOriginal();
				int tradedVolume = 0;

				OrderStatusEnum orderStatus = OrderStatusEnum.OS_Rejected;

				HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(pInputOrder.getCombHedgeFlag(), HedgeFlagEnum.HF_Unknown);
				ContingentConditionEnum contingentCondition = CtpConstant.contingentConditionMapReverse.getOrDefault(pInputOrder.getContingentCondition(), ContingentConditionEnum.CC_Unknown);
				ForceCloseReasonEnum forceCloseReason = CtpConstant.forceCloseReasonMapReverse.getOrDefault(pInputOrder.getForceCloseReason(), ForceCloseReasonEnum.FCR_Unknown);
				TimeConditionEnum timeCondition = CtpConstant.timeConditionMapReverse.getOrDefault(pInputOrder.getTimeCondition(), TimeConditionEnum.TC_Unknown);
				String gtdDate = pInputOrder.getGTDDate();
				int autoSuspend = pInputOrder.getIsAutoSuspend();
				int userForceClose = pInputOrder.getUserForceClose();
				int swapOrder = pInputOrder.getIsSwapOrder();
				VolumeConditionEnum volumeCondition = CtpConstant.volumeConditionMapReverse.getOrDefault(pInputOrder.getVolumeCondition(), VolumeConditionEnum.VC_Unknown);
				OrderPriceTypeEnum orderPriceType = CtpConstant.orderPriceTypeMapReverse.getOrDefault(pInputOrder.getOrderPriceType(), OrderPriceTypeEnum.OPT_Unknown);

				int minVolume = pInputOrder.getMinVolume();
				double stopPrice = pInputOrder.getStopPrice();

				String originalOrderId = orderIdToOriginalOrderIdMap.getOrDefault(orderId, "");

				OrderField.Builder orderBuilder = OrderField.newBuilder();
				orderBuilder.setAccountId(accountId);
				orderBuilder.setOriginOrderId(originalOrderId);
				orderBuilder.setOrderId(orderId);
				orderBuilder.setAdapterOrderId(adapterOrderId);
				orderBuilder.setDirection(direction);
				orderBuilder.setOffsetFlag(offsetflag);
				orderBuilder.setPrice(price);
				orderBuilder.setTotalVolume(totalVolume);
				orderBuilder.setTradedVolume(tradedVolume);
				orderBuilder.setOrderStatus(orderStatus);
				orderBuilder.setTradingDay(tradingDay);
				orderBuilder.setFrontId(frontId);
				orderBuilder.setSessionId(sessionId);
				orderBuilder.setGatewayId(gatewayId);
				orderBuilder.setHedgeFlag(hedgeFlag);
				orderBuilder.setContingentCondition(contingentCondition);
				orderBuilder.setForceCloseReason(forceCloseReason);
				orderBuilder.setTimeCondition(timeCondition);
				orderBuilder.setGtdDate(gtdDate);
				orderBuilder.setAutoSuspend(autoSuspend);
				orderBuilder.setVolumeCondition(volumeCondition);
				orderBuilder.setMinVolume(minVolume);
				orderBuilder.setStopPrice(stopPrice);
				orderBuilder.setUserForceClose(userForceClose);
				orderBuilder.setSwapOrder(swapOrder);
				orderBuilder.setOrderPriceType(orderPriceType);

				if (pRspInfo != null && pRspInfo.getErrorMsg() != null) {
					orderBuilder.setStatusMsg(pRspInfo.getErrorMsg());
				}

				if (instrumentQueried && gatewayAdapter.contractMap.containsKey(symbol)) {
					orderBuilder.setContract(gatewayAdapter.contractMap.get(symbol));
					OrderField order = orderBuilder.build();
					orderIdToOrderMap.put(order.getOrderId(), order);
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ORDER, order);
				} else {
					ContractField.Builder contractBuilder = ContractField.newBuilder();
					contractBuilder.setSymbol(symbol);
					orderBuilder.setContract(gatewayAdapter.contractMap.get(symbol));

					orderBuilderCacheList.add(orderBuilder);
				}
			} else {
				logger.error("{}????????????????????????????????????(OnRspOrderInsert)??????,?????????", logInfo);
			}

			if (pRspInfo != null) {
				logger.error("{}??????????????????????????????(OnRspOrderInsert) ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				if (instrumentQueried) {
					NoticeField notice  = NoticeField.newBuilder()
						.setContent(logInfo + "???????????????????????????????????????ID:" + pRspInfo.getErrorID() + "???????????????:" + pRspInfo.getErrorMsg())
						.setStatus(CommonStatusEnum.COMS_ERROR)
						.setTimestamp(System.currentTimeMillis())
						.build();
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
				}
			} else {
				logger.error("{}????????????????????????????????????(OnRspOrderInsert)??????,??????????????????", logInfo);
			}

		} catch (Throwable t) {
			logger.error("{}????????????????????????????????????(OnRspOrderInsert)??????", logInfo, t);
		}

	}

	public void OnRspParkedOrderInsert(CThostFtdcParkedOrderField pParkedOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspParkedOrderAction(CThostFtdcParkedOrderActionField pParkedOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// ??????????????????
	public void OnRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			logger.error("{}??????????????????????????????(OnRspOrderAction) ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			if (instrumentQueried) {
				NoticeField notice  = NoticeField.newBuilder()
					.setContent(logInfo + "???????????????????????????????????????ID:" + pRspInfo.getErrorID() + "???????????????:" + pRspInfo.getErrorMsg())
					.setStatus(CommonStatusEnum.COMS_ERROR)
					.setTimestamp(System.currentTimeMillis())
					.build();
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
			}
		} else {
			logger.error("{}????????????????????????????????????(OnRspOrderAction)??????,???????????????", logInfo);
		}
	}

	public void OnRspQueryMaxOrderVolume(CThostFtdcQueryMaxOrderVolumeField pQueryMaxOrderVolume, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// ????????????????????????
	public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if(pRspInfo == null) {
				logger.warn("????????????????????????");
			} else if (pRspInfo.getErrorID() == 0) {
				logger.info("{}????????????????????????????????????", logInfo);
			} else {
				logger.error("{}???????????????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				gatewayAdapter.disconnect();
				return;
			}

			// ???????????????
			Thread.sleep(1000);

			logger.info("{}???????????????????????????????????????", logInfo);
			CThostFtdcQryInvestorField pQryInvestor = new CThostFtdcQryInvestorField();
			pQryInvestor.setInvestorID(userId);
			pQryInvestor.setBrokerID(brokerId);
			cThostFtdcTraderApi.ReqQryInvestor(pQryInvestor, reqId.addAndGet(1));
		} catch (Throwable t) {
			logger.error("{}?????????????????????????????????", logInfo, t);
//			gatewayAdapter.disconnect();
		}
	}

	public void OnRspRemoveParkedOrder(CThostFtdcRemoveParkedOrderField pRemoveParkedOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspRemoveParkedOrderAction(CThostFtdcRemoveParkedOrderActionField pRemoveParkedOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspExecOrderInsert(CThostFtdcInputExecOrderField pInputExecOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspExecOrderAction(CThostFtdcInputExecOrderActionField pInputExecOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspForQuoteInsert(CThostFtdcInputForQuoteField pInputForQuote, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQuoteInsert(CThostFtdcInputQuoteField pInputQuote, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQuoteAction(CThostFtdcInputQuoteActionField pInputQuoteAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspBatchOrderAction(CThostFtdcInputBatchOrderActionField pInputBatchOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspOptionSelfCloseInsert(CThostFtdcInputOptionSelfCloseField pInputOptionSelfClose, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspOptionSelfCloseAction(CThostFtdcInputOptionSelfCloseActionField pInputOptionSelfCloseAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspCombActionInsert(CThostFtdcInputCombActionField pInputCombAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryOrder(CThostFtdcOrderField pOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryTrade(CThostFtdcTradeField pTrade, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// ??????????????????
	public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

		try {
			if (pInvestorPosition == null || StringUtils.isEmpty(pInvestorPosition.getInstrumentID())) {
				return;
			}
			String symbol = pInvestorPosition.getInstrumentID();

			if (!(instrumentQueried && gatewayAdapter.contractMap.containsKey(symbol))) {
				logger.warn("{}???????????????????????????,???????????????????????????,??????{}", logInfo, symbol);
				return;
			}

			ContractField contract = gatewayAdapter.contractMap.get(symbol);

			String uniqueSymbol = symbol + "@" + contract.getExchange().getValueDescriptor().getName() + "@" + contract.getProductClass().getValueDescriptor().getName();

			// ????????????????????????,??????userId????????????ID
			String accountCode = userId;
			// ???????????????????????????????????????
			String accountId = accountCode + "@" + gatewayId;

			PositionDirectionEnum direction = CtpConstant.posiDirectionMapReverse.getOrDefault(pInvestorPosition.getPosiDirection(), PositionDirectionEnum.PD_Unknown);
			HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.get(String.valueOf(pInvestorPosition.getHedgeFlag()));
			// ??????????????????
			String positionId = uniqueSymbol + "@" + direction.getValueDescriptor().getName() + "@" + hedgeFlag.getValueDescriptor().getName() + "@" + accountId;

			PositionField.Builder positionBuilder;
			if (positionBuilderMap.containsKey(positionId)) {
				positionBuilder = positionBuilderMap.get(positionId);
			} else {
				positionBuilder = PositionField.newBuilder();
				positionBuilderMap.put(positionId, positionBuilder);
				positionBuilder.setContract(gatewayAdapter.contractMap.get(symbol));
				positionBuilder.setPositionDirection(CtpConstant.posiDirectionMapReverse.getOrDefault(pInvestorPosition.getPosiDirection(), PositionDirectionEnum.PD_Unknown));
				positionBuilder.setPositionId(positionId);

				positionBuilder.setAccountId(accountId);
				positionBuilder.setGatewayId(gatewayId);
				positionBuilder.setHedgeFlag(hedgeFlag);

			}

			positionBuilder.setUseMargin(positionBuilder.getUseMargin() + pInvestorPosition.getUseMargin());
			positionBuilder.setExchangeMargin(positionBuilder.getExchangeMargin() + pInvestorPosition.getExchangeMargin());

			positionBuilder.setPosition(positionBuilder.getPosition() + pInvestorPosition.getPosition());

			if (positionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Long) {
				positionBuilder.setFrozen(pInvestorPosition.getShortFrozen());
			} else {
				positionBuilder.setFrozen(pInvestorPosition.getLongFrozen());
			}

			if (ExchangeEnum.INE == positionBuilder.getContract().getExchange() || ExchangeEnum.SHFE == positionBuilder.getContract().getExchange()) {
				// ????????????????????????????????????????????????????????????????????????????????????,??????????????????
				if (pInvestorPosition.getYdPosition() > 0 && pInvestorPosition.getTodayPosition() == 0) {

					positionBuilder.setYdPosition(positionBuilder.getYdPosition() + pInvestorPosition.getPosition());

					if (positionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Long) {
						positionBuilder.setYdFrozen(positionBuilder.getYdFrozen() + pInvestorPosition.getShortFrozen());
					} else {
						positionBuilder.setYdFrozen(positionBuilder.getYdFrozen() + pInvestorPosition.getLongFrozen());
					}
				} else {
					positionBuilder.setTdPosition(positionBuilder.getTdPosition() + pInvestorPosition.getPosition());

					if (positionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Long) {
						positionBuilder.setTdFrozen(positionBuilder.getTdFrozen() + pInvestorPosition.getShortFrozen());
					} else {
						positionBuilder.setTdFrozen(positionBuilder.getTdFrozen() + pInvestorPosition.getLongFrozen());
					}
				}
			} else {
				positionBuilder.setTdPosition(positionBuilder.getTdPosition() + pInvestorPosition.getTodayPosition());
				positionBuilder.setYdPosition(positionBuilder.getPosition() - positionBuilder.getTdPosition());

				// ?????????????????????
				if (ExchangeEnum.CFFEX == positionBuilder.getContract().getExchange()) {
					if (positionBuilder.getTdPosition() > 0) {
						if (positionBuilder.getTdPosition() >= positionBuilder.getFrozen()) {
							positionBuilder.setTdFrozen(positionBuilder.getFrozen());
						} else {
							positionBuilder.setTdFrozen(positionBuilder.getTdPosition());
							positionBuilder.setYdFrozen(positionBuilder.getFrozen() - positionBuilder.getTdPosition());
						}
					} else {
						positionBuilder.setYdFrozen(positionBuilder.getFrozen());
					}
				} else {
					// ????????????????????????????????????????????????????????????
					if (positionBuilder.getYdPosition() > 0) {
						if (positionBuilder.getYdPosition() >= positionBuilder.getFrozen()) {
							positionBuilder.setYdFrozen(positionBuilder.getFrozen());
						} else {
							positionBuilder.setYdFrozen(positionBuilder.getYdPosition());
							positionBuilder.setTdFrozen(positionBuilder.getFrozen() - positionBuilder.getYdPosition());
						}
					} else {
						positionBuilder.setTdFrozen(positionBuilder.getFrozen());
					}
				}

			}

			// ????????????
			double cost = positionBuilder.getPrice() * positionBuilder.getPosition() * positionBuilder.getContract().getMultiplier();
			double openCost = positionBuilder.getOpenPrice() * positionBuilder.getPosition() * positionBuilder.getContract().getMultiplier();

			// ????????????
			positionBuilder.setPositionProfit(positionBuilder.getPositionProfit() + pInvestorPosition.getPositionProfit());

			// ??????????????????
			if (positionBuilder.getPosition() != 0) {
				positionBuilder.setPrice((cost + pInvestorPosition.getPositionCost()) / (positionBuilder.getPosition() * positionBuilder.getContract().getMultiplier()));
				positionBuilder.setOpenPrice((openCost + pInvestorPosition.getOpenCost()) / (positionBuilder.getPosition() * positionBuilder.getContract().getMultiplier()));
			}

			// ????????????
			if (bIsLast) {
				for (PositionField.Builder tmpPositionBuilder : positionBuilderMap.values()) {

					if (tmpPositionBuilder.getPosition() != 0) {

						tmpPositionBuilder.setPriceDiff(tmpPositionBuilder.getPositionProfit() / tmpPositionBuilder.getContract().getMultiplier() / tmpPositionBuilder.getPosition());

						if (tmpPositionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Long
								|| (tmpPositionBuilder.getPosition() > 0 && tmpPositionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Net)) {

							// ??????????????????
							tmpPositionBuilder.setLastPrice(tmpPositionBuilder.getPrice() + tmpPositionBuilder.getPriceDiff());
							// ????????????????????????
							tmpPositionBuilder.setOpenPriceDiff(tmpPositionBuilder.getLastPrice() - tmpPositionBuilder.getOpenPrice());
							// ??????????????????
							tmpPositionBuilder.setOpenPositionProfit(tmpPositionBuilder.getOpenPriceDiff() * tmpPositionBuilder.getPosition() * tmpPositionBuilder.getContract().getMultiplier());

						} else if (tmpPositionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Short
								|| (tmpPositionBuilder.getPosition() < 0 && tmpPositionBuilder.getPositionDirection() == PositionDirectionEnum.PD_Net)) {

							// ??????????????????
							tmpPositionBuilder.setLastPrice(tmpPositionBuilder.getPrice() - tmpPositionBuilder.getPriceDiff());
							// ????????????????????????
							tmpPositionBuilder.setOpenPriceDiff(tmpPositionBuilder.getOpenPrice() - tmpPositionBuilder.getLastPrice());
							// ??????????????????
							tmpPositionBuilder.setOpenPositionProfit(tmpPositionBuilder.getOpenPriceDiff() * tmpPositionBuilder.getPosition() * tmpPositionBuilder.getContract().getMultiplier());

						} else {
							logger.error("{}???????????????????????????????????????????????????{}", logInfo, tmpPositionBuilder.toString());
						}

						// ???????????????????????????
						tmpPositionBuilder.setContractValue(tmpPositionBuilder.getLastPrice() * tmpPositionBuilder.getContract().getMultiplier() * tmpPositionBuilder.getPosition());

						if (tmpPositionBuilder.getUseMargin() != 0) {
							tmpPositionBuilder.setPositionProfitRatio(tmpPositionBuilder.getPositionProfit() / tmpPositionBuilder.getUseMargin());
							tmpPositionBuilder.setOpenPositionProfitRatio(tmpPositionBuilder.getOpenPositionProfit() / tmpPositionBuilder.getUseMargin());

						}
					}
					// ??????????????????
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.POSITION, tmpPositionBuilder.build());
				}
				// ????????????
				positionBuilderMap = new HashMap<>();
			}

		} catch (Throwable t) {
			logger.error("{}??????????????????????????????", logInfo, t);
			gatewayAdapter.disconnect();
		}
	}

	// ??????????????????
	public void OnRspQryTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {

		try {
			String accountCode = pTradingAccount.getAccountID();
			String currency = pTradingAccount.getCurrencyID();

			if (StringUtils.isBlank(currency)) {
				currency = "CNY";
			}

			String accountId = accountCode + "@" + gatewayId;

			AccountField.Builder accountBuilder = AccountField.newBuilder();
			accountBuilder.setCode(accountCode);
			accountBuilder.setCurrency(CurrencyEnum.valueOf(currency));
			accountBuilder.setAvailable(pTradingAccount.getAvailable());
			accountBuilder.setCloseProfit(pTradingAccount.getCloseProfit());
			accountBuilder.setCommission(pTradingAccount.getCommission());
			accountBuilder.setGatewayId(gatewayId);
			accountBuilder.setMargin(pTradingAccount.getCurrMargin());
			accountBuilder.setPositionProfit(pTradingAccount.getPositionProfit());
			accountBuilder.setPreBalance(pTradingAccount.getPreBalance());
			accountBuilder.setAccountId(accountId);
			accountBuilder.setDeposit(pTradingAccount.getDeposit());
			accountBuilder.setWithdraw(pTradingAccount.getWithdraw());
			accountBuilder.setHolder(investorName);

			accountBuilder.setBalance(pTradingAccount.getBalance());

			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ACCOUNT, accountBuilder.build());
		} catch (Throwable t) {
			logger.error("{}??????????????????????????????", logInfo, t);
			gatewayAdapter.disconnect();
		}

	}

	public void OnRspQryInvestor(CThostFtdcInvestorField pInvestor, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
				logger.error("{}??????????????????????????? ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				gatewayAdapter.disconnect();
			} else {
				if (pInvestor != null) {
					investorName = pInvestor.getInvestorName();
					logger.info("{}???????????????????????????????????????:{}", logInfo, investorName);
				} else {
					logger.error("{}???????????????????????????????????????", logInfo);
				}
			}

			if (bIsLast) {
				if (StringUtils.isBlank(investorName)) {
					logger.warn("{}???????????????????????????????????????", logInfo);
					NoticeField notice = NoticeField.newBuilder()
							.setContent(logInfo + "??????????????????????????????")
							.setStatus(CommonStatusEnum.COMS_WARN)
							.setTimestamp(System.currentTimeMillis())
							.build();
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
				}
				investorNameQueried = true;
				// ???????????????
				Thread.sleep(1000);
				// ??????????????????
				logger.info("{}????????????????????????????????????", logInfo);
				CThostFtdcQryInstrumentField cThostFtdcQryInstrumentField = new CThostFtdcQryInstrumentField();
				cThostFtdcTraderApi.ReqQryInstrument(cThostFtdcQryInstrumentField, reqId.incrementAndGet());
			}
		} catch (Throwable t) {
			logger.error("{}?????????????????????????????????", logInfo, t);
			gatewayAdapter.disconnect();
		}
	}

	public void OnRspQryTradingCode(CThostFtdcTradingCodeField pTradingCode, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInstrumentMarginRate(CThostFtdcInstrumentMarginRateField pInstrumentMarginRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField pInstrumentCommissionRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryExchange(CThostFtdcExchangeField pExchange, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryProduct(CThostFtdcProductField pProduct, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// ??????????????????
	public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			String symbol = pInstrument.getInstrumentID();
			String name = CtpContractNameResolver.getCNSymbolName(symbol);
			ContractField.Builder contractBuilder = ContractField.newBuilder();
			contractBuilder.setGatewayId(GatewayType.CTP.toString());
			contractBuilder.setSymbol(symbol);
			contractBuilder.setExchange(CtpConstant.exchangeMapReverse.getOrDefault(pInstrument.getExchangeID(), ExchangeEnum.UnknownExchange));
			contractBuilder.setProductClass(CtpConstant.productTypeMapReverse.getOrDefault(pInstrument.getProductClass(), ProductClassEnum.UnknownProductClass));
			contractBuilder.setUnifiedSymbol(contractBuilder.getSymbol() + "@" + contractBuilder.getExchange() + "@" + contractBuilder.getProductClass());
			contractBuilder.setContractId(contractBuilder.getUnifiedSymbol() + "@" + gatewayId);
			contractBuilder.setName(name != null ? name : pInstrument.getInstrumentName());
			contractBuilder.setFullName(pInstrument.getInstrumentName());
			contractBuilder.setThirdPartyId(contractBuilder.getSymbol() + "@" + GatewayType.CTP);

			if (pInstrument.getVolumeMultiple() <= 0) {
				contractBuilder.setMultiplier(1);
			} else {
				contractBuilder.setMultiplier(pInstrument.getVolumeMultiple());
			}

			contractBuilder.setPriceTick(pInstrument.getPriceTick());
			contractBuilder.setCurrency(CurrencyEnum.CNY); // ???????????????
			contractBuilder.setLastTradeDateOrContractMonth(pInstrument.getExpireDate());
			contractBuilder.setStrikePrice(pInstrument.getStrikePrice());
			contractBuilder.setOptionsType(CtpConstant.optionTypeMapReverse.getOrDefault(pInstrument.getOptionsType(), OptionsTypeEnum.O_Unknown));

			if (pInstrument.getUnderlyingInstrID() != null) {
				contractBuilder.setUnderlyingSymbol(pInstrument.getUnderlyingInstrID());
			}

			contractBuilder.setUnderlyingMultiplier(pInstrument.getUnderlyingMultiple());
			contractBuilder.setMaxLimitOrderVolume(pInstrument.getMaxLimitOrderVolume());
			contractBuilder.setMaxMarketOrderVolume(pInstrument.getMaxMarketOrderVolume());
			contractBuilder.setMinLimitOrderVolume(pInstrument.getMinLimitOrderVolume());
			contractBuilder.setMinMarketOrderVolume(pInstrument.getMinMarketOrderVolume());
			contractBuilder.setMaxMarginSideAlgorithm(pInstrument.getMaxMarginSideAlgorithm() == '1');
			contractBuilder.setLongMarginRatio(pInstrument.getLongMarginRatio());
			contractBuilder.setShortMarginRatio(pInstrument.getShortMarginRatio());

			ContractField contract = contractBuilder.build();
			gatewayAdapter.contractMap.put(contractBuilder.getSymbol(), contract);
			gatewayAdapter.registry.register(new NormalContract(contract, System.currentTimeMillis()));
			if (bIsLast) {
				
				logger.info("{}????????????????????????????????????!??????{}???", logInfo, gatewayAdapter.contractMap.size());
				ContractFactory contractFactory = new ContractFactory(gatewayAdapter.contractMap.values().stream().toList());
				contractFactory.makeIndexContract().stream().forEach(gatewayAdapter.registry::register);
				
				instrumentQueried = true;
				this.startIntervalQuery();

				logger.info("{}??????????????????????????????Order,??????{}???", logInfo, orderBuilderCacheList.size());
				for (OrderField.Builder orderBuilder : orderBuilderCacheList) {
					if (gatewayAdapter.contractMap.containsKey(orderBuilder.getContract().getSymbol())) {
						orderBuilder.setContract(gatewayAdapter.contractMap.get(orderBuilder.getContract().getSymbol()));
						OrderField order = orderBuilder.build();
						orderIdToOrderMap.put(order.getOrderId(), order);
						gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ORDER, order);
					} else {
						logger.error("{}??????????????????????????????????????????{}", logInfo, orderBuilder.getContract().getSymbol());
					}
				}
				orderBuilderCacheList.clear();

				logger.info("{}??????????????????????????????Trade,??????{}???", logInfo, tradeBuilderCacheList.size());
				for (TradeField.Builder tradeBuilder : tradeBuilderCacheList) {
					if (gatewayAdapter.contractMap.containsKey(tradeBuilder.getContract().getSymbol())) {
						tradeBuilder.setContract(gatewayAdapter.contractMap.get(tradeBuilder.getContract().getSymbol()));
						gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.TRADE, tradeBuilder.build());
					} else {
						logger.error("{}??????????????????????????????????????????{}", logInfo, tradeBuilder.getContract().getSymbol());
					}
				}
				tradeBuilderCacheList.clear();
				
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.GATEWAY_READY, gatewayId);
			}
		} catch (Throwable t) {
			logger.error("{}OnRspQryInstrument Exception", logInfo, t);
		}

	}
	
	public void OnRspQryDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQrySettlementInfo(CThostFtdcSettlementInfoField pSettlementInfo, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryTransferBank(CThostFtdcTransferBankField pTransferBank, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInvestorPositionDetail(CThostFtdcInvestorPositionDetailField pInvestorPositionDetail, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryNotice(CThostFtdcNoticeField pNotice, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQrySettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInvestorPositionCombineDetail(CThostFtdcInvestorPositionCombineDetailField pInvestorPositionCombineDetail, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryCFMMCTradingAccountKey(CThostFtdcCFMMCTradingAccountKeyField pCFMMCTradingAccountKey, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryEWarrantOffset(CThostFtdcEWarrantOffsetField pEWarrantOffset, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInvestorProductGroupMargin(CThostFtdcInvestorProductGroupMarginField pInvestorProductGroupMargin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryExchangeMarginRate(CThostFtdcExchangeMarginRateField pExchangeMarginRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryExchangeMarginRateAdjust(CThostFtdcExchangeMarginRateAdjustField pExchangeMarginRateAdjust, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryExchangeRate(CThostFtdcExchangeRateField pExchangeRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQrySecAgentACIDMap(CThostFtdcSecAgentACIDMapField pSecAgentACIDMap, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryProductExchRate(CThostFtdcProductExchRateField pProductExchRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryProductGroup(CThostFtdcProductGroupField pProductGroup, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryMMInstrumentCommissionRate(CThostFtdcMMInstrumentCommissionRateField pMMInstrumentCommissionRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryMMOptionInstrCommRate(CThostFtdcMMOptionInstrCommRateField pMMOptionInstrCommRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInstrumentOrderCommRate(CThostFtdcInstrumentOrderCommRateField pInstrumentOrderCommRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQrySecAgentTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQrySecAgentCheckMode(CThostFtdcSecAgentCheckModeField pSecAgentCheckMode, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryOptionInstrTradeCost(CThostFtdcOptionInstrTradeCostField pOptionInstrTradeCost, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryOptionInstrCommRate(CThostFtdcOptionInstrCommRateField pOptionInstrCommRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryExecOrder(CThostFtdcExecOrderField pExecOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryForQuote(CThostFtdcForQuoteField pForQuote, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryQuote(CThostFtdcQuoteField pQuote, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryOptionSelfClose(CThostFtdcOptionSelfCloseField pOptionSelfClose, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryInvestUnit(CThostFtdcInvestUnitField pInvestUnit, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryCombInstrumentGuard(CThostFtdcCombInstrumentGuardField pCombInstrumentGuard, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryCombAction(CThostFtdcCombActionField pCombAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryTransferSerial(CThostFtdcTransferSerialField pTransferSerial, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryAccountregister(CThostFtdcAccountregisterField pAccountregister, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	// ????????????
	public void OnRtnOrder(CThostFtdcOrderField pOrder) {
		try {
			String symbol = pOrder.getInstrumentID();

			// ????????????????????????,??????userId????????????ID
			String accountCode = userId;
			// ???????????????????????????????????????CNY
			String accountId = accountCode + "@" + gatewayId;

			int frontId = pOrder.getFrontID();
			int sessionId = pOrder.getSessionID();
			String orderRef = StringUtils.trim(pOrder.getOrderRef());

			String adapterOrderId = frontId + "_" + sessionId + "_" + orderRef;
			String orderId = gatewayId + "@" + adapterOrderId;

			String exchangeAndOrderSysId = pOrder.getExchangeID() + "@" + pOrder.getOrderSysID();

			exchangeIdAndOrderSysIdToOrderIdMap.put(exchangeAndOrderSysId, orderId);
			orderIdToOrderRefMap.put(orderId, orderRef);
			orderIdToAdapterOrderIdMap.put(orderId, adapterOrderId);

			DirectionEnum direction = CtpConstant.directionMapReverse.getOrDefault(pOrder.getDirection(), DirectionEnum.D_Unknown);
			OffsetFlagEnum offsetFlag = CtpConstant.offsetMapReverse.getOrDefault(pOrder.getCombOffsetFlag().toCharArray()[0], OffsetFlagEnum.OF_Unknown);

			double price = pOrder.getLimitPrice();

			int totalVolume = pOrder.getVolumeTotalOriginal();
			int tradedVolume = pOrder.getVolumeTraded();

			OrderStatusEnum orderStatus = CtpConstant.statusMapReverse.get(pOrder.getOrderStatus());
			String statusMsg = pOrder.getStatusMsg();

			String orderDate = pOrder.getInsertDate();
			String orderTime = pOrder.getInsertTime();
			LocalDateTime tradeDatetime = LocalDateTime.of(LocalDate.from(DateTimeConstant.D_FORMAT_INT_FORMATTER.parse(orderDate)), LocalTime.from(DateTimeConstant.T_FORMAT_FORMATTER.parse(orderTime)));
			long tradeTimestamp = tradeDatetime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
			String activeTime = String.valueOf(tradeTimestamp);
			String cancelTime = pOrder.getCancelTime();
			String updateTime = pOrder.getUpdateTime();
			String suspendTime = pOrder.getSuspendTime();

			HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(pOrder.getCombHedgeFlag(), HedgeFlagEnum.HF_Unknown);
			ContingentConditionEnum contingentCondition = CtpConstant.contingentConditionMapReverse.getOrDefault(pOrder.getContingentCondition(), ContingentConditionEnum.CC_Unknown);
			ForceCloseReasonEnum forceCloseReason = CtpConstant.forceCloseReasonMapReverse.getOrDefault(pOrder.getForceCloseReason(), ForceCloseReasonEnum.FCR_Unknown);
			TimeConditionEnum timeCondition = CtpConstant.timeConditionMapReverse.getOrDefault(pOrder.getTimeCondition(), TimeConditionEnum.TC_Unknown);

			int userForceClose = pOrder.getUserForceClose();
			String gtdDate = pOrder.getGTDDate();
			int autoSuspend = pOrder.getIsAutoSuspend();
			int swapOrder = pOrder.getIsSwapOrder();

			VolumeConditionEnum volumeCondition = CtpConstant.volumeConditionMapReverse.getOrDefault(pOrder.getVolumeCondition(), VolumeConditionEnum.VC_Unknown);
			OrderPriceTypeEnum orderPriceType = CtpConstant.orderPriceTypeMapReverse.getOrDefault(pOrder.getOrderPriceType(), OrderPriceTypeEnum.OPT_Unknown);

			int minVolume = pOrder.getMinVolume();
			double stopPrice = pOrder.getStopPrice();

			String orderLocalId = StringUtils.trim(pOrder.getOrderLocalID());
			String orderSysId = StringUtils.trim(pOrder.getOrderSysID());
			String sequenceNo = pOrder.getSequenceNo() + "";
			String brokerOrderSeq = pOrder.getBrokerOrderSeq() + "";

			String originalOrderId = orderIdToOriginalOrderIdMap.getOrDefault(orderId, "");

			OrderField.Builder orderBuilder = OrderField.newBuilder();
			orderBuilder.setAccountId(accountId);
			orderBuilder.setActiveTime(activeTime);
			orderBuilder.setAdapterOrderId(adapterOrderId);
			orderBuilder.setCancelTime(cancelTime);
			orderBuilder.setDirection(direction);
			orderBuilder.setFrontId(frontId);
			orderBuilder.setOffsetFlag(offsetFlag);
			orderBuilder.setOrderDate(orderDate);
			orderBuilder.setOrderId(orderId);
			orderBuilder.setOrderStatus(orderStatus);
			orderBuilder.setOrderTime(orderTime);
			orderBuilder.setOriginOrderId(originalOrderId);
			orderBuilder.setPrice(price);
			orderBuilder.setSessionId(sessionId);
			orderBuilder.setTotalVolume(totalVolume);
			orderBuilder.setTradedVolume(tradedVolume);
			orderBuilder.setTradingDay(tradingDay);
			orderBuilder.setUpdateTime(updateTime);
			orderBuilder.setStatusMsg(statusMsg);
			orderBuilder.setGatewayId(gatewayId);
			orderBuilder.setHedgeFlag(hedgeFlag);
			orderBuilder.setContingentCondition(contingentCondition);
			orderBuilder.setForceCloseReason(forceCloseReason);
			orderBuilder.setTimeCondition(timeCondition);
			orderBuilder.setGtdDate(gtdDate);
			orderBuilder.setAutoSuspend(autoSuspend);
			orderBuilder.setVolumeCondition(volumeCondition);
			orderBuilder.setMinVolume(minVolume);
			orderBuilder.setStopPrice(stopPrice);
			orderBuilder.setUserForceClose(userForceClose);
			orderBuilder.setSwapOrder(swapOrder);
			orderBuilder.setSuspendTime(suspendTime);
			orderBuilder.setOrderLocalId(orderLocalId);
			orderBuilder.setOrderSysId(orderSysId);
			orderBuilder.setSequenceNo(sequenceNo);
			orderBuilder.setBrokerOrderSeq(brokerOrderSeq);
			orderBuilder.setOrderPriceType(orderPriceType);

			if (instrumentQueried) {
				if (gatewayAdapter.contractMap.containsKey(symbol)) {
					orderBuilder.setContract(gatewayAdapter.contractMap.get(symbol));
					OrderField order = orderBuilder.build();
					orderIdToOrderMap.put(order.getOrderId(), order);
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ORDER, order);
				} else {
					logger.error("{}????????????????????????????????????,???????????????:{}", logInfo, symbol);
				}
			} else {
				ContractField.Builder contractBuilder = ContractField.newBuilder();
				contractBuilder.setSymbol(symbol);
				orderBuilder.setContract(contractBuilder);
				orderBuilderCacheList.add(orderBuilder);
			}
			logger.info("{}?????????????????????{}?????????{}?????????{}?????????{}?????????{}?????????{}?????????{}????????????{}?????????{} & {}?????????{}", logInfo, 
					symbol, originalOrderId, direction, offsetFlag, price, stopPrice, tradedVolume, tradingDay, hedgeFlag, timeCondition, orderStatus);
		} catch (Throwable t) {
			logger.error("{}OnRtnOrder Exception", logInfo, t);
		}
	}

	// ????????????
	public void OnRtnTrade(CThostFtdcTradeField pTrade) {
		try {

			String exchangeAndOrderSysId = pTrade.getExchangeID() + "@" + pTrade.getOrderSysID();

			String orderId = exchangeIdAndOrderSysIdToOrderIdMap.getOrDefault(exchangeAndOrderSysId, "");
			String adapterOrderId = orderIdToAdapterOrderIdMap.getOrDefault(orderId, "");

			String symbol = pTrade.getInstrumentID();
			DirectionEnum direction = CtpConstant.directionMapReverse.getOrDefault(pTrade.getDirection(), DirectionEnum.D_Unknown);
			String adapterTradeId = adapterOrderId + "@" + direction.getValueDescriptor().getName() + "@" + StringUtils.trim(pTrade.getTradeID());
			String tradeId = gatewayId + "@" + adapterTradeId;
			OffsetFlagEnum offsetFlag = CtpConstant.offsetMapReverse.getOrDefault(pTrade.getOffsetFlag(), OffsetFlagEnum.OF_Unknown);
			double price = pTrade.getPrice();
			int volume = pTrade.getVolume();
			String tradeDate = pTrade.getTradeDate();
			String tradeTime = pTrade.getTradeTime();
			LocalDateTime tradeDatetime = LocalDateTime.of(LocalDate.from(DateTimeConstant.D_FORMAT_INT_FORMATTER.parse(tradeDate)), LocalTime.from(DateTimeConstant.T_FORMAT_FORMATTER.parse(tradeTime)));
			long tradeTimestamp = tradeDatetime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();

			HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(String.valueOf(pTrade.getHedgeFlag()), HedgeFlagEnum.HF_Unknown);
			TradeTypeEnum tradeType = CtpConstant.tradeTypeMapReverse.getOrDefault(pTrade.getTradeType(), TradeTypeEnum.TT_Unknown);
			PriceSourceEnum priceSource = CtpConstant.priceSourceMapReverse.getOrDefault(pTrade.getPriceSource(), PriceSourceEnum.PSRC_Unknown);

			String orderLocalId = StringUtils.trim(pTrade.getOrderLocalID());
			String orderSysId = StringUtils.trim(pTrade.getOrderSysID());
			String sequenceNo = pTrade.getSequenceNo() + "";
			String brokerOrderSeq = pTrade.getBrokerOrderSeq() + "";
			String settlementID = pTrade.getSettlementID() + "";

			String originalOrderId = orderIdToOriginalOrderIdMap.getOrDefault(orderId, "");

			// ????????????????????????,??????userId????????????ID
			String accountCode = userId;
			// ???????????????????????????????????????CNY
			String accountId = accountCode + "@" + gatewayId;

			TradeField.Builder tradeBuilder = TradeField.newBuilder();

			tradeBuilder.setAccountId(accountId);
			tradeBuilder.setAdapterOrderId(adapterOrderId);
			tradeBuilder.setAdapterTradeId(adapterTradeId);
			tradeBuilder.setTradeDate(tradeDate);
			tradeBuilder.setTradeId(tradeId);
			tradeBuilder.setTradeTime(tradeTime);
			tradeBuilder.setTradingDay(tradingDay);
			tradeBuilder.setTradeTimestamp(tradeTimestamp);
			tradeBuilder.setDirection(direction);
			tradeBuilder.setOffsetFlag(offsetFlag);
			tradeBuilder.setOrderId(orderId);
			tradeBuilder.setOriginOrderId(originalOrderId);
			tradeBuilder.setPrice(price);
			tradeBuilder.setVolume(volume);
			tradeBuilder.setGatewayId(gatewayId);
			tradeBuilder.setOrderLocalId(orderLocalId);
			tradeBuilder.setOrderSysId(orderSysId);
			tradeBuilder.setSequenceNo(sequenceNo);
			tradeBuilder.setBrokerOrderSeq(brokerOrderSeq);
			tradeBuilder.setSettlementId(settlementID);
			tradeBuilder.setHedgeFlag(hedgeFlag);
			tradeBuilder.setTradeType(tradeType);
			tradeBuilder.setPriceSource(priceSource);

			if (instrumentQueried && gatewayAdapter.contractMap.containsKey(symbol)) {
				tradeBuilder.setContract(gatewayAdapter.contractMap.get(symbol));
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.TRADE, tradeBuilder.build());
			} else {
				ContractField.Builder contractBuilder = ContractField.newBuilder();
				contractBuilder.setSymbol(symbol);

				tradeBuilder.setContract(contractBuilder);
				tradeBuilderCacheList.add(tradeBuilder);
			}
			logger.info("{}?????????????????????{}?????????{}?????????{}?????????{}?????????{}?????????{}????????????{}?????????{} & {}", logInfo, 
					symbol, originalOrderId, direction, offsetFlag, price, volume, tradingDay, hedgeFlag, tradeType);
		} catch (Throwable t) {
			logger.error("{}OnRtnTrade Exception", logInfo, t);
		}

	}

	// ??????????????????
	public void OnErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
		try {
			logger.error("{}?????????????????????????????????OnErrRtnOrderInsert??? ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			logger.error(
					"{}?????????????????????????????????OnErrRtnOrderInsert??? ?????????????????? ->{InstrumentID:{}, LimitPrice:{}, VolumeTotalOriginal:{}, OrderPriceType:{}, Direction:{}, CombOffsetFlag:{}, OrderRef:{}, InvestorID:{}, UserID:{}, BrokerID:{}, ExchangeID:{}, CombHedgeFlag:{}, ContingentCondition:{}, ForceCloseReason:{}, IsAutoSuspend:{}, IsSwapOrder:{}, MinVolume:{}, TimeCondition:{}, VolumeCondition:{}, StopPrice:{}}", //
					logInfo, //
					pInputOrder.getInstrumentID(), //
					pInputOrder.getLimitPrice(), //
					pInputOrder.getVolumeTotalOriginal(), //
					pInputOrder.getOrderPriceType(), //
					pInputOrder.getDirection(), //
					pInputOrder.getCombOffsetFlag(), //
					pInputOrder.getOrderRef(), //
					pInputOrder.getInvestorID(), //
					pInputOrder.getUserID(), //
					pInputOrder.getBrokerID(), //
					pInputOrder.getExchangeID(), //
					pInputOrder.getCombHedgeFlag(), //
					pInputOrder.getContingentCondition(), //
					pInputOrder.getForceCloseReason(), //
					pInputOrder.getIsAutoSuspend(), //
					pInputOrder.getIsSwapOrder(), //
					pInputOrder.getMinVolume(), //
					pInputOrder.getTimeCondition(), //
					pInputOrder.getVolumeCondition(), //
					pInputOrder.getStopPrice());

			if (instrumentQueried) {
				NoticeField notice = NoticeField.newBuilder()
					.setContent(logInfo + "???????????????????????????????????????ID:" + pRspInfo.getErrorID() + "???????????????:" + pRspInfo.getErrorMsg())
					.setStatus(CommonStatusEnum.COMS_ERROR)
					.setTimestamp(System.currentTimeMillis()).build();
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
			}
		} catch (Throwable t) {
			logger.error("{}OnErrRtnOrderInsert Exception", logInfo, t);
		}

	}

	// ??????????????????
	public void OnErrRtnOrderAction(CThostFtdcOrderActionField pOrderAction, CThostFtdcRspInfoField pRspInfo) {
		if (pRspInfo != null) {
			logger.error("{}????????????????????????(OnErrRtnOrderAction) ??????ID:{},????????????:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			if (instrumentQueried) {
				NoticeField notice = NoticeField.newBuilder()
					.setContent(logInfo + "???????????????????????????????????????ID:" + pRspInfo.getErrorID() + "???????????????:" + pRspInfo.getErrorMsg())
					.setStatus(CommonStatusEnum.COMS_ERROR)
					.setTimestamp(System.currentTimeMillis())
					.build();
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, notice);
			}
		} else {
			logger.error("{}??????????????????????????????(OnErrRtnOrderAction)??????,???????????????", logInfo);
		}
	}

	public void OnRtnInstrumentStatus(CThostFtdcInstrumentStatusField pInstrumentStatus) {
	}

	public void OnRtnBulletin(CThostFtdcBulletinField pBulletin) {
	}

	public void OnRtnTradingNotice(CThostFtdcTradingNoticeInfoField pTradingNoticeInfo) {
	}

	public void OnRtnErrorConditionalOrder(CThostFtdcErrorConditionalOrderField pErrorConditionalOrder) {
	}

	public void OnRtnExecOrder(CThostFtdcExecOrderField pExecOrder) {
	}

	public void OnErrRtnExecOrderInsert(CThostFtdcInputExecOrderField pInputExecOrder, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnExecOrderAction(CThostFtdcExecOrderActionField pExecOrderAction, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnForQuoteInsert(CThostFtdcInputForQuoteField pInputForQuote, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnRtnQuote(CThostFtdcQuoteField pQuote) {
	}

	public void OnErrRtnQuoteInsert(CThostFtdcInputQuoteField pInputQuote, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnQuoteAction(CThostFtdcQuoteActionField pQuoteAction, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField pForQuoteRsp) {
	}

	public void OnRtnCFMMCTradingAccountToken(CThostFtdcCFMMCTradingAccountTokenField pCFMMCTradingAccountToken) {
	}

	public void OnErrRtnBatchOrderAction(CThostFtdcBatchOrderActionField pBatchOrderAction, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnRtnOptionSelfClose(CThostFtdcOptionSelfCloseField pOptionSelfClose) {
	}

	public void OnErrRtnOptionSelfCloseInsert(CThostFtdcInputOptionSelfCloseField pInputOptionSelfClose, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnOptionSelfCloseAction(CThostFtdcOptionSelfCloseActionField pOptionSelfCloseAction, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnRtnCombAction(CThostFtdcCombActionField pCombAction) {
	}

	public void OnErrRtnCombActionInsert(CThostFtdcInputCombActionField pInputCombAction, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnRspQryContractBank(CThostFtdcContractBankField pContractBank, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryParkedOrder(CThostFtdcParkedOrderField pParkedOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryParkedOrderAction(CThostFtdcParkedOrderActionField pParkedOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryTradingNotice(CThostFtdcTradingNoticeField pTradingNotice, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryBrokerTradingParams(CThostFtdcBrokerTradingParamsField pBrokerTradingParams, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQryBrokerTradingAlgos(CThostFtdcBrokerTradingAlgosField pBrokerTradingAlgos, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQueryCFMMCTradingAccountToken(CThostFtdcQueryCFMMCTradingAccountTokenField pQueryCFMMCTradingAccountToken, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRtnFromBankToFutureByBank(CThostFtdcRspTransferField pRspTransfer) {
	}

	public void OnRtnFromFutureToBankByBank(CThostFtdcRspTransferField pRspTransfer) {
	}

	public void OnRtnRepealFromBankToFutureByBank(CThostFtdcRspRepealField pRspRepeal) {
	}

	public void OnRtnRepealFromFutureToBankByBank(CThostFtdcRspRepealField pRspRepeal) {
	}

	public void OnRtnFromBankToFutureByFuture(CThostFtdcRspTransferField pRspTransfer) {
	}

	public void OnRtnFromFutureToBankByFuture(CThostFtdcRspTransferField pRspTransfer) {
	}

	public void OnRtnRepealFromBankToFutureByFutureManual(CThostFtdcRspRepealField pRspRepeal) {
	}

	public void OnRtnRepealFromFutureToBankByFutureManual(CThostFtdcRspRepealField pRspRepeal) {
	}

	public void OnRtnQueryBankBalanceByFuture(CThostFtdcNotifyQueryAccountField pNotifyQueryAccount) {
	}

	public void OnErrRtnBankToFutureByFuture(CThostFtdcReqTransferField pReqTransfer, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnFutureToBankByFuture(CThostFtdcReqTransferField pReqTransfer, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnRepealBankToFutureByFutureManual(CThostFtdcReqRepealField pReqRepeal, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnRepealFutureToBankByFutureManual(CThostFtdcReqRepealField pReqRepeal, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnErrRtnQueryBankBalanceByFuture(CThostFtdcReqQueryAccountField pReqQueryAccount, CThostFtdcRspInfoField pRspInfo) {
	}

	public void OnRtnRepealFromBankToFutureByFuture(CThostFtdcRspRepealField pRspRepeal) {
	}

	public void OnRtnRepealFromFutureToBankByFuture(CThostFtdcRspRepealField pRspRepeal) {
	}

	public void OnRspFromBankToFutureByFuture(CThostFtdcReqTransferField pReqTransfer, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspFromFutureToBankByFuture(CThostFtdcReqTransferField pReqTransfer, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRspQueryBankAccountMoneyByFuture(CThostFtdcReqQueryAccountField pReqQueryAccount, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
	}

	public void OnRtnOpenAccountByBank(CThostFtdcOpenAccountField pOpenAccount) {
	}

	public void OnRtnCancelAccountByBank(CThostFtdcCancelAccountField pCancelAccount) {
	}

	public void OnRtnChangeAccountByBank(CThostFtdcChangeAccountField pChangeAccount) {
	}
}