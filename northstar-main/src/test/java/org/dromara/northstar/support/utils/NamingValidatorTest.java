package org.dromara.northstar.support.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NamingValidatorTest {

	@Test
	void test() {
		assertThat(NamingValidator.isValid("666")).isTrue();
		assertThat(NamingValidator.isValid("sss")).isTrue();
		assertThat(NamingValidator.isValid("好好好")).isTrue();
		assertThat(NamingValidator.isValid("策略_a1")).isTrue();
		assertThat(NamingValidator.isValid("//\\\\")).isFalse();
	}

}
