package org.dromara.northstar.gateway.playback.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.common.model.core.Bar;
import org.junit.jupiter.api.Test;

class DataFrameTest {
	
	DataFrame<Bar> bars = new DataFrame<>(50000);

	@Test
	void test() {
		Bar bar1 = Bar.builder().actionTimestamp(50000).build();
		Bar bar2 = Bar.builder().actionTimestamp(1).build();
		bars.add(bar1);
		bars.add(bar2);
		
		assertThat(bars.items()).hasSize(1);
	}

}
