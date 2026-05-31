package org.dromara.northstar.common.model.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubmitOrderReqTest {

	@Test
	void test() {
		assertThat(SubmitOrderReq.builder().build().originOrderId()).isNotEmpty();
	}

}
