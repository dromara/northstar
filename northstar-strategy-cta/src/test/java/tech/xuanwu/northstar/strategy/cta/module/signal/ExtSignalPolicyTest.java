package tech.xuanwu.northstar.strategy.cta.module.signal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;
import common.TestDataFactory;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;

public class ExtSignalPolicyTest extends CommonParamTest{

	private TestDataFactory factory = new TestDataFactory("testModule");
	
	private ExtSignalPolicy policy = new ExtSignalPolicy();
	
	@Before
	public void setup() {
		ExtSignalPolicy.InitParams initParam = new ExtSignalPolicy.InitParams();
		initParam.bindedUnifiedSymbol = "rb2210@SHFE@FUTURES";
		policy.initWithParams(initParam);
		policy.setModuleStatus(factory.makeModuleStatus(ModuleState.EMPTY));
		target = policy;
	}
	
	@Test
	public void shouldConvertBuyOpenSignal() {
		policy.onExtMsg("RB2210：多开3124，止损3100");
		assertThat(policy.signalQ).hasSize(1);
		assertThat(policy.signalQ.peek().price()).isEqualTo(3124);
		assertThat(policy.signalQ.peek().stopPrice()).isEqualTo(3100);
	}

	@Test
	public void shouldConvertSellOpenSignal() {
		policy.onExtMsg("rb2210:空开3124,止损4000");
		assertThat(policy.signalQ).hasSize(1);
	}
	
	@Test
	public void shouldConvertBuyCloseSignal() {
		policy.onExtMsg("rb2210:多平3124");
		assertThat(policy.signalQ).hasSize(1);
		assertThat(policy.signalQ.peek().price()).isEqualTo(3124);
		assertThat(policy.signalQ.peek().stopPrice()).isZero();
	}
	
	@Test
	public void shouldConvertSellCloseSignal() {
		policy.onExtMsg("rb2210：空平3124");
		assertThat(policy.signalQ).hasSize(1);
	}
	
	@Test
	public void shouldConvertReverseBuySignal() {
		policy.onExtMsg("rb2210：反手开多4123");
		assertThat(policy.signalQ).hasSize(2);
	}
	
	@Test
	public void shouldConvertReverseSellSignal() {
		policy.onExtMsg("rb2210:反手开空5214");
		assertThat(policy.signalQ).hasSize(2);
	}
	
	

}
