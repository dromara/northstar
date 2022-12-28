package tech.quantit.northstar.gateway.ctp.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.gateway.api.domain.contract.ContractDefinition;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelper;
import test.common.TestFieldFactory;

class CnFtPeriodHelperFactoryTest {
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	CnFtPeriodHelperFactory phFactory = new CnFtPeriodHelperFactory();
	
	ContractDefinition cd = ContractDefinition.builder()
			.symbolPattern(Pattern.compile("rb[0-9]{3,4}@.+"))
			.tradeTimeType("CN_FT_TT1")
			.build();
	
	@Test
	void test() {
		PeriodHelper h1 = phFactory.newInstance(5, false, cd);
		PeriodHelper h2 = phFactory.newInstance(5, false, cd);
		PeriodHelper h4 = phFactory.newInstance(5, false, cd);
		assertThat(h1 == h2).isTrue();
		assertThat(h1 == h4).isTrue();
		
		PeriodHelper h3 = phFactory.newInstance(5, true, cd);
		assertThat(h1 != h3).isTrue();
	}

}
