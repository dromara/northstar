package tech.quantit.northstar.main.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DuplicateKeyException;

import com.alibaba.fastjson.JSON;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.GatewaySettingsMetaInfoProvider;
import tech.quantit.northstar.gateway.api.GatewayChannelProvider;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGateway;
import tech.quantit.northstar.main.utils.CodecUtils;

/**
 * 网关服务
 * 注意：GatewaySetting为了防止数据库被攻破，因此对其做了对称加密，并且会在部署的机器上写入一段随机密码。
 * 只有在代码、数据库、服务器随机密码同时被攻破时，才有可能对GatewaySetting信息进行解码
 * @author KevinHuangwl
 *
 */
@Slf4j
@AllArgsConstructor
public class GatewayService implements InitializingBean {
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private GatewayChannelProvider gatewayTypeProvider;
	
	private GatewaySettingsMetaInfoProvider gatewaySettingsProvider;
	
	private IContractManager contractMgr;
	
	private IGatewayRepository gatewayRepo;
	
	private IMarketDataRepository mdRepo;
	
	private ISimAccountRepository simAccRepo;
	
	private IPlaybackRuntimeRepository playbackRtRepo;
	
	private IModuleRepository moduleRepo;
	
	/**
	 * 创建网关
	 * @return
	 * @throws Exception 
	 */
	public boolean createGateway(GatewayDescription gatewayDescription) {
		log.info("创建网关[{}]", gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		
		return doCreateGateway(gatewayDescription);
	}
	
	private boolean doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayConnection conn = new GatewayConnection(gatewayDescription);
		GatewayFactory factory = gatewayTypeProvider.getFactory(gatewayDescription.getGatewayType());
		gateway = factory.newInstance(gatewayDescription);
		gatewayConnMgr.createPair(conn, gateway);
		if(gatewayDescription.isAutoConnect()) {
			connect(gatewayDescription.getGatewayId());
		}
		
		return true;
	}
	
	private void doSaveGatewayDescription(GatewayDescription gatewayDescription) {
		if(null == gatewayDescription.getSettings()) {
			throw new IllegalArgumentException("网关设置不能为空");
		}
		Object settings = gatewayDescription.getSettings();
		String srcSettings = JSON.toJSONString(settings);
		gatewayDescription.setSettings(CodecUtils.encrypt(srcSettings));
		try {			
			gatewayRepo.insert(gatewayDescription);
		} catch(DuplicateKeyException e) {
			throw new IllegalStateException("已存在同名网关，不能重复创建", e);
		}
		gatewayDescription.setSettings(settings);
	}
	
	/**
	 * 更新网关
	 * @return
	 * @throws Exception 
	 */
	public boolean updateGateway(GatewayDescription gatewayDescription) {
		log.info("更新网关[{}]", gatewayDescription.getGatewayId());
		doDeleteGateway(gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		// 先删除旧的，再重新创建新的
		return doCreateGateway(gatewayDescription);
	}
	
	/**
	 * 移除网关
	 * @return
	 */
	public boolean deleteGateway(String gatewayId) {
		log.info("移除网关[{}]", gatewayId);
		GatewayConnection conn = null;
		if(gatewayConnMgr.exist(gatewayId)) {
			conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		if(conn.isConnected()) {
			throw new IllegalStateException("非断开状态的网关不能删除");
		}
		GatewayDescription gd = conn.getGwDescription();
		if(gd.getGatewayUsage() == GatewayUsage.MARKET_DATA) {			
			for(GatewayConnection gc : gatewayConnMgr.getAllConnections()) {
				if(StringUtils.equals(gc.getGwDescription().getBindedMktGatewayId(), gatewayId)) {
					throw new IllegalStateException("仍有账户网关与本行情网关存在绑定关系，请先解除绑定！");
				}
			}
		} else {
			for(ModuleDescription md : moduleRepo.findAllSettings()) {
				for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
					if(StringUtils.equals(mad.getAccountGatewayId(), gatewayId)) {
						throw new IllegalStateException("仍有模组与本账户网关存在绑定关系，请先解除绑定！");
					}
				}
			}
		}
		boolean flag = doDeleteGateway(gatewayId);
		mdRepo.dropGatewayData(gatewayId);
		if(gd.getGatewayType().equals("SIM"))
			simAccRepo.deleteById(gatewayId);
		if(gd.getGatewayType().equals("PLAYBACK")) 
			playbackRtRepo.deleteById(gatewayId);
		return flag;
	}
	
	private boolean doDeleteGateway(String gatewayId) {
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		Gateway gateway = gatewayConnMgr.getGatewayByConnection(conn);
		gatewayConnMgr.removePair(conn);
		gatewayRepo.deleteById(gatewayId);
		if(gateway instanceof SimTradeGateway simGateway) {
			simGateway.destory();
		}
		return true;
	}
	
	/**
	 * 查询所有网关配置
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllGatewayDescription() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.toList();
	}
	
	/**
	 * 查询网关配置
	 * @param gatewayId
	 * @return
	 */
	public GatewayDescription findGatewayDescription(String gatewayId) {
		return gatewayConnMgr.getGatewayConnectionById(gatewayId).getGwDescription();
	}
	
	/**
	 * 查询所有行情网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllMarketGatewayDescription() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.filter(gwDescription -> gwDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA)
				.toList();
	}
	
	/**
	 * 查询所有交易网关
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllTraderGatewayDescription() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.filter(gwDescription -> gwDescription.getGatewayUsage() != GatewayUsage.MARKET_DATA)
				.toList();
	}
	
	/**
	 * 连接网关
	 * @return
	 */
	public boolean connect(String gatewayId) {
		log.info("连接网关[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
			gateway.connect();
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		
		return true;
	}
	
	/**
	 * 断开网关
	 * @return
	 */
	public boolean disconnect(String gatewayId) {
		log.info("断开网关[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			gatewayConnMgr.getGatewayById(gatewayId).disconnect();
		} else {
			throw new NoSuchElementException("没有该网关记录：" +  gatewayId);
		}
		
		return true;
	}
	
	/**
	 * 模拟出入金
	 * @param money
	 * @return
	 */
	public boolean simMoneyIO(String gatewayId, int money) {
		SimTradeGateway gateway = (SimTradeGateway) gatewayConnMgr.getGatewayById(gatewayId);
		gateway.moneyIO(money);
		if(money != 0) {			
			log.info("模拟账户[{}]，{}金：{}", gatewayId, money>0 ? "入": "出", Math.abs(money));
		}
		return true;
	}
	
	/**
	 * 活跃检测
	 * @param gatewayId
	 * @return
	 */
	public boolean isActive(String gatewayId) {
		try {
			MarketGateway gateway = (MarketGateway) gatewayConnMgr.getGatewayById(gatewayId);
			return gateway.isActive();
		} catch (ClassCastException e) {
			throw new IllegalStateException(gatewayId + "不是一个行情网关", e);
		}
	}
	
	/**
	 * 网关配置元信息
	 * @param gatewayType
	 * @return
	 */
	public Collection<ComponentField> getGatewaySettingsMetaInfo(String gatewayType) {
		return gatewaySettingsProvider.getSettings(gatewayType);
	}
	
	/**
	 * 网关已订阅合约
	 * @param gatewayId
	 * @return
	 */
	public List<ContractSimpleInfo> getSubscribedContractList(String gatewayId){
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(gd == null) {
			throw new NoSuchElementException("没有找到网关：" + gatewayId);
		}
		return gd.getSubscribedContracts();
	}
	
	/**
	 * 复位重置回放网关
	 * @param gatewayId
	 * @return
	 * @throws Exception 
	 */
	public boolean resetPlayback(String gatewayId) {
		log.info("复位 [{}] 行情回放网关", gatewayId);
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		playbackRtRepo.deleteById(gatewayId);
		decodeSettings(gd);
		doCreateGateway(gd);
		return true;
	}
	
	private GatewayDescription decodeSettings(GatewayDescription gd) {
		String decodeStr = CodecUtils.decrypt((String) gd.getSettings());
		if(!JSON.isValid(decodeStr)) {
			throw new IllegalStateException("解码字符串非法，很可能是临时文件夹" + System.getProperty("user.home") + File.separator
					+ ".northstar-salt这个盐文件与加密时的不一致导致无法解码。解决办法：手动移除旧的Gateway数据，重新录入，并确保盐文件不会丢失。");
		}
		gd.setSettings(JSON.parseObject(decodeStr));
		return gd;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		CompletableFuture.runAsync(() -> {
			List<GatewayDescription> result = gatewayRepo.findAll();
			// 因为依赖关系，加载要有先后顺序
			result.stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.MARKET_DATA).map(this::decodeSettings).forEach(this::doCreateGateway);
			result.stream().filter(gd -> gd.getGatewayUsage() == GatewayUsage.TRADE).map(this::decodeSettings).forEach(this::doCreateGateway);
		}, CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS));
	}

}