package org.dromara.northstar.common.utils;

import org.dromara.northstar.common.utils.LocalEnvUtils;
import org.junit.jupiter.api.Test;

class LocalEnvUtilsTest {

	@Test
	void testMAC() throws Exception {
		System.out.println(LocalEnvUtils.getMACAddress());
	}

}
