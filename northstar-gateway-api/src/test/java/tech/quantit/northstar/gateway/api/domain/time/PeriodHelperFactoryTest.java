package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.model.ContractDefinition;
import test.common.TestFieldFactory;

class PeriodHelperFactoryTest {

	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	PeriodHelperFactory phFactory;
	
	
	@BeforeEach
	void prepare() {
		ContractDefinition cd = ContractDefinition.builder()
				.gatewayType("CTP")
				.symbolPattern(Pattern.compile("rb[0-9]{3,4}@.+"))
				.tradeTimeType("CN_FT_TT1")
				.build();
		phFactory = new PeriodHelperFactory(List.of(cd));
	}
	
	@Test
	void test() {
		PeriodHelper h1 = phFactory.newInstance(5, false, factory.makeContract("rb2301"));
		PeriodHelper h2 = phFactory.newInstance(5, false, factory.makeContract("rb2301"));
		PeriodHelper h4 = phFactory.newInstance(5, false, factory.makeContract("rb2311"));
		assertThat(h1 == h2).isTrue();
		assertThat(h1 == h4).isTrue();
		
		PeriodHelper h3 = phFactory.newInstance(5, true, factory.makeContract("rb2301"));
		assertThat(h1 != h3).isTrue();
	}

}
