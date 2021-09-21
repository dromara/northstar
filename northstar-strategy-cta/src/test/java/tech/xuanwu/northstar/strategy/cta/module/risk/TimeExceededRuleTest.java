package tech.xuanwu.northstar.strategy.cta.module.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class TimeExceededRuleTest extends CommonParamTest{

	
	TimeExceededRule rule = new TimeExceededRule();
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	@Before
	public void setUp() throws Exception {
		rule.timeoutSeconds = 10;
		target = rule;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldAcceptIfNotTimeout() {
		rule.onSubmitOrder(SubmitOrderReqField.newBuilder().build());
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1234), mock(ModuleStatus.class))).isEqualTo(RiskAuditResult.ACCEPTED);
	}
	
	
	@Test
	public void shouldRetryIfTimeout() {
		rule.lastUpdateTime = System.currentTimeMillis() - (rule.timeoutSeconds + 1) * 1000;
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1234), mock(ModuleStatus.class))).isEqualTo(RiskAuditResult.RETRY);
	}
}
