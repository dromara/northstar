package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;

import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.data.IModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@DataJpaTest
class ModuleRepoAdapterTest {
	
	@Autowired
	ModuleDealRecordRepository mdrDelegate;
	@Autowired
	ModuleDescriptionRepository mdDelegate;
	@Autowired
	ModuleRuntimeDescriptionRepository mrdDelegate;

	static IModuleRepository repo;
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	TradeField openTrade = fieldFactory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = fieldFactory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
	
	String moduleName = "testModule";
	
	ModuleAccountDescription mad = ModuleAccountDescription.builder()
			.accountGatewayId("testGateway")
			.bindedContracts(List.of(ContractSimpleInfo.builder().value("rb2210@SHFE@FUTURES").build()))
			.build();
	
	ModuleDescription md = ModuleDescription.builder()
			.moduleName(moduleName)
			.type(ModuleType.SPECULATION)
			.moduleAccountSettingsDescription(List.of(mad))
			.build();
	
	ModuleAccountRuntimeDescription mard = ModuleAccountRuntimeDescription.builder()
			.initBalance(100000)
			.build();
	
	ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
			.moduleName(moduleName)
			.moduleAccountRuntime(mard)
			.build();
	
	ModuleDealRecord mdr = ModuleDealRecord.builder()
			.moduleName(moduleName)
			.openTrade(openTrade.toByteArray())
			.closeTrade(closeTrade.toByteArray())
			.build();
	
	@BeforeEach
	void prepare() {
		repo = new ModuleRepoAdapter(mdDelegate, mrdDelegate, mdrDelegate);
	}
	
	@Test
	void testSaveSettings() {
		assertDoesNotThrow(() -> {
			repo.saveSettings(md);
		});
	}

	@Test
	void testFindSettingsByName() {
		testSaveSettings();
		assertThat(repo.findSettingsByName(moduleName)).isEqualTo(md);
	}

	@Test
	void testFindAllSettings() {
		testSaveSettings();
		assertThat(repo.findAllSettings()).isNotEmpty();
	}

	@Test
	void testDeleteSettingsByName() {
		testSaveSettings();
		repo.deleteSettingsByName(moduleName);
		assertThat(repo.findAllSettings()).isEmpty();
	}

	@Test
	void testSaveRuntime() {
		assertDoesNotThrow(() -> {
			repo.saveRuntime(mrd);
		});
	}

	@Test
	void testFindRuntimeByName() {
		testSaveRuntime();
		assertThat(repo.findRuntimeByName(moduleName)).isEqualTo(mrd);
	}

	@Test
	void testDeleteRuntimeByName() {
		testSaveRuntime();
		repo.deleteRuntimeByName(moduleName);
		assertThrows(NoSuchElementException.class, () -> {
			repo.findRuntimeByName(moduleName);
		});
	}

	@Test
	void testSaveDealRecord() {
		assertDoesNotThrow(() -> {
			repo.saveDealRecord(mdr);
		});
	}

	@Test
	void testFindAllDealRecords() {
		testSaveDealRecord();
		assertThat(repo.findAllDealRecords(moduleName)).isNotEmpty();
	}

	@Test
	void testRemoveAllDealRecords() {
		testSaveDealRecord();
		repo.removeAllDealRecords(moduleName);
		assertThat(repo.findAllDealRecords(moduleName)).isEmpty();
	}

}
