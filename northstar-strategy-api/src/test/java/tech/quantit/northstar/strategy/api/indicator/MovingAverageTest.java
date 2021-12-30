package tech.quantit.northstar.strategy.api.indicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import xyz.redtorch.pb.CoreField.BarField;

class MovingAverageTest {
	String symbol = "rb2210";
	double[] sample = new double[20];
	
	@BeforeEach
	void prepare() {
		for(int i=0; i<20; i++) {
			sample[i] = ThreadLocalRandom.current().nextDouble(10000); 
		}
	}

	@Test
	void test() {
		Indicator ma10 = new MovingAverage(symbol, 10, ValueType.HIGH);
		for(double val : sample) {
			ma10.onBar(BarField.newBuilder().setHighPrice(val).build());
		}
		for(int i=0; i<10; i++) {
			double[] values = new double[10];
			System.arraycopy(sample, 10 - i, values, 0, 10);
			assertThat(ma10.value(i)).isCloseTo(StatUtils.mean(values), offset(1e-6));
		}
	}

}
