package org.dromara.northstar.indicator.volume;

import static org.junit.jupiter.api.Assertions.*;

import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreField.ContractField;

class VWAPIndicatorTest {
	
	ContractField contract = ContractField.newBuilder().build();

	@Test
	void test() {
		double[] prices = { 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0 };
		int[] volumes = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		Indicator vwap = new VWAPIndicator(Configuration.builder().contract(contract).build(), 10);
		for(int i=0; i<prices.length; i++) {
			vwap.dependencies().get(0).update(Num.of(prices[i], i));
			vwap.dependencies().get(1).update(Num.of(volumes[i], i));
			vwap.update(Num.of(prices[i], i));
		}
		
		double actual = vwap.value(0);

		assertEquals(70, actual, 1e-9); // tolerance of 0.0001
	}

}
