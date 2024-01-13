package org.dromara.northstar.indicator;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.junit.jupiter.api.Test;

class AbstractIndicatorTest {
	
	Contract c = Contract.builder().unifiedSymbol("rb2401@Test").build();
	
	TestIndicator tester = new TestIndicator(Configuration.builder().indicatorName("test").contract(c).numOfUnits(5).build());

	@Test
	void test() {
		tester.update(Num.of(0, 1, true));
		assertThat(tester.ringBuf.size()).isEqualTo(1);
		assertThat(tester.get(0).timestamp()).isEqualTo(1);
		tester.update(Num.of(0, 2, true));	// 由于时间不同，这次值会覆盖上一个值
		assertThat(tester.ringBuf.size()).isEqualTo(1);
		assertThat(tester.get(0).timestamp()).isEqualTo(2);
		tester.update(Num.of(0, 1, false));
		assertThat(tester.ringBuf.size()).isEqualTo(1);
		assertThat(tester.get(0).timestamp()).isEqualTo(1);
		
		tester.update(Num.of(0, 1, false));	// 与上一个值相同状态，这次更新会被忽略
		assertThat(tester.ringBuf.size()).isEqualTo(1);
	}

	class TestIndicator extends AbstractIndicator{

		protected TestIndicator(Configuration cfg) {
			super(cfg);
		}

		@Override
		protected Num evaluate(Num num) {
			return num;
		}
		
	}
}
