package tech.quantit.northstar.data.mongo;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.ModuleType;
import tech.quantit.northstar.common.model.*;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.data.ISimAccountRepository;
import tech.quantit.northstar.data.mongo.po.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模拟账户服务测试
 * @author : wpxs
 */
public class ModuleRepoMongoImplTest {

	MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "TEST_NS_DB");

	IModuleRepository repo = new ModuleRepoMongoImpl(mongoTemplate);

	ModuleSettingsDescription msd1 = ModuleSettingsDescription.builder()
			.moduleName("test1")
			.strategySetting(ComponentAndParamsPair.builder().componentMeta(new ComponentMetaInfo("name1","className1")).build())
			.type(ModuleType.SPECULATION)
			.build();
	ModuleSettingsDescription msd2 = ModuleSettingsDescription.builder()
			.moduleName("test2")
			.strategySetting(ComponentAndParamsPair.builder().componentMeta(new ComponentMetaInfo("name2","className2")).build())
			.type(ModuleType.ARBITRAGE)
			.build();

	ModuleDescription md1 = ModuleDescription.builder()
			.moduleName("test1")
			.moduleState(ModuleState.HOLDING_LONG)
			.enabled(true)
			.positionDescription(ModulePositionDescription.builder().logicalPosition(0).logicalPositionProfit(1.0).build())
			.build();

	ModuleDescription md2 = ModuleDescription.builder()
			.moduleName("test2")
			.moduleState(ModuleState.HOLDING_LONG)
			.enabled(true)
			.positionDescription(ModulePositionDescription.builder().logicalPosition(0).logicalPositionProfit(1.0).build())
			.build();

	ModuleTradeRecord mt1 = ModuleTradeRecord.builder()
			.moduleName("test1")
			.actionDay("20220415")
			.contractName("A2205")
			.operation("buy")
			.price(100.0)
			.build();

	ModuleTradeRecord mt2 = ModuleTradeRecord.builder()
			.moduleName("test1")
			.actionDay("20220414")
			.contractName("A2204")
			.operation("buy")
			.price(100.0)
			.build();

	ModuleDealRecord mdr1 = ModuleDealRecord.builder()
			.moduleName("test1")
			.closeDate("20220415")
			.closePrice(10.0)
			.closeProfit(11)
			.contractName("A2205")
			.build();

	ModuleDealRecord mdr2 = ModuleDealRecord.builder()
			.moduleName("test1")
			.closeDate("20220413")
			.closePrice(10.0)
			.closeProfit(11)
			.contractName("A2204")
			.build();

	@AfterEach
	void clear() {
		mongoTemplate.dropCollection(ModuleSettingsDescriptionPO.class);
		mongoTemplate.dropCollection(ModuleDescriptionPO.class);
		mongoTemplate.dropCollection(ModuleTradeRecordPO.class);
		mongoTemplate.dropCollection(ModuleDealRecordPO.class);
	}

	@Test
	void testSaveSettings(){
		repo.saveSettings(msd1);
		assertThat(mongoTemplate.findAll(ModuleSettingsDescriptionPO.class)).hasSize(1);
	}

	@Test
	void testFindSettingsByName(){
		repo.saveSettings(msd1);
		ModuleSettingsDescription settingsByName = repo.findSettingsByName(msd1.getModuleName());
		assertThat(settingsByName).isNotNull();
	}

	@Test
	void testFindAll(){
		repo.saveSettings(msd1);
		repo.saveSettings(msd2);
		assertThat(repo.findAll()).hasSize(2);
	}

	@Test
	void testDeleteSettingsByName(){
		repo.saveSettings(msd1);
		repo.deleteSettingsByName(msd1.getModuleName());
		assertThat(mongoTemplate.findAll(ModuleSettingsDescriptionPO.class)).isEmpty();
	}

	@Test
	void testSave(){
		repo.save(md1);
		assertThat(mongoTemplate.findAll(ModuleDescriptionPO.class)).hasSize(1);
	}

	@Test
	void testFindByName(){
		repo.save(md1);
		ModuleDescription moduleDescription = repo.findByName(md1.getModuleName());
		assertThat(moduleDescription).isNotNull();
	}

	@Test
	void testDeleteByName(){
		repo.save(md1);
		repo.deleteByName(md1.getModuleName());
		assertThat(mongoTemplate.findAll(ModuleDescriptionPO.class)).isEmpty();
	}

	@Test
	void testSaveTradeRecord(){
		repo.saveTradeRecord(mt1);
		assertThat(mongoTemplate.findAll(ModuleTradeRecordPO.class)).hasSize(1);
	}

	@Test
	void testFindAllTradeRecords(){
		repo.saveTradeRecord(mt1);
		repo.saveTradeRecord(mt2);
		assertThat(repo.findAllTradeRecords(mt1.getModuleName())).hasSize(2);
	}

	@Test
	void testRemoveAllTradeRecords(){
		repo.saveTradeRecord(mt1);
		repo.removeAllTradeRecords(mt1.getModuleName());
		assertThat(mongoTemplate.findAll(ModuleTradeRecordPO.class)).isEmpty();
	}

	@Test
	void testSaveDealRecord(){
		repo.saveDealRecord(mdr1);
		assertThat(mongoTemplate.findAll(ModuleDealRecordPO.class)).hasSize(1);
	}

	@Test
	void testFindAllDealRecords(){
		repo.saveDealRecord(mdr1);
		repo.saveDealRecord(mdr2);
		assertThat(repo.findAllDealRecords(mdr1.getModuleName())).hasSize(2);
	}

	@Test
	void testRemoveAllDealRecords(){
		repo.saveDealRecord(mdr1);
		repo.removeAllDealRecords(mdr1.getModuleName());
		assertThat(mongoTemplate.findAll(ModuleDealRecordPO.class)).isEmpty();
	}
}
