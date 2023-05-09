package org.dromara.northstar.indicator.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.junit.jupiter.api.Test;

class SimpleValueIndicatorTest {
	
	SimpleValueIndicator in = new SimpleValueIndicator(Configuration.builder().build());

	@Test
	void testNaN() {
		in.update(Num.NaN());
		assertThat(in.getData()).isEmpty();
	}
	
	@Test
	void test() {
		in.update(Num.of(0, 0));
		assertThat(in.getData()).hasSize(1);
	}
	
	@Test
	void testUnstable() {
		in.update(Num.of(0, 0, true));
		assertThat(in.getData()).hasSize(1);
		
		in.update(Num.of(0, 0, false));
		assertThat(in.getData()).hasSize(1);
	}
	
	@Test
	void testTimestamp() {
		in.update(Num.of(0, 0));
		assertThat(in.getData()).hasSize(1);
		
		in.update(Num.of(0, 1));
		assertThat(in.getData()).hasSize(2);
		
		in.update(Num.of(0, 0));
		assertThat(in.getData()).hasSize(2);
		
		in.update(Num.of(0, 1));
		assertThat(in.getData()).hasSize(2);
	}

	@Test
	void testComplex() {
		in.update(Num.of(0, 0));
		assertThat(in.getData()).hasSize(1);
		
		in.update(Num.of(0, 1, true));
		assertThat(in.getData()).hasSize(2);
		
		in.update(Num.of(0, 1));
		assertThat(in.getData()).hasSize(2);
		
		in.update(Num.of(0, 1));				// 无效更新
		assertThat(in.getData()).hasSize(2);
		
		in.update(Num.of(0, 1, true));			// 无效更新
		assertThat(in.getData()).hasSize(2);
		
		in.update(Num.of(0, 2, true));			// 有效更新，增加一位
		assertThat(in.getData()).hasSize(3);
		
		in.update(Num.of(0, 3, true));			// 有效更新，更新最新值
		assertThat(in.getData()).hasSize(3);
		
		in.update(Num.of(0, 3));
		assertThat(in.getData()).hasSize(3);
		
		in.update(Num.of(0, 10));
		assertThat(in.getData()).hasSize(4);
		
		in.update(Num.of(0, 5));
		assertThat(in.getData()).hasSize(4);
		
		in.update(Num.of(0, 6));
		assertThat(in.getData()).hasSize(4);
	}
}
