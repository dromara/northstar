package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.main.persistence.po.ContractPO;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.ContractField;

class ProtoBeanUtilsTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() throws IOException {
		TestFieldFactory factory = new TestFieldFactory("testGateway");
		ContractField contract = factory.makeContract("rb2210");
		assertThat(ProtoBeanUtils.toPojoBean(ContractPO.class, contract)).isOfAnyClassIn(ContractPO.class);
	}
	
	@Test
	void testException() {
		assertThrows(IllegalArgumentException.class, ()->{			
			ProtoBeanUtils.toPojoBean(ContractPO.class, null);
		});
	}

	@Test
	void testException2() {
		TestFieldFactory factory = new TestFieldFactory("testGateway");
		ContractField contract = factory.makeContract("rb2210");
		assertThrows(IllegalArgumentException.class, ()->{			
			ProtoBeanUtils.toPojoBean(null, contract);
		});
	}
	
	@Test
	void testException3() throws IOException {
		assertThrows(IllegalArgumentException.class, ()->{			
			ProtoBeanUtils.toProtoBean(null, getClass());
		});
	}
	
	@Test
	void testException4() throws IOException {
		assertThrows(IllegalArgumentException.class, ()->{			
			ProtoBeanUtils.toProtoBean(ContractField.newBuilder(), null);
		});
	}
}
