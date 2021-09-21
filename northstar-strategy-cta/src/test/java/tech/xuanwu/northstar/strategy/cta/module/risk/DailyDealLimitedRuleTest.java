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
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class DailyDealLimitedRuleTest extends CommonParamTest{

	DailyDealLimitedRule rule = new DailyDealLimitedRule();
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	@Before
	public void setUp() throws Exception {
		rule.dailyDealLimit = 4;
		target = rule;
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	@Test
	public void shouldAcceptIfWithinLimit() {
		ModuleStatus status = mock(ModuleStatus.class);
		when(status.getCountOfOpeningToday()).thenReturn(3);
		rule.onSubmitOrder(SubmitOrderReqField.newBuilder().build());
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1234), status)).isEqualTo(RiskAuditResult.ACCEPTED);
	}

	@Test
	public void shouldRejectIfBeyondLimit() {
		ModuleStatus status = mock(ModuleStatus.class);
		when(status.getCountOfOpeningToday()).thenReturn(4);
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1234), status)).isEqualTo(RiskAuditResult.REJECTED);
	}
}
