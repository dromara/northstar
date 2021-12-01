package tech.xuanwu.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.strategy.api.model.ModuleDealRecord;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class MongoUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		ModuleDealRecord e = ModuleDealRecord.builder()
				.openPrice(100)
				.closePrice(1000)
				.contractName("test")
				.direction(PositionDirectionEnum.PD_Long)
				.build();
		
		assertThat(MongoUtils.beanToDocument(e)).isOfAnyClassIn(Document.class);
		assertThat(MongoUtils.documentToBean(MongoUtils.beanToDocument(e), ModuleDealRecord.class)).isEqualTo(e);
	}

}
