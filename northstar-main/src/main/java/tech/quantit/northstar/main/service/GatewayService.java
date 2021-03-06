package tech.quantit.northstar.main.service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DuplicateKeyException;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ContractDefinition;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.gateway.api.Gateway;
import tech.quantit.northstar.gateway.api.GatewayFactory;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.playback.PlaybackGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimGatewayFactory;
import tech.quantit.northstar.gateway.sim.trade.SimTradeGateway;
import tech.quantit.northstar.main.utils.CodecUtils;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * ????????????
 * ?????????GatewaySetting???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
 * ???????????????????????????????????????????????????????????????????????????????????????GatewaySetting??????????????????
 * @author KevinHuangwl
 *
 */
@Slf4j
public class GatewayService implements InitializingBean, ApplicationContextAware{
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private IGatewayRepository gatewayRepo;
	
	private IMarketDataRepository mdRepo;
	
	private ISimAccountRepository simAccRepo;
	
	private IPlaybackRuntimeRepository playbackRtRepo;
	
	private ApplicationContext ctx;
	
	private IModuleRepository moduleRepo;
	
	private ContractManager contractMgr;
	
	public GatewayService(GatewayAndConnectionManager gatewayConnMgr, IGatewayRepository gatewayRepo, IMarketDataRepository mdRepo,
			IPlaybackRuntimeRepository playbackRtRepo, IModuleRepository moduleRepo, ISimAccountRepository simAccRepo, ContractManager contractMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.gatewayRepo = gatewayRepo;
		this.mdRepo = mdRepo;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.simAccRepo = simAccRepo;
		this.playbackRtRepo = playbackRtRepo;
	}
	
	/**
	 * ????????????
	 * @return
	 * @throws Exception 
	 */
	public boolean createGateway(GatewayDescription gatewayDescription) throws Exception {
		log.info("????????????[{}]", gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		
		return doCreateGateway(gatewayDescription);
	}
	
	private boolean doCreateGateway(GatewayDescription gatewayDescription) {
		Gateway gateway = null;
		GatewayConnection conn = new GatewayConnection(gatewayDescription);
		GatewayFactory factory = null;
		if(gatewayDescription.getGatewayType() == GatewayType.CTP) {
			factory = ctx.getBean(CtpGatewayFactory.class);
		} else if(gatewayDescription.getGatewayType() == GatewayType.SIM) {
			factory = ctx.getBean(SimGatewayFactory.class);
		} else if(gatewayDescription.getGatewayType() == GatewayType.CTP_SIM) {
			factory = ctx.getBean(CtpSimGatewayFactory.class);
		} else if(gatewayDescription.getGatewayType() == GatewayType.PLAYBACK) {
			factory = ctx.getBean(PlaybackGatewayFactory.class);
		} else {
			throw new NoSuchElementException("???????????????????????????" + gatewayDescription.getGatewayType());
		}
		gateway = factory.newInstance(gatewayDescription);
		gatewayConnMgr.createPair(conn, gateway);
		if(gatewayDescription.isAutoConnect()) {
			connect(gatewayDescription.getGatewayId());
		}
		
		return true;
	}
	
	private void doSaveGatewayDescription(GatewayDescription gatewayDescription) {
		if(null == gatewayDescription.getSettings()) {
			throw new IllegalArgumentException("????????????????????????");
		}
		Object settings = gatewayDescription.getSettings();
		String srcSettings = JSON.toJSONString(settings);
		gatewayDescription.setSettings(CodecUtils.encrypt(srcSettings));
		try {			
			gatewayRepo.insert(gatewayDescription);
		} catch(DuplicateKeyException e) {
			throw new IllegalStateException("??????????????????????????????????????????", e);
		}
		gatewayDescription.setSettings(settings);
	}
	
	/**
	 * ????????????
	 * @return
	 * @throws Exception 
	 */
	public boolean updateGateway(GatewayDescription gatewayDescription) throws Exception {
		log.info("????????????[{}]", gatewayDescription.getGatewayId());
		doDeleteGateway(gatewayDescription.getGatewayId());
		doSaveGatewayDescription(gatewayDescription);
		// ???????????????????????????????????????
		return doCreateGateway(gatewayDescription);
	}
	
	/**
	 * ????????????
	 * @return
	 */
	public boolean deleteGateway(String gatewayId) {
		log.info("????????????[{}]", gatewayId);
		GatewayConnection conn = null;
		if(gatewayConnMgr.exist(gatewayId)) {
			conn = gatewayConnMgr.getGatewayConnectionById(gatewayId);
		} else {
			throw new NoSuchElementException("????????????????????????" +  gatewayId);
		}
		if(conn.isConnected()) {
			throw new IllegalStateException("????????????????????????????????????");
		}
		GatewayDescription gd = conn.getGwDescription();
		if(gd.getGatewayUsage() == GatewayUsage.MARKET_DATA) {			
			for(GatewayConnection gc : gatewayConnMgr.getAllConnections()) {
				if(StringUtils.equals(gc.getGwDescription().getBindedMktGatewayId(), gatewayId)) {
					throw new IllegalStateException("??????????????????????????????????????????????????????????????????????????????");
				}
			}
		} else {
			for(ModuleDescription md : moduleRepo.findAllSettings()) {
				for(ModuleAccountDescription mad : md.getModuleAccountSettingsDescription()) {
					if(StringUtils.equals(mad.getAccountGatewayId(), gatewayId)) {
						throw new IllegalStateException("????????????????????????????????????????????????????????????????????????");
					}
				}
			}
		}
		boolean flag = doDeleteGateway(gatewayId);
		mdRepo.dropGatewayData(gatewayId);
		if(gd.getGatewayType() == GatewayType.SIM)
			simAccRepo.deleteById(gatewayId);
		if(gd.getGatewayType() == GatewayType.PLAYBACK) 
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
	 * ????????????????????????
	 * @return
	 * @throws Exception 
	 */
	public List<GatewayDescription> findAllGatewayDescription() {
		return gatewayConnMgr.getAllConnections().stream()
				.map(GatewayConnection::getGwDescription)
				.toList();
	}
	
	/**
	 * ??????????????????
	 * @param gatewayId
	 * @return
	 */
	public GatewayDescription findGatewayDescription(String gatewayId) {
		return gatewayConnMgr.getGatewayConnectionById(gatewayId).getGwDescription();
	}
	
	/**
	 * ????????????????????????
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
	 * ????????????????????????
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
	 * ????????????
	 * @return
	 */
	public boolean connect(String gatewayId) {
		log.info("????????????[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			Gateway gateway = gatewayConnMgr.getGatewayById(gatewayId);
			gateway.connect();
		} else {
			throw new NoSuchElementException("????????????????????????" +  gatewayId);
		}
		
		return true;
	}
	
	/**
	 * ????????????
	 * @return
	 */
	public boolean disconnect(String gatewayId) {
		log.info("????????????[{}]", gatewayId);
		if(gatewayConnMgr.exist(gatewayId)) {
			gatewayConnMgr.getGatewayById(gatewayId).disconnect();
		} else {
			throw new NoSuchElementException("????????????????????????" +  gatewayId);
		}
		
		return true;
	}
	
	/**
	 * ???????????????
	 * @param money
	 * @return
	 */
	public boolean simMoneyIO(String gatewayId, int money) {
		SimTradeGateway gateway = (SimTradeGateway) gatewayConnMgr.getGatewayById(gatewayId);
		gateway.moneyIO(money);
		if(money != 0) {			
			log.info("????????????[{}]???{}??????{}", gatewayId, money>0 ? "???": "???", Math.abs(money));
		}
		return true;
	}
	
	/**
	 * ????????????
	 * @param gatewayId
	 * @return
	 */
	public boolean isActive(String gatewayId) {
		try {
			MarketGateway gateway = (MarketGateway) gatewayConnMgr.getGatewayById(gatewayId);
			return gateway.isActive();
		} catch (ClassCastException e) {
			throw new IllegalStateException(gatewayId + "????????????????????????", e);
		}
	}
	
	/**
	 * ??????????????????
	 * @param gatewayType
	 * @return
	 */
	public List<ContractDefinition> contractDefinitions(GatewayType gatewayType){
		return contractMgr.getAllContractDefinitions().stream()
				.filter(def -> def.getGatewayType() == gatewayType)
				.toList();
	}
	
	/**
	 * ?????????????????????
	 * @param gatewayType
	 * @return
	 */
	public List<byte[]> getSubscribedContracts(String gatewayId){
		Map<String, GatewayDescription> resultMap = gatewayRepo.findAll().stream().collect(Collectors.toMap(GatewayDescription::getGatewayId, item -> item));
		GatewayDescription gd = resultMap.get(gatewayId);
		if(gd == null) {
			throw new NoSuchElementException("????????????????????????" + gatewayId);
		}
		List<ContractField> resultList = new LinkedList<>();
		for(String contractDefId : gd.getSubscribedContractGroups()) {
			resultList.addAll(contractMgr.relativeContracts(contractDefId));
		}
		return resultList.stream().map(ContractField::toByteArray).toList();
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		List<GatewayDescription> result = gatewayRepo.findAll();
		for(GatewayDescription gd : result) {
			String decodeStr = CodecUtils.decrypt((String) gd.getSettings());
			if(!JSON.isValid(decodeStr)) {
				throw new IllegalStateException("???????????????????????????????????????????????????" + System.getProperty("user.home") + File.separator
						+ ".northstar-salt?????????????????????????????????????????????????????????????????????????????????????????????Gateway?????????????????????????????????????????????????????????");
			}
			gd.setSettings(JSON.parseObject(decodeStr));
			doCreateGateway(gd);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}
}