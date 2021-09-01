package tech.xuanwu.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModulePositionPO;
import tech.xuanwu.northstar.strategy.common.model.persistence.ModuleStatusPO;

public class MongoUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		ModuleStatusPO e = ModuleStatusPO.builder()
				.moduleName("test")
				.holdingTradingDay("20210608")
				.state(ModuleState.EMPTY)
				.positions(Lists.newArrayList(new ModulePositionPO()))
				.build();
		
		assertThat(MongoUtils.beanToDocument(e)).isOfAnyClassIn(Document.class);
		assertThat(MongoUtils.documentToBean(MongoUtils.beanToDocument(e), ModuleStatusPO.class)).isEqualTo(e);
	}

}
