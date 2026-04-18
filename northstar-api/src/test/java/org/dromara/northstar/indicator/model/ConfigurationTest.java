package org.dromara.northstar.indicator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.ValueType;
import org.junit.jupiter.api.Test;

class ConfigurationTest {
	
	Contract c = Contract.builder().unifiedSymbol("testSymbol").build();
	
	@Test
	void testDefaultValue() {
		Configuration cfg = Configuration.builder().indicatorName("testIndicator").contract(c).build();
		assertThat(cfg.interval()).isEqualTo(Interval.M1);
		assertThat(cfg.valueType()).isEqualTo(ValueType.CLOSE);
		assertThat(cfg.cacheLength()).isEqualTo(16);
		assertThat(cfg.ifPlotPerBar()).isFalse();
		assertThat(cfg.visible()).isTrue();
	}
	
	@Test
	void testException() {
		assertThrows(IllegalArgumentException.class, () -> {
			Configuration.builder().build();
		});
	}

}
