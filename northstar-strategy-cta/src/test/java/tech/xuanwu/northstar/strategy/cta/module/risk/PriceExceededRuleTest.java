package tech.xuanwu.northstar.strategy.cta.module.risk;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.CommonParamTest;

public class PriceExceededRuleTest extends CommonParamTest {

	@Before
	public void setUp() throws Exception {
		target = new PriceExceededRule();
	}

	@After
	public void tearDown() throws Exception {
	}

}
