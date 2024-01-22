package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.IMarketCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class SimTradeGatewayLocalTest {
	
	private SimTradeGatewayLocal gateway;
	
	Contract c1 = Contract.builder().symbol("rb2205@SHFE").multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08)
			.contractDefinition(ContractDefinition.builder().commissionFee(2).build()).build();
	SubmitOrderReq submitOrder = SubmitOrderReq.builder()
			.contract(c1)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Close)
			.volume(1)
			.build();
	
	@BeforeEach
	void prepare() {
		FastEventEngine feEngine = mock(FastEventEngine.class);
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("gatewayId")
				.bindedMktGatewayId("bindedGatewayId")
				.build();
		SimGatewayAccount simAccount = mock(SimGatewayAccount.class);
		PositionManager posMgr = mock(PositionManager.class);
		when(simAccount.getPositionManager()).thenReturn(posMgr);
		when(posMgr.positionFields()).thenReturn(List.of());
		gateway = new SimTradeGatewayLocal(feEngine, gd, simAccount, mock(ISimAccountRepository.class), mock(IMarketCenter.class));
	}

	@Test
	void testConnectAndDisConnect() {
		gateway.connect();
		assertThat(gateway.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);
		
		gateway.disconnect();
		assertThat(gateway.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
	}

	@Test
	void testSubmitOrder() {
		gateway.connect();
		assertDoesNotThrow(() -> {			
			gateway.submitOrder(submitOrder);
		});
	}

	@Test
	void testCancelOrder() {
		gateway.connect();
		String id = gateway.submitOrder(submitOrder);
		assertDoesNotThrow(() -> {			
			gateway.cancelOrder(id);
		});
	}

	@Test
	void testMoneyIO() {
		gateway.moneyIO(1);
		verify(gateway.account).onDeposit(1);
		gateway.moneyIO(-1);
		verify(gateway.account).onWithdraw(1);
	}
	
	@Test
	void testMoneyIO2() {
		gateway.moneyIO(-1);
		verify(gateway.account).onWithdraw(1);
	}

	@Test
	void testGetAuthErrorFlag() {
		assertThat(gateway.getAuthErrorFlag()).isFalse();
	}

}
