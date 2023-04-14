package tech.quantit.northstar.data.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.data.redis.ModuleRepoRedisImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.ModuleType;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.data.IModuleRepository;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

class ModuleRepoRedisImplTest {

	static LettuceConnectionFactory factory = new LettuceConnectionFactory();
	
	static RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
	
	static IModuleRepository repo;
	
	String KEY_PREFIX = Constants.APP_NAME + "Module:";
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	TradeField openTrade = fieldFactory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = fieldFactory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
	
	String moduleName = "testModule";
	
	ModuleAccountDescription mad = ModuleAccountDescription.builder()
			.accountGatewayId("testGateway")
			.moduleAccountInitBalance(100000)
			.bindedContracts(List.of(ContractSimpleInfo.builder().value("rb2210@SHFE@FUTURES").build()))
			.build();
	
	ModuleDescription md = ModuleDescription.builder()
			.moduleName(moduleName)
			.type(ModuleType.SPECULATION)
			.moduleAccountSettingsDescription(List.of(mad))
			.build();
	
	ModuleAccountRuntimeDescription mard = ModuleAccountRuntimeDescription.builder()
			.accountId("testModuleAcc")
			.initBalance(100000)
			.build();
	
	ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
			.moduleName(moduleName)
			.accountRuntimeDescriptionMap(Map.of("testModuleAcc", mard))
			.build();
	
	ModuleDealRecord mdr = ModuleDealRecord.builder()
			.moduleName(moduleName)
			.openTrade(openTrade.toByteArray())
			.closeTrade(closeTrade.toByteArray())
			.build();
	
	@BeforeEach
	void prepare() {
		factory.setDatabase(15);
		factory.afterPropertiesSet();
		
		redisTemplate.setConnectionFactory(factory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
		redisTemplate.afterPropertiesSet();
		
		repo = new ModuleRepoRedisImpl(redisTemplate);
	}
	
	@AfterEach
	void cleanup() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	void testSaveSettings() {
		repo.saveSettings(md);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "Settings:" + moduleName)).isTrue();
	}

	@Test
	void testFindSettingsByName() {
		repo.saveSettings(md);
		assertThat(repo.findSettingsByName(moduleName)).isEqualTo(md);
	}

	@Test
	void testFindAllSettings() {
		repo.saveSettings(md);
		assertThat(repo.findAllSettings()).isNotEmpty();
	}

	@Test
	void testDeleteSettingsByName() {
		repo.saveSettings(md);
		repo.deleteSettingsByName(moduleName);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "Settings:" + moduleName)).isFalse();
	}

	@Test
	void testSaveRuntime() {
		repo.saveRuntime(mrd);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "Runtime:" + moduleName)).isTrue();
	}

	@Test
	void testFindRuntimeByName() {
		repo.saveRuntime(mrd);
		assertThat(repo.findRuntimeByName(moduleName)).isEqualTo(mrd);
	}

	@Test
	void testDeleteRuntimeByName() {
		repo.saveRuntime(mrd);
		repo.deleteRuntimeByName(moduleName);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "Runtime:" + moduleName)).isFalse();
	}

	@Test
	void testSaveDealRecord() {
		repo.saveDealRecord(mdr);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "DealRecord:" + moduleName)).isTrue();
	}

	@Test
	void testFindAllDealRecords() {
		repo.saveDealRecord(mdr);
		assertThat(repo.findAllDealRecords(moduleName)).isNotEmpty();
	}

	@Test
	void testRemoveAllDealRecords() {
		repo.removeAllDealRecords(moduleName);
		assertThat(redisTemplate.hasKey(KEY_PREFIX + "DealRecord:" + moduleName)).isFalse();
	}

}
