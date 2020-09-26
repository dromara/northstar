package tech.xuanwu.northstar.trader.domain.simulated;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import xyz.redtorch.pb.CoreField.AccountField;

public class GwPositionsTest {

	GwAccount account;
	
	GwPositions gwPositions;
	GwOrders gwOrders;
	
	@Before
	public void prepare() {
		AccountField af = AccountField.newBuilder()
				.setAccountId("test")
				.setGatewayId("test")
				.setDeposit(100000)
				.build();
		account = new GwAccount(af);
		
		gwPositions = new GwPositions();
	}
	
	Offset<Double> PRECISION = Offset.offset(0.001);
	
	@Test
	public void testAddPosition() {}
	
	@Test
	public void testReducePosition() {}
	
	@Test
	public void testFrozenPosition() {}
	
	@Test
	public void testUnfrozenPosition() {
		
	}
	
	@Test
	public void testTotalCloseProfit() {
		assertThat(gwPositions.getTotalCloseProfit()).isEqualTo(0);
	}
	
	@Test
	public void testTotalPositionProfit() {
		assertThat(gwPositions.getTotalPositionProfit()).isEqualTo(0);
	}
	
	@Test
	public void testTotalUseMargin() {
		assertThat(gwPositions.getTotalUseMargin()).isEqualTo(0);
	}
}
