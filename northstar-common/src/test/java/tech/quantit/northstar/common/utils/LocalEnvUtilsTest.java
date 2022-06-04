package tech.quantit.northstar.common.utils;

import org.junit.jupiter.api.Test;

class LocalEnvUtilsTest {

	@Test
	void testMAC() throws Exception {
		System.out.println(LocalEnvUtils.getMACAddress());
	}

}
