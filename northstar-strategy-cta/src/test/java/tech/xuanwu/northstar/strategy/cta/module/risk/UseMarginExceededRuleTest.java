package tech.xuanwu.northstar.strategy.cta.module.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class UseMarginExceededRuleTest extends CommonParamTest{
	
	UseMarginExceededRule rule = new UseMarginExceededRule();
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	final static String SYMBOL = "rb2210@SHFE@FUTURES";
	
	ContractField contract = ContractField.newBuilder()
			.setMultiplier(10)
			.setUnifiedSymbol(SYMBOL)
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.build();

	@Before
	public void setUp() throws Exception {
		rule.limitedPercentageOfTotalBalance = 80;
		target = rule;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldAcceptIfHaveEnoughAmount() {
		ModuleStatus status = mock(ModuleStatus.class);
		when(status.getAccountAvailable()).thenReturn(10000D);
		rule.onSubmitOrder(SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setPrice(5000)
				.setVolume(2)
				.build());
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 5000), status)).isEqualTo(RiskAuditResult.ACCEPTED);
	}
	
	@Test
	public void shouldRejectIfNotEnoughAmount() {
		ModuleStatus status = mock(ModuleStatus.class);
		when(status.getAccountAvailable()).thenReturn(10000D);
		rule.onSubmitOrder(SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setPrice(5000)
				.setVolume(3)
				.build());
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 5000), status)).isEqualTo(RiskAuditResult.REJECTED);
	}

}
