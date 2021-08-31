package tech.xuanwu.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.main.persistence.po.ContractPO;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.ContractField;

public class ProtoBeanUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		TestFieldFactory factory = new TestFieldFactory("testGateway");
		ContractField contract = factory.makeContract("rb2210");
		assertThat(ProtoBeanUtils.toPojoBean(ContractPO.class, contract)).isOfAnyClassIn(ContractPO.class);
	}

}
