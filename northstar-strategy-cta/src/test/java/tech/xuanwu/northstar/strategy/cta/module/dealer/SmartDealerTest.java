package tech.xuanwu.northstar.strategy.cta.module.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.PriceType;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.cta.module.signal.CtaSignal;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

public class SmartDealerTest extends CommonParamTest {
	
	SmartDealer dealer = new SmartDealer();
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	final static String USYMBOL = "rb2210@SHFE@FUTURES";
	final static String SYMBOL = "rb2210";

	@Before
	public void setUp() throws Exception {
		dealer.bindedUnifiedSymbol = USYMBOL;
		dealer.openVol = 1;
		dealer.priceTypeStr = PriceType.SIGNAL_PRICE;
		dealer.lastMinBar = BarField.newBuilder().build();
		dealer.lossToleranceInTick = 20;
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.getModuleName()).thenReturn("testModule");
		ContractManager contractMgr = mock(ContractManager.class);
		when(contractMgr.getContract(USYMBOL)).thenReturn(ContractField.newBuilder()
				.setUnifiedSymbol(USYMBOL)
				.setPriceTick(1)
				.build());
		dealer.setContractManager(contractMgr);
		target = dealer;
	}
	
	@Test
	public void shouldGetNothingIfNoSignal() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.baseline = 0;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
	@Test
	public void shouldGetBuyOpenReqWhenEmptyIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1230).setOpenPrice(1232).build();
		dealer.currentSignal = mock(CtaSignal.class);
		when(dealer.currentSignal.isBuy()).thenReturn(true);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isPresent();
	}
	
	@Test
	public void shouldNotGetBuyOpenReqWhenEmptyIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1230).setOpenPrice(1232).build();
		dealer.currentSignal = mock(CtaSignal.class);
		when(dealer.currentSignal.isSell()).thenReturn(true);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
	@Test
	public void shouldGetSellOpenReqWhenEmptyIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1250).setOpenPrice(1240).build();
		dealer.currentSignal = mock(CtaSignal.class);
		when(dealer.currentSignal.isSell()).thenReturn(true);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isPresent();
	}
	
	@Test
	public void shouldNotGetSellOpenReqWhenEmptyIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1250).setOpenPrice(1240).build();
		dealer.currentSignal = mock(CtaSignal.class);
		when(dealer.currentSignal.isBuy()).thenReturn(true);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
	@Test
	public void shouldGetNothingWhenNotEmptyIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(false);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.PENDING_ORDER);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1250).build();
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
	@Test
	public void shouldGetBuyCloseReqWhenHoldingShortIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_SHORT)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_SHORT);
		dealer.baseline = 1234;
		dealer.numOfBarsForCurrentDay = 100;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isPresent();
	}
	
	@Test
	public void shouldGetSellCloseReqWhenHoldingLongIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_LONG);
		dealer.baseline = 1234;
		dealer.numOfBarsForCurrentDay = 100;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isPresent();
	}
	
	@Test
	public void shouldGetBuyCloseReqWhenWithinColdDownPeriodIfTriggerStopLoss() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_SHORT)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_SHORT);
		dealer.baseline = 1234;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1255))).isPresent();
	}
	
	@Test
	public void shouldGetSellCloseReqWhenWithinColdDownPeriodIfTriggerStopLoss() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_LONG);
		dealer.baseline = 1234;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1213))).isPresent();
	}
	
	@Test
	public void shouldGetNothingWhenWithinColdDownPeriodEvenAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_SHORT)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_SHORT);
		dealer.baseline = 1234;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1253))).isEmpty();
	}
	
	@Test
	public void shouldGetNothingWhenWithinColdDownPeriodEvenAcrossBaseline2() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_LONG);
		dealer.baseline = 1234;
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1215))).isEmpty();
	}
	
	@Test
	public void shouldNotGetBuyCloseReqWhenWithinColdDownPeriodIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_SHORT)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_SHORT);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1230).build();
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
	@Test
	public void shouldNotGetSellCloseReqWhenWithinColdDownPeriodIfAcrossBaseline() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.HOLDING_LONG);
		dealer.baseline = 1234;
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1250).build();
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
	@Test
	public void shouldUpdateBarCount() {
		dealer.numOfBarsForCurrentDay = 100;
		dealer.onTrade(TradeField.newBuilder().build());
		assertThat(dealer.barNumOfLastAction).isEqualTo(100);
	}
	
	@Test
	public void shouldUpdateBaseline() {
		CtaSignal signal = CtaSignal.builder().state(SignalOperation.BuyOpen).signalPrice(3234D).build();
		dealer.lastTick = factory.makeTickField(SYMBOL, 1234);
		dealer.onSignal(signal);
		assertThat(dealer.baseline).isEqualTo(3234D);
	}
	
	@Test
	public void shouldGetNotThingIfNotAnySignal() {
		dealer.moduleStatus = mock(ModuleStatus.class);
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		when(dealer.moduleStatus.getCurrentState()).thenReturn(ModuleState.EMPTY);
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1230).setOpenPrice(1232).build();
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1234))).isEmpty();
	}
	
}
