package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.gateway.api.GatewayAbstract;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import tech.quantit.northstar.gateway.ctp.CTP;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class CtpGatewayAdapter extends GatewayAbstract implements MarketGateway, TradeGateway {

	private static final Logger logger = LoggerFactory.getLogger(CtpGatewayAdapter.class);

	static {
		String envTmpDir = "";
		String tempLibPath = "";
		try {
			logger.info("开始复制运行库");
			if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {

				envTmpDir = System.getProperty("java.io.tmpdir");
				tempLibPath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "api" + File.separator + "jctp" + File.separator + "lib" + File.separator
						+ "jctpv6v3v15x64api" + File.separator;

				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/libiconv.dll"));
				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/thostmduserapi_se.dll"));
				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/thosttraderapi_se.dll"));
				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/jctpv6v3v15x64api.dll"));
			} else {

				envTmpDir = "/tmp";
				tempLibPath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "api" + File.separator + "jctp" + File.separator + "lib" + File.separator
						+ "jctpv6v3v15x64api" + File.separator;

				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/libthostmduserapi_se.so"));
				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/libthosttraderapi_se.so"));
				copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/libjctpv6v3v15x64api.so"));
			}
		} catch (Exception e) {
			logger.warn("复制运行库失败", e);
		}

		try {
			logger.info("开始加载运行库");
			if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
				System.load(tempLibPath + File.separator + "libiconv.dll");
				System.load(tempLibPath + File.separator + "thostmduserapi_se.dll");
				System.load(tempLibPath + File.separator + "thosttraderapi_se.dll");
				System.load(tempLibPath + File.separator + "jctpv6v3v15x64api.dll");
			} else {
				System.load(tempLibPath + File.separator + "libthostmduserapi_se.so");
				System.load(tempLibPath + File.separator + "libthosttraderapi_se.so");
				System.load(tempLibPath + File.separator + "libjctpv6v3v15x64api.so");
			}
		} catch (Exception e) {
			logger.warn("加载运行库失败", e);
		}
	}
	
	private MdSpi mdSpi = null;
	private TdSpi tdSpi = null;
	
	public CtpGatewayAdapter(FastEventEngine fastEventEngine, GatewaySettingField gatewaySetting, GlobalMarketRegistry registry) {
		super(gatewaySetting, registry);
		
		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_Trade) {
			tdSpi = new TdSpi(this);
		} else if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData) {
			mdSpi = new MdSpi(this);
		} else {
			mdSpi = new MdSpi(this);
			tdSpi = new TdSpi(this);
		}
		
		this.fastEventEngine = fastEventEngine;
	}
	
	
	
	@Override
	public boolean subscribe(ContractField contractField) {
		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData || gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_TradeAndMarketData) {
			if (mdSpi == null) {
				logger.error(getLogInfo() + "行情接口尚未初始化或已断开");
				return false;
			} else {
				// 如果网关类型仅为行情,那就无法通过交易接口拿到合约信息，以订阅时的合约信息为准
				if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData) {
					contractMap.put(contractField.getSymbol(), contractField);
				}

				return mdSpi.subscribe(contractField.getSymbol());
			}
		} else {
			logger.warn(getLogInfo() + "不包含订阅功能");
			return false;
		}
	}

	@Override
	public boolean unsubscribe(ContractField contractField) {
		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData || gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_TradeAndMarketData) {
			if (mdSpi == null) {
				logger.error(getLogInfo() + "行情接口尚未初始化或已断开");
				return false;
			} else {
				return mdSpi.unsubscribe(contractField.getSymbol());
			}
		} else {
			logger.warn(getLogInfo() + "不包含取消订阅功能");
			return false;
		}
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_Trade || gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_TradeAndMarketData) {
			if (tdSpi == null || !tdSpi.isConnected()) {
				logger.error(getLogInfo() + "交易接口尚未初始化或已断开");
				return "";
			} else {
				return tdSpi.submitOrder(submitOrderReq);
			}
		} else {
			logger.warn(getLogInfo() + "不包含提交定单功能");
			return "";
		}
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_Trade || gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_TradeAndMarketData) {
			if (tdSpi == null || !tdSpi.isConnected()) {
				logger.error(getLogInfo() + "交易接口尚未初始化或已断开");
				return false;
			} else {
				return tdSpi.cancelOrder(cancelOrderReq);
			}
		} else {
			logger.warn(getLogInfo() + "不包含撤销定单功能");
			return false;
		}
	}

	@Override
	public void disconnect() {

		lastConnectBeginTimestamp = 0;

		final TdSpi tdSpiForDisconnect = tdSpi;
		final MdSpi mdSpiForDisconnect = mdSpi;
		tdSpi = null;
		mdSpi = null;
		new Thread(new Runnable() {
			@Override
			public void run() {
				logger.warn("当前网关类型：{}", gatewaySetting.getGatewayType());
				try {
					if(tdSpiForDisconnect != null) {
						tdSpiForDisconnect.disconnect();
						logger.info("断开tdSpi");
					}
					if(mdSpiForDisconnect != null) {
						mdSpiForDisconnect.disconnect();
						logger.info("断开mdSpi");
					}
					logger.warn(getLogInfo() + "异步断开操作完成");
				} catch (Throwable t) {
					logger.error(getLogInfo() + "异步断开操作错误", t);
				}

			}
		}).start();
	}

	@Override
	public void connect() {
		lastConnectBeginTimestamp = System.currentTimeMillis();

		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_Trade) {
			if (tdSpi == null) {
				tdSpi = new TdSpi(this);
			}
		} else if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData) {
			if (mdSpi == null) {
				mdSpi = new MdSpi(this);
			}
		} else {
			if (tdSpi == null) {
				tdSpi = new TdSpi(this);
			}
			if (mdSpi == null) {
				mdSpi = new MdSpi(this);
			}
		}

		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_Trade && tdSpi != null) {
			tdSpi.connect();
		} else if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData && mdSpi != null) {
			mdSpi.connect();
		} else if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_TradeAndMarketData && tdSpi != null && mdSpi != null) {
			tdSpi.connect();
			mdSpi.connect();
		} else {
			logger.error(getLogInfo() + "检测到SPI实例为空");
		}
	}

	@Override
	public boolean isConnected() {
		if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_Trade && tdSpi != null) {
			return tdSpi.isConnected();
		} else if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_MarketData && mdSpi != null) {
			return mdSpi.isConnected();
		} else if (gatewaySetting.getGatewayType() == GatewayTypeEnum.GTE_TradeAndMarketData && tdSpi != null && mdSpi != null) {
			return tdSpi.isConnected() && mdSpi.isConnected();
		}
		return false;
	}
	
	@Override
	public boolean isActive() {
		if(mdSpi == null) {
			return false;
		}
		return mdSpi.isActive();
	}
	
	/**
	 * 复制URL到临时文件夹,例如从war包中
	 * 
	 * @param targetDir
	 * @param sourceURL
	 * @throws IOException
	 */
	private static void copyURLToFileForTmp(String targetDir, URL sourceURL) throws IOException {
		File orginFile = new File(sourceURL.getFile());
		File targetFile = new File(targetDir + File.separator + orginFile.getName());
		if (!targetFile.getParentFile().exists()) {
			targetFile.getParentFile().mkdirs();
		}
		if (targetFile.exists()) {
			targetFile.delete();
		}
		FileUtils.copyURLToFile(sourceURL, targetFile);

		targetFile.deleteOnExit();
	}



	@Override
	public String gatewayType() {
		return CTP.class.getName();
	}

}
