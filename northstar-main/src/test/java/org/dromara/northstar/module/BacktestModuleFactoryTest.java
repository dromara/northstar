package org.dromara.northstar.module;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractDefinitionProvider;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.mktdata.QuantitDataServiceManager;
import org.dromara.northstar.gateway.playback.PlaybackContract;
import org.dromara.northstar.strategy.example.BeginnerExampleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Slf4j
class BacktestModuleFactoryTest {

	private RestTemplate restTemplate = makeRestTemplate();
	private NorthstarDataSource ds = new NorthstarDataSource(new QuantitDataServiceManager(restTemplate));
	Contract c = Contract.builder()
			.contractId("ao0000@SHFE@FUTURES@PLAYBACK")
			.unifiedSymbol("ao0000@SHFE@FUTURES")
			.symbol("ao0000")
			.exchange(ExchangeEnum.SHFE)
			.productClass(ProductClassEnum.FUTURES)
			.priceTick(1)
			.name("测试合约-氧化铝")
			.gatewayId("testGateway")
			.build();
	ContractDefinition cd = ContractDefinition.builder()
			.symbolPattern(Pattern.compile("ao.+"))
			.marginRatio(0.08)
			.exchange(ExchangeEnum.SHFE)
			.productClass(ProductClassEnum.FUTURES)
			.tradeTimeDef(TradeTimeDefinition.builder()
					.timeSlots(List.of(TimeSlot.builder()
							.start(LocalTime.of(9, 0))
							.end(LocalTime.of(15, 0))
							.build()))
					.weekendClosure(false)
					.build())
			.build();
	List<IContractDefinitionProvider> providers = List.of(new IContractDefinitionProvider() {
		public List<ContractDefinition> get(){
			return List.of(cd);
		}
	});
	
	private MarketCenter mktCenter = new MarketCenter(mock(FastEventEngine.class), providers, new HashMap<>());
	
	private BacktestModuleFactory factory = new BacktestModuleFactory();
	
	ContractSimpleInfo csi = ContractSimpleInfo.builder()
			.id("ao0000@SHFE@FUTURES@PLAYBACK")
			.unifiedSymbol("ao0000@SHFE@FUTURES")
			.build();
	
	RestTemplate makeRestTemplate() {
    	String token = System.getenv("NS_DS_SECRET");
    	String partialToken = "空";
    	if(StringUtils.isNotBlank(token)) {
    		partialToken = token.split("\\.")[2];
    	}
    	log.info("数据服务token验证码: {}", partialToken);
        return new RestTemplateBuilder()
                .setReadTimeout(Duration.ofSeconds(60))
                .setConnectTimeout(Duration.ofSeconds(10))
                .rootUri("https://marketplace.quantit.tech")
				.defaultHeader("Authorization", String.format("Bearer %s", token))
				.defaultHeader("NS_Version", "UnitTest")
                .build();
    }
	
	@BeforeEach
	void setUp() {
		
		mktCenter.addInstrument(new PlaybackContract(c, ds));
		factory.dataSource = ds;
		factory.mktCenter = mktCenter;
		factory.accMgr = mock(AccountManager.class);
		factory.moduleRepo = mock(IModuleRepository.class);
	}

	@Test
	void testCreateTestRunner() {
		StrategyMetaInfo strategyInfo = new StrategyMetaInfo();
		strategyInfo.setName("示例-简单策略");
		strategyInfo.setClassName(BeginnerExampleStrategy.class.getName());
		strategyInfo.setSettings(new BeginnerExampleStrategy.InitParams().getMetaInfo());
		
		ExplorerTaskDescription task = ExplorerTaskDescription.builder()
				.strategySetting(strategyInfo)
				.preloadFrom("2025-01-01")
				.startDate("2025-01-02")
				.endDate("2025-01-03")
				.initBalance(100000)
				.contracts(List.of(csi))
				.intervals(List.of("1m"))
				.build();
		
		assertNotNull(factory.createTestRunner(task));
	}
}
