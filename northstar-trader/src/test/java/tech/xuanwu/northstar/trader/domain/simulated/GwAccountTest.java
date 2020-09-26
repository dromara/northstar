package tech.xuanwu.northstar.trader.domain.simulated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import xyz.redtorch.pb.CoreField.AccountField;

public class GwAccountTest {
	
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
		
	}
	
	Offset<Double> PRECISION = Offset.offset(0.001);

	@Test
	public void testAccount() {
		gwPositions = mock(GwPositions.class);
		when(gwPositions.getTotalCloseProfit()).thenReturn(1000D);
		when(gwPositions.getTotalPositionProfit()).thenReturn(3333D);
		when(gwPositions.getTotalUseMargin()).thenReturn(6969D);
		gwOrders = mock(GwOrders.class);
		when(gwOrders.getTotalCommission()).thenReturn(123D);
		when(gwOrders.getTotalFrozenAmount()).thenReturn(7777D);
		
		account.setGwOrders(gwOrders);
		account.setGwPositions(gwPositions);
		
		assertThat(account.getAccount().getBalance()).isCloseTo(104210, PRECISION);
		assertThat(account.getAccount().getAvailable()).isCloseTo(89464, PRECISION);
		assertThat(account.getAccount().getMargin()).isCloseTo(14746, PRECISION);
		assertThat(account.getAccount().getCloseProfit()).isCloseTo(1000, PRECISION);
		assertThat(account.getAccount().getCommission()).isCloseTo(123, PRECISION);
		assertThat(account.getAccount().getDeposit()).isCloseTo(100000, PRECISION);
		assertThat(account.getAccount().getWithdraw()).isCloseTo(0, PRECISION);
		assertThat(account.getAccount().getPositionProfit()).isCloseTo(3333, PRECISION);
	}
	
	@Test
	public void testDeposit() {
		gwPositions = mock(GwPositions.class);
		when(gwPositions.getTotalCloseProfit()).thenReturn(0D);
		when(gwPositions.getTotalPositionProfit()).thenReturn(0D);
		when(gwPositions.getTotalUseMargin()).thenReturn(0D);
		gwOrders = mock(GwOrders.class);
		when(gwOrders.getTotalCommission()).thenReturn(0D);
		when(gwOrders.getTotalFrozenAmount()).thenReturn(0D);
		
		account.setGwOrders(gwOrders);
		account.setGwPositions(gwPositions);
		
		AccountField af = account.deposit(500);
		assertThat(af.getBalance()).isCloseTo(100500, PRECISION);
		assertThat(af.getAvailable()).isCloseTo(100500, PRECISION);
		assertThat(af.getMargin()).isCloseTo(0, PRECISION);
		assertThat(af.getCloseProfit()).isCloseTo(0, PRECISION);
		assertThat(af.getCommission()).isCloseTo(0, PRECISION);
		assertThat(af.getDeposit()).isCloseTo(100500, PRECISION);
		assertThat(af.getWithdraw()).isCloseTo(0, PRECISION);
		assertThat(af.getPositionProfit()).isCloseTo(0, PRECISION);
	}
	
	
	@Test
	public void testWithdraw() {
		gwPositions = mock(GwPositions.class);
		when(gwPositions.getTotalCloseProfit()).thenReturn(0D);
		when(gwPositions.getTotalPositionProfit()).thenReturn(0D);
		when(gwPositions.getTotalUseMargin()).thenReturn(0D);
		gwOrders = mock(GwOrders.class);
		when(gwOrders.getTotalCommission()).thenReturn(0D);
		when(gwOrders.getTotalFrozenAmount()).thenReturn(0D);
		
		account.setGwOrders(gwOrders);
		account.setGwPositions(gwPositions);
		
		AccountField af = account.withdraw(5000);
		assertThat(af.getBalance()).isCloseTo(95000, PRECISION);
		assertThat(af.getAvailable()).isCloseTo(95000, PRECISION);
		assertThat(af.getMargin()).isCloseTo(0, PRECISION);
		assertThat(af.getCloseProfit()).isCloseTo(0, PRECISION);
		assertThat(af.getCommission()).isCloseTo(0, PRECISION);
		assertThat(af.getDeposit()).isCloseTo(100000, PRECISION);
		assertThat(af.getWithdraw()).isCloseTo(5000, PRECISION);
		assertThat(af.getPositionProfit()).isCloseTo(0, PRECISION);
	}
	
	@Test
	public void testProceedDailySettlement() {
		
	}
}
