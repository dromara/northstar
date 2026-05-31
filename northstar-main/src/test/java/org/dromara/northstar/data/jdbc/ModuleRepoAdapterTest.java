package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;

import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

@DataJpaTest
class ModuleRepoAdapterTest {

	@Autowired
	DealRecordRepository mdrDelegate;
	@Autowired
	ModuleDescriptionRepository mdDelegate;
	@Autowired
	ModuleRuntimeDescriptionRepository mrdDelegate;

	static IModuleRepository repo;

	Contract contract = Contract.builder().unifiedSymbol("rb2210").build();
	Trade openTrade = Trade.builder()
			.contract(contract)
			.volume(1000)
			.price(1)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.build();

	Trade closeTrade = Trade.builder()
			.contract(contract)
			.volume(1000)
			.price(1)
			.direction(DirectionEnum.D_Sell)
			.offsetFlag(OffsetFlagEnum.OF_Close)
			.build();

	String moduleName = "testModule";

	ModuleDescription md = ModuleDescription.builder()
			.name(moduleName)
			.type(ModuleType.SPECULATION)
			.bindedContracts(List.of(ContractSimpleInfo.builder().unifiedSymbol("rb2210@SHFE@FUTURES").build()))
			.build();
	
	ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
			.moduleName(moduleName)
			.build();

	@BeforeEach
	void prepare() {
		repo = new ModuleRepoAdapter(mdDelegate, mrdDelegate);
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

}
