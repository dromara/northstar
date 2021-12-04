package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.quantit.northstar.main.utils.CodecUtils;

public class CodecUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String message = "hello world";
		assertThat(CodecUtils.encrypt(message)).isNotEqualTo(message);
		assertThat(CodecUtils.decrypt(CodecUtils.encrypt(message))).isEqualTo(message);
	}

}
