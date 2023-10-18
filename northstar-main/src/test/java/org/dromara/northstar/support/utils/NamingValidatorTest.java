package org.dromara.northstar.support.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NamingValidatorTest {

	@Test
	void test() {
		NamingValidator validator = new NamingValidator();
		
		assertThat(validator.isValid("666")).isTrue();
		assertThat(validator.isValid("sss")).isTrue();
		assertThat(validator.isValid("好好好")).isTrue();
		assertThat(validator.isValid("策略_a1")).isTrue();
		assertThat(validator.isValid("//\\\\")).isFalse();
	}

}
