package org.dromara.northstar.ai.sampling;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.util.Random;

import org.dromara.northstar.ai.SampleData;
import org.junit.jupiter.api.Test;

class SampleDataWriterTest {

	@Test
	void test() {
		assertDoesNotThrow(() -> {
			SampleDataWriter sdw = new SampleDataWriter(new File("data/test2.csv"));
			Random r = new Random();
			for(int i=0; i<10; i++) {
				sdw.append(SampleData.builder()
						.actionDate("20231230")
						.actionTime("00:00:02")
						.states(new float[] {r.nextFloat(), r.nextFloat(), r.nextFloat()})
						.marketPrice(r.nextDouble())
						.build());
			}
		});
	}

}
