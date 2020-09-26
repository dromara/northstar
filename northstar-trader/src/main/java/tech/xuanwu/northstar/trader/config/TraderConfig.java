package tech.xuanwu.northstar.trader.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.persistance.CtpSettingRepo;
import tech.xuanwu.northstar.persistance.po.CtpSetting.ConnectionType;
import tech.xuanwu.northstar.persistance.po.CtpSetting.MarketType;
import tech.xuanwu.northstar.persistance.po.CtpSetting;
import tech.xuanwu.northstar.trader.constants.Constants;
import tech.xuanwu.northstar.trader.domain.simulated.SimulatedGateway;
import tech.xuanwu.northstar.trader.domain.simulated.SimulatedMarket;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayImpl;
import xyz.redtorch.pb.CoreEnum.ConnectStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewayField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

/**
 * 交易端设置
 * @author kevinhuangwl
 *
 */
@Slf4j
@Configuration
public class TraderConfig implements InitializingBean{
	
	@Value("${spring.profiles.active:prod}")
	String profile;
	
	@Autowired
	CtpSettingRepo ctpSettingRepo;
	
	HashMap<String, String> envMap = new HashMap<>(){
		{
			put("dev", "仿真行情（Simnow724）");
			put("test", "测试行情（Simnow）");
			put("prod", "真实行情（RealCTP）");
		}
	};
	
	@PostConstruct
	private void printEnv() {
		log.info("当前运行：{}", envMap.get(profile));
	}
	
	/**
	 * 合约字典
	 * @return
	 */
	@Bean(Constants.CONTRACT_MAP)
	public ConcurrentHashMap<String, ContractField> createContractMap(){
		return new ConcurrentHashMap<String, ContractField>(1000);
	}
	
	/**
	 * 交易账户
	 * @return
	 */
	@Bean(Constants.TRADABLE_ACCOUNT)
	public ConcurrentHashMap<String, GatewayApi> createGatewayMap(FastEventEngine fastEventEngine, 
			@Qualifier(Constants.CONTRACT_MAP) ConcurrentHashMap<String, ContractField> contractMap){
		ConcurrentHashMap<String, GatewayApi> accountMap = new ConcurrentHashMap<>(); 
		Iterable<CtpSetting> ctpSettings = ctpSettingRepo.findByMarketType(getMarketType());
		Iterator<CtpSetting> itSettings = ctpSettings.iterator();
		log.info("----------初始化账户----------");
		while(itSettings.hasNext()) {
			CtpSetting ctpSetting = itSettings.next();
			String gatewayId = ctpSetting.getGatewayId();
			if(ctpSetting.getConnectionType() == ConnectionType.SIM_ACCOUNT) {
				log.info("初始化CTP模拟账户：{}", gatewayId);
				SimulatedMarket simMarket = new SimulatedMarket(gatewayId, Constants.CTP_MARKETDATA, fastEventEngine, contractMap);
				GatewayApi simGateway = new SimulatedGateway(fastEventEngine, ctpSetting.convertTo(), simMarket);
				accountMap.put(gatewayId, simGateway);
				simGateway.connect();
			}
			if(ctpSetting.getConnectionType() == ConnectionType.ACCOUNT) {
				log.info("初始化CTP账户：{}", gatewayId);
				GatewayApi gateway = new CtpGatewayImpl(fastEventEngine, ctpSetting.convertTo());
				accountMap.put(gatewayId, gateway);
				gateway.connect();
			}
			
		}
		log.info("----------初始化账户结束----------");
		
		return accountMap;
	}
	
	/**
	 * 网关概况
	 * @param gatewayMap
	 * @return
	 */
	@Bean(Constants.TRADABLE_ACCOUNT_PROFILE)
	public ConcurrentHashMap<String, GatewayField> createGatewayProfileMap(
			@Qualifier(Constants.TRADABLE_ACCOUNT) Map<String, GatewayApi> gatewayMap){
		ConcurrentHashMap<String, GatewayField> gatewayProfileMap = new ConcurrentHashMap<String, GatewayField>();
		for(Entry<String, GatewayApi> e : gatewayMap.entrySet()) {
			String gatewayId = e.getValue().getGateway().getGatewayId();
			GatewayField gatewayProfile = GatewayField.newBuilder()
					.setGatewayId(gatewayId)
					.setName(e.getValue().getGatewaySetting().getGatewayName())
					.setStatus(ConnectStatusEnum.CS_Disconnected)
					.setGatewayType(e.getValue().getGatewaySetting().getGatewayType())
					.setGatewayAdapterType(e.getValue().getGatewaySetting().getGatewayAdapterType())
					.build();
			gatewayProfileMap.put(gatewayId, gatewayProfile);
		}
		return gatewayProfileMap;
	}
	
	/**
	 * CTP行情网关
	 * @param fastEventEngine
	 * @return
	 */
	@Bean(Constants.CTP_MARKETDATA)
	public GatewayApi createCtpMarketDataGateway(FastEventEngine fastEventEngine) {
		Iterable<CtpSetting> ctpSettings = ctpSettingRepo.findByMarketType(getMarketType());
		Iterator<CtpSetting> itSettings = ctpSettings.iterator();
		GatewaySettingField gatewaySetting = null;
		while(itSettings.hasNext()) {
			CtpSetting ctpSetting = itSettings.next();
			if(ctpSetting.getConnectionType() == ConnectionType.SIM_ACCOUNT) {
				gatewaySetting = ctpSetting.convertTo();
				break;
			}
		}
		if(gatewaySetting == null) {
			throw new IllegalStateException("没有找到相应的行情设置");
		}
		
		gatewaySetting = gatewaySetting.toBuilder()
				.setGatewayId(Constants.CTP_MARKETDATA)
				.setGatewayName(Constants.CTP_MARKETDATA)
				.setStatus(ConnectStatusEnum.CS_Connected)
				.build();
		GatewayApi gateway = new CtpGatewayImpl(fastEventEngine, gatewaySetting);
		gateway.connect();		
		return gateway;
	}
	
	
	protected MarketType getMarketType() {
		MarketType type;
		switch(profile) {
		case "prod":
			type = MarketType.REAL;
			break;
		case "dev":
			type = MarketType.SIMULATE;
			break;
		case "test":
			type = MarketType.TEST;
			break;
		default:
			throw new IllegalStateException();
		}
		return type;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("设置完成");
	}
}
