package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodecUtilsTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		String message = "hello world";
		assertThat(CodecUtils.encrypt(message)).isNotEqualTo(message);
		assertThat(CodecUtils.decrypt(CodecUtils.encrypt(message))).isEqualTo(message);
	}

}
