package tech.xuanwu.northstar.strategy.cta.module.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import common.CommonParamTest;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.PriceType;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.data.SimpleBar;
import tech.xuanwu.northstar.strategy.cta.module.signal.CtaSignal;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class SmartDealerTest extends CommonParamTest {
	
	SmartDealer dealer = new SmartDealer();
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	final static String USYMBOL = "rb2210@SHFE@FUTURES";
	final static String SYMBOL = "rb2210";

	@Before
	public void setUp() throws Exception {
		dealer.bindedUnifiedSymbol = USYMBOL;
		dealer.openVol = 1;
		dealer.signalAccordanceTimeout = 1; //1秒
		dealer.numberOfTickForSafeZoon = 10; //10TICK
		dealer.periodToleranceInDangerZoon = 1; //1秒
		dealer.priceTypeStr = PriceType.SIGNAL_PRICE;
		dealer.lastMinBar = BarField.newBuilder().build();
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
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 0))).isEmpty();
	}
	
	@Test
	public void shouldGetOpenOrderReqIfAcrossAfterSignal() throws InterruptedException {
		when(dealer.moduleStatus.at(ModuleState.PLACING_ORDER)).thenReturn(true);
		dealer.lastTick = factory.makeTickField(SYMBOL, 1234);
		dealer.onSignal(CtaSignal.builder()
				.state(SignalOperation.BuyOpen)
				.signalPrice(1234)
				.build());
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1233).build();
		Optional<SubmitOrderReqField> orderReq = dealer.onTick(factory.makeTickField(SYMBOL, 1234));
		assertThat(orderReq).isPresent();
		assertThat(orderReq.get().getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		assertThat(orderReq.get().getPrice()).isZero(); //市价单
	}
	
	@Test
	public void shouldGetOpenOrderReqIfTimeoutAfterSignal() throws InterruptedException {
		when(dealer.moduleStatus.at(ModuleState.PLACING_ORDER)).thenReturn(true);
		dealer.lastTick = factory.makeTickField(SYMBOL, 1234);
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1240).build();
		dealer.onSignal(CtaSignal.builder()
				.state(SignalOperation.BuyOpen)
				.signalPrice(1234)
				.build());
		Thread.sleep(1100);
		Optional<SubmitOrderReqField> orderReq = dealer.onTick(factory.makeTickField(SYMBOL, 1260));
		assertThat(orderReq).isPresent();
		assertThat(orderReq.get().getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		assertThat(orderReq.get().getPrice()).isEqualTo(1260); // 限价单
	}
	
	@Test
	public void shouldGetOpenOrderIfLastSignalExist() {
		when(dealer.moduleStatus.at(ModuleState.EMPTY)).thenReturn(true);
		dealer.baseline = 1234;
		dealer.lastSignal = CtaSignal.builder()
				.state(SignalOperation.BuyOpen)
				.signalPrice(1234)
				.build();
		dealer.lastMinBar = BarField.newBuilder().setClosePrice(1233).build();
		Optional<SubmitOrderReqField> orderReq = dealer.onTick(factory.makeTickField(SYMBOL, 1234));
		assertThat(orderReq).isPresent();
		assertThat(orderReq.get().getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_Open);
		assertThat(orderReq.get().getPrice()).isZero(); // 市价单
	}
	
	@Test
	public void shouldGetCloseOrderIfReceivedCloseSignal() {
		when(dealer.moduleStatus.at(ModuleState.PLACING_ORDER)).thenReturn(true);
		when(dealer.moduleStatus.isSameDayHolding(ArgumentMatchers.anyString())).thenReturn(true);
		dealer.onSignal(CtaSignal.builder()
				.state(SignalOperation.SellClose)
				.signalPrice(1234)
				.build());
		Optional<SubmitOrderReqField> orderReq = dealer.onTick(factory.makeTickField(SYMBOL, 1260));
		assertThat(orderReq).isPresent();
		assertThat(orderReq.get().getOffsetFlag()).isEqualTo(OffsetFlagEnum.OF_CloseToday);
		assertThat(orderReq.get().getPrice()).isZero();
	}
	
	@Test
	public void shouldGetCloseOrderIfTimeoutInDangerZoon() throws InterruptedException {
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.isSameDayHolding(ArgumentMatchers.anyString())).thenReturn(true);
		
		dealer.lastSignal = CtaSignal.builder()
				.state(SignalOperation.BuyOpen)
				.signalPrice(1234)
				.build();
		
		dealer.holdingProfitBar = mock(SimpleBar.class);
		when(dealer.holdingProfitBar.actualDiff()).thenReturn(-1D);
		Thread.sleep(1100);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1243))).isPresent();
	}
	
	@Test
	public void shouldGetCloseOrderIfProfitRetrieve() throws InterruptedException {
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.isSameDayHolding(ArgumentMatchers.anyString())).thenReturn(true);
		dealer.lastSignal = CtaSignal.builder()
				.state(SignalOperation.BuyOpen)
				.signalPrice(1234)
				.build();
		dealer.holdingProfitBar = mock(SimpleBar.class);
		when(dealer.holdingProfitBar.upperShadow()).thenReturn(26D);
		when(dealer.holdingProfitBar.barRange()).thenReturn(100D);
		when(dealer.holdingProfitBar.getHigh()).thenReturn(50D);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1300))).isPresent();
	}

	@Test
	public void shouldGetNothingIfTimeoutInSafeZoon() throws InterruptedException {
		when(dealer.moduleStatus.at(ModuleState.HOLDING_LONG)).thenReturn(true);
		when(dealer.moduleStatus.isSameDayHolding(ArgumentMatchers.anyString())).thenReturn(true);
		when(dealer.moduleStatus.getHoldingProfit()).thenReturn(110D);
		dealer.holdingProfitBar = new SimpleBar(0);
		dealer.lastSignal = CtaSignal.builder()
				.state(SignalOperation.BuyOpen)
				.signalPrice(1234)
				.build();
		dealer.baseline = 1234;
		Thread.sleep(1100);
		assertThat(dealer.onTick(factory.makeTickField(SYMBOL, 1245))).isEmpty();
	}
	
	
	//当自行裁量期超时时，如果未触发信号止损价，应该发单，否则不发单
}
