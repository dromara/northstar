package org.dromara.northstar.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.support.utils.CodecUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CodecUtilsTest {

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String message = "hello world";
		assertThat(CodecUtils.encrypt(message)).isNotEqualTo(message);
		assertThat(CodecUtils.decrypt(CodecUtils.encrypt(message))).isEqualTo(message);
	}

}
