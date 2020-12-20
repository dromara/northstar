package tech.xuanwu.northstar.trader.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.GatewayApi;
import tech.xuanwu.northstar.persistance.CtpSettingRepo;
import tech.xuanwu.northstar.persistance.po.CtpSetting;
import tech.xuanwu.northstar.persistance.po.CtpSetting.ConnectionType;
import tech.xuanwu.northstar.persistance.po.CtpSetting.MarketType;
import tech.xuanwu.northstar.trader.constants.Constants;
import tech.xuanwu.northstar.trader.domain.simulated.SimulatedGateway;
import tech.xuanwu.northstar.trader.domain.simulated.SimulatedMarket;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayImpl;
import xyz.redtorch.pb.CoreEnum.ConnectStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

/**
 * 交易端设置
 * @author kevinhuangwl
 *
 */
@Slf4j
@Configuration
@PropertySource(value="classpath:ctp-${spring.profiles.active}.yml", factory=YamlPropertySourceFactory.class)
public class TraderConfig implements InitializingBean{
	
	@Value("${spring.profiles.active:prod}")
	String profile;
	
	@Autowired
	CtpSettingRepo ctpSettingRepo;
	
	HashMap<String, String> envMap = new HashMap<>(){
		private static final long serialVersionUID = 7655899354976577760L;

		{
			put("dev", "仿真行情（Simnow724）");
			put("test", "测试行情（Simnow）");
			put("prod", "真实行情（RealCTP）");
		}
	};
	
	ConcurrentHashMap<String, GatewayApi> accountMap = new ConcurrentHashMap<>(); 
	
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
	 * 真实交易账户
	 * @return
	 */
	@Bean(Constants.TRADABLE_ACCOUNT)
	public ConcurrentHashMap<String, GatewayApi> createGatewayMap(FastEventEngine fastEventEngine, 
			@Qualifier(Constants.CONTRACT_MAP) ConcurrentHashMap<String, ContractField> contractMap){
		Iterable<CtpSetting> ctpSettings = ctpSettingRepo.findByMarketType(getMarketType());
		Iterator<CtpSetting> itSettings = ctpSettings.iterator();
		log.info("----------初始化账户----------");
		while(itSettings.hasNext()) {
			CtpSetting ctpSetting = itSettings.next();
			String gatewayId = ctpSetting.getGatewayId();
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
	
	@Bean
	@ConfigurationProperties(prefix="data-gateway-setting")
	public CtpSetting createCtpSetting() {
		return new CtpSetting();
	}
	
	/**
	 * CTP行情网关
	 * @param fastEventEngine
	 * @return
	 */
	@Bean(Constants.CTP_MARKETDATA)
	public GatewayApi createCtpMarketDataGateway(FastEventEngine fastEventEngine, CtpSetting ctpSetting) {
		GatewaySettingField gatewaySetting = ctpSetting.convertTo();
		gatewaySetting = gatewaySetting.toBuilder()
				.setGatewayId(Constants.CTP_MARKETDATA)
				.setGatewayName(Constants.CTP_MARKETDATA)
				.setStatus(ConnectStatusEnum.CS_Connected)
				.build();
		GatewayApi gateway = new CtpGatewayImpl(fastEventEngine, gatewaySetting);
		gateway.connect();		
		return gateway;
	}
	
	/**
	 * CTP模拟账户
	 * @param fastEventEngine
	 * @param ctpSetting
	 * @param contractMap
	 * @return
	 */
	@Bean(Constants.CTP_SIM_ACCOUNT)
	public GatewayApi createCtpSimAccount(FastEventEngine fastEventEngine, CtpSetting ctpSetting,
			@Qualifier(Constants.CONTRACT_MAP) ConcurrentHashMap<String, ContractField> contractMap) {
		log.info("初始化CTP模拟账户：{}", ctpSetting.getGatewayId());
		SimulatedMarket simMarket = new SimulatedMarket(ctpSetting.getGatewayId(), Constants.CTP_MARKETDATA, fastEventEngine, contractMap);
		GatewayApi simGateway = new SimulatedGateway(fastEventEngine, ctpSetting.convertTo(), simMarket);
		accountMap.put(ctpSetting.getGatewayId(), simGateway);
		simGateway.connect();
		return simGateway;
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
