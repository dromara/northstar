package tech.xuanwu.northstar.strategy.cta.module.signal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;
import common.TestDataFactory;
import tech.xuanwu.northstar.common.utils.CommonUtils;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;
import xyz.redtorch.pb.CoreField.TickField;

public class PAExtSignalPolicyTest extends CommonParamTest{
	
	private PAExtSignalPolicy policy = new PAExtSignalPolicy();
	
	private TestDataFactory factory = new TestDataFactory("testModule");
	
	@Before
	public void setup() {
		PAExtSignalPolicy.InitParams initParam = new PAExtSignalPolicy.InitParams();
		initParam.bindedUnifiedSymbol = "rb2110@SHFE@FUTURES";
		policy.initWithParams(initParam);
		policy.setModuleStatus(factory.makeModuleStatus(ModuleState.HOLDING_LONG));
		target = policy;
		policy.currentTick = TickField.newBuilder().build();
	}

	@Test
	public void shouldGetSellCloseAndSellOpenSignal() {
		String textReverse = "【XX期货】：RB2110在2859.0的价格平多单，在2859.0的价格开空单，止损价：2904.0，目前持有空单1手（-1），仅供参考。";
		policy.onExtMsg(textReverse);
		Optional<Signal> signal = policy.onTick(5000, mock(BarData.class));
		assertThat(signal.get().isOpening()).isFalse();
		assertThat(((CtaSignal)signal.get()).getState()).isEqualTo(SignalOperation.SellClose);
		Optional<Signal> signal2 = policy.onTick(5000, mock(BarData.class));
		assertThat(((CtaSignal)signal2.get()).getState()).isEqualTo(SignalOperation.SellOpen);
		assertThat(CommonUtils.isEquals(((CtaSignal)signal2.get()).getSignalPrice(), 2859)).isTrue();
		assertThat(CommonUtils.isEquals(((CtaSignal)signal2.get()).getStopPrice(), 2904)).isTrue();
	}
	
	@Test
	public void shouldGetBuyOpenSignal() {
		String textOpen = "【XX期货】：RB2110在5314.0的价格开多单，止损价：5271.0，目前持有多单1手（+1），仅供参考。";
		policy.onExtMsg(textOpen);
		Optional<Signal> signal6 = policy.onTick(5000, mock(BarData.class));
		assertThat(signal6.get().isOpening()).isTrue();
		assertThat(((CtaSignal)signal6.get()).getState()).isEqualTo(SignalOperation.BuyOpen);
		assertThat(CommonUtils.isEquals(((CtaSignal)signal6.get()).getSignalPrice(), 5314)).isTrue();
		assertThat(CommonUtils.isEquals(((CtaSignal)signal6.get()).getStopPrice(), 5271)).isTrue();
	}

	@Test
	public void shouldGetSellOpenSignal() {
		policy.setModuleStatus(factory.makeModuleStatus(ModuleState.EMPTY));
		String textReverse = "【XX期货】：RB2110在2859.0的价格平多单，在2859.0的价格开空单，止损价：2904.0，目前持有空单1手（-1），仅供参考。";
		policy.onExtMsg(textReverse);
		Optional<Signal> signal3 = policy.onTick(5000, mock(BarData.class));
		assertThat(signal3.get().isOpening()).isTrue();
		assertThat(((CtaSignal)signal3.get()).getState()).isEqualTo(SignalOperation.SellOpen);
		Optional<Signal> signal4 = policy.onTick(5000, mock(BarData.class));
		assertThat(signal4).isEmpty();
	}
	
	@Test
	public void shouldGetBuyCloseSignal() {
		String textClose = "赢家信息：RB2110在5314.0的价格平空单，仅供参考。";
		policy.onExtMsg(textClose);
		Optional<Signal> signal = policy.onTick(5000, mock(BarData.class));
		assertThat(signal.get().isOpening()).isFalse();
		assertThat(((CtaSignal)signal.get()).getState()).isEqualTo(SignalOperation.BuyClose);
		assertThat(CommonUtils.isEquals(((CtaSignal)signal.get()).getSignalPrice(), 5314)).isTrue();
	}
	
	@Test
	public void shouldDoNothing() {
		String textClose = "随便写点东西";
		policy.onExtMsg(textClose);
		Optional<Signal> signal = policy.onTick(5000, mock(BarData.class));
		assertThat(signal).isEmpty();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailInitIfParamNotSet() {
		DynamicParams params = policy.getDynamicParams();
		policy.initWithParams(params);
	}
}
