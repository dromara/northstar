package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class MongoUtilsTest {

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
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
