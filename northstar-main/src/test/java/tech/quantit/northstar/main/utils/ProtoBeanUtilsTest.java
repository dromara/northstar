package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.quantit.northstar.main.persistence.po.ContractPO;
import tech.quantit.northstar.main.utils.ProtoBeanUtils;
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
	
	@Test(expected = IllegalArgumentException.class)
	public void testException() {
		ProtoBeanUtils.toPojoBean(ContractPO.class, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException2() {
		TestFieldFactory factory = new TestFieldFactory("testGateway");
		ContractField contract = factory.makeContract("rb2210");
		ProtoBeanUtils.toPojoBean(null, contract);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testException3() throws IOException {
		ProtoBeanUtils.toProtoBean(null, getClass());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testException4() throws IOException {
		ProtoBeanUtils.toProtoBean(ContractField.newBuilder(), null);
	}
}
