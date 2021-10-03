package tech.xuanwu.northstar.strategy.cta.module.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.cta.module.signal.CtaSignal;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;

public class SampleDealerTest extends CommonParamTest {
	
	SampleDealer dealer = new SampleDealer();
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	final static String SYMBOL = "rb2210@SHFE@FUTURES";
	
	CtaSignal signal = CtaSignal.builder()
			.signalPrice(1100)
			.state(SignalOperation.BuyOpen)
			.build();

	@Before
	public void setUp() throws Exception {
		dealer.bindedUnifiedSymbol = SYMBOL;
		dealer.openVol = 1;
		ContractManager contractMgr = mock(ContractManager.class);
		when(contractMgr.getContract(SYMBOL)).thenReturn(ContractField.newBuilder()
				.setUnifiedSymbol(SYMBOL)
				.setPriceTick(1)
				.build());
		dealer.setContractManager(contractMgr);
		target = dealer;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void resolveOpponentPrice() {
		dealer.priceTypeStr = "对手价";
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(1235D, Offset.offset(1e-6));
	}
	
	@Test
	public void resolveAnyPrice() {
		dealer.priceTypeStr = "市价";
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(0, Offset.offset(1e-6));
	}
	
	@Test
	public void resolveLastPrice() {
		dealer.priceTypeStr = "最新价";
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(1234D, Offset.offset(1e-6));
		
		dealer.overprice = 3;
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(1237D, Offset.offset(1e-6));
	}
	
	@Test
	public void resolveQueuePrice() {
		dealer.priceTypeStr = "排队价";
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(1233D, Offset.offset(1e-6));
	}
	
	@Test
	public void resolveSignalPrice() {
		dealer.priceTypeStr = "信号价";
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(1100, Offset.offset(1e-6));
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowIfPriceTypeUnidentified() {
		dealer.priceTypeStr = "未知价";
		assertThat(dealer.resolvePrice(signal, factory.makeTickField("rb2210", 1234))).isCloseTo(1100, Offset.offset(1e-6));
	}
	
	@Test
	public void verifyBindedSymbol() {
		assertThat(dealer.bindedUnifiedSymbols().contains(SYMBOL)).isTrue();
	}
	
	@Test
	public void shouldGetSubmitOrderReqWhenReceivingSignal() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.priceTypeStr = "信号价";
		dealer.onSignal(signal);
		assertThat(dealer.onTick(factory.makeTickField("rb2210", 1234))).isPresent();
	}
	
	@Test
	public void shouldModifyOrderReq() {
		shouldGetSubmitOrderReqWhenReceivingSignal();
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.PLACING_ORDER)).thenReturn(true);
		assertThat(dealer.onTick(factory.makeTickField("rb2210", 1234))).isPresent();
	}
}
