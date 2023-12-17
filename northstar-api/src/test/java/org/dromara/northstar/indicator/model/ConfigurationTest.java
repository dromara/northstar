package org.dromara.northstar.indicator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.constant.ValueType;
import org.junit.jupiter.api.Test;

class ConfigurationTest {
	
	Contract c = Contract.builder().unifiedSymbol("testSymbol").build();
	
	@Test
	void testDefaultValue() {
		Configuration cfg = Configuration.builder().indicatorName("testIndicator").contract(c).build();
		assertThat(cfg.numOfUnits()).isEqualTo(1);
		assertThat(cfg.period()).isEqualTo(PeriodUnit.MINUTE);
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
