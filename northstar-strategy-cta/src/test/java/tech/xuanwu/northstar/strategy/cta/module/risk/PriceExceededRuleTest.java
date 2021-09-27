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
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class PriceExceededRuleTest extends CommonParamTest {

	PriceExceededRule rule = new PriceExceededRule();
	
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
		rule.priceDifTolerance = 20;
		target = rule;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldRejectIfPriceExceedWhenBuy() {
		ModuleStatus status = mock(ModuleStatus.class);
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.setPrice(1234)
				.setVolume(1)
				.build();
		rule.onSubmitOrder(orderReq);
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1255), status)).isEqualTo(RiskAuditResult.REJECTED);
	}
	

	@Test
	public void shouldRejectIfPriceExceedWhenSell() {
		ModuleStatus status = mock(ModuleStatus.class);
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setDirection(DirectionEnum.D_Sell)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.setPrice(1234)
				.setVolume(1)
				.build();
		rule.onSubmitOrder(orderReq);
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1213), status)).isEqualTo(RiskAuditResult.REJECTED);
	}
	
	@Test
	public void shouldAcceptIfPriceWithinRange() {
		ModuleStatus status = mock(ModuleStatus.class);
		SubmitOrderReqField orderReq = SubmitOrderReqField.newBuilder()
				.setContract(contract)
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.setPrice(1234)
				.setVolume(1)
				.build();
		rule.onSubmitOrder(orderReq);
		assertThat(rule.canDeal(factory.makeTickField("rb2210", 1254), status)).isEqualTo(RiskAuditResult.ACCEPTED);
	}
}
