package xyz.redtorch.gateway.ctp.x64v6v3v15v;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.common.GatewayAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
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
	
	public CtpGatewayAdapter(FastEventEngine fastEventEngine, GatewayDescription gd, IMarketCenter mktCenter) {
		super(gd, mktCenter);
		
		switch(gd.getGatewayUsage()){
		case TRADE: 
			tdSpi = new TdSpi(this);
			break;
		case MARKET_DATA:
			mdSpi = new MdSpi(this);
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + gd.getGatewayUsage());
		}
		
		this.fastEventEngine = fastEventEngine;
	}
	
	@Override
	public boolean subscribe(ContractField contractField) {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			if (mdSpi == null) {
				logger.error(getLogInfo() + "行情接口尚未初始化或已断开");
				return false;
			}
			return mdSpi.subscribe(contractField.getSymbol());
		}
		logger.warn(getLogInfo() + "不包含订阅功能");
		return false;
	}

	@Override
	public boolean unsubscribe(ContractField contractField) {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			if (mdSpi == null) {
				logger.error(getLogInfo() + "行情接口尚未初始化或已断开");
				return false;
			}
			return mdSpi.unsubscribe(contractField.getSymbol());
		}
		logger.warn(getLogInfo() + "不包含取消订阅功能");
		return false;
	}

	@Override
	public String submitOrder(SubmitOrderReqField submitOrderReq) {
		if (!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		return tdSpi.submitOrder(submitOrderReq);
	}

	@Override
	public boolean cancelOrder(CancelOrderReqField cancelOrderReq) {
		if (!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		return tdSpi.cancelOrder(cancelOrderReq);
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
				logger.warn("当前网关类型：{}", gatewayDescription.getGatewayUsage());
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

		if (gatewayDescription.getGatewayUsage() == GatewayUsage.TRADE) {
			if (tdSpi == null) {
				tdSpi = new TdSpi(this);
			}
			tdSpi.connect();
		} else if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			if (mdSpi == null) {
				mdSpi = new MdSpi(this);
			}
			mdSpi.connect();
		}
	}

	private boolean isConnected() {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.TRADE && tdSpi != null) {
			return tdSpi.isConnected();
		} else if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA && mdSpi != null) {
			return mdSpi.isConnected();
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
		Files.deleteIfExists(targetFile.toPath());
		FileUtils.copyURLToFile(sourceURL, targetFile);

		targetFile.deleteOnExit();
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.CTP;
	}

	@Override
	public ConnectionState getConnectionState() {
		return connState;
	}

}
