package tech.xuanwu.northstar.gateway.sim;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.engine.event.FastEventEngine;
import xyz.redtorch.pb.CoreEnum.GatewayTypeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;

public class SimGatewayTest {
	
	private SimGateway gateway;
	
	@Before
	public void prepare() {
		FastEventEngine feEngine = mock(FastEventEngine.class);
		GatewaySettingField gwSettings = GatewaySettingField.newBuilder()
				.setGatewayId("testGateway")
				.setGatewayType(GatewayTypeEnum.GTE_Trade)
				.build();
		Map<String, ContractField> contractMap = mock(Map.class);
		SimFactory simFactory = new SimFactory("testGateway", feEngine, 1, contractMap);
		gateway = new SimGatewayLocalImpl(feEngine, gwSettings, simFactory.newGwAccountHolder());
	}

	@Test
	public void testMoneyIO() {
		gateway.moneyIO(2000);
		
		
	}

	@Test
	public void testOnTickWhileEmpty() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testOnTickWhileHolding() {
		fail("Not yet implemented");
	}

	@Test
	public void testSubmitAndCancelOrder() {
		fail("Not yet implemented");
	}

	@Test
	public void testSubmitOrderAndDealOnEmpty() {
		fail("Not yet implemented");
	}

	
	@Test
	public void testSubmitOrderAndDealOnHolding() {
		fail("Not yet implemented");
	}
}
