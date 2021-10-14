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
		assertThat(policy.immedidateSignalQ).hasSize(1);
		assertThat(policy.immedidateSignalQ.peek().price()).isEqualTo(3124);
		assertThat(policy.immedidateSignalQ.peek().stopPrice()).isEqualTo(3100);
		assertThat(policy.immedidateSignalQ.peek().isBuy()).isTrue();
	}

	@Test
	public void shouldConvertSellOpenSignal() {
		policy.onExtMsg("rb2210:空开3124,止损4000");
		assertThat(policy.immedidateSignalQ).hasSize(1);
		assertThat(policy.immedidateSignalQ.peek().isSell()).isTrue();
	}
	
	@Test
	public void shouldConvertBuyCloseSignal() {
		policy.onExtMsg("rb2210:多平3124");
		assertThat(policy.immedidateSignalQ).hasSize(1);
		assertThat(policy.immedidateSignalQ.peek().price()).isEqualTo(3124);
		assertThat(policy.immedidateSignalQ.peek().stopPrice()).isZero();
		assertThat(policy.immedidateSignalQ.peek().isBuy()).isTrue();
	}
	
	@Test
	public void shouldConvertSellCloseSignal() {
		policy.onExtMsg("rb2210：空平3124");
		assertThat(policy.immedidateSignalQ).hasSize(1);
		assertThat(policy.immedidateSignalQ.peek().isSell()).isTrue();
	}
	
	@Test
	public void shouldConvertStopProfitSignal() {
		policy.onExtMsg("rb2210:止盈3125");
		assertThat(policy.delaySignalQ).hasSize(1);
		assertThat(policy.delaySignalQ.peek().isBuy()).isFalse();
		assertThat(policy.delaySignalQ.peek().isSell()).isFalse();
		assertThat(policy.delaySignalQ.peek().price()).isEqualTo(3125);
	}
	

}
