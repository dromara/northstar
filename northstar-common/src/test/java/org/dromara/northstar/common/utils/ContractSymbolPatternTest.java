package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class ContractSymbolPatternTest {

	@Test
	void testOPTION() {
		Pattern ptn = Pattern.compile("[A-z]+[0-9]{3,4}[^@].+");
		assertThat(ptn.matcher("m2207-C-2600@CTP").matches()).isTrue();
	}
}
