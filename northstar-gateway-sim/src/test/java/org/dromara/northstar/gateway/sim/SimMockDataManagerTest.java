package org.dromara.northstar.gateway.sim;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.gateway.sim.market.SimTickGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

class SimMockDataManagerTest {
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	SimMockDataManager dsMgr;
	
	ContractField contract = factory.makeContract("sim999");
	
	@BeforeEach
	void prepare() {
		Map<String, SimTickGenerator> tickGenMap = new HashMap<>();
		tickGenMap.put(contract.getUnifiedSymbol(), new SimTickGenerator(contract));
		dsMgr = new SimMockDataManager(tickGenMap);
	}

	@Test
	void test() {
		List<BarField> bars = dsMgr.getMinutelyData(contract, LocalDate.now().minusDays(7), LocalDate.now());
		assertThat(bars).hasSize(60*24);
	}

}
