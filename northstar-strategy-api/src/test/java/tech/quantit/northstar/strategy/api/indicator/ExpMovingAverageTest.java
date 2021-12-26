package tech.quantit.northstar.strategy.api.indicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import xyz.redtorch.pb.CoreField.BarField;

class ExpMovingAverageTest {
	
	double[] sample = new double[] {
		5562,5421,5465,5515,5422,5546,5304,4976,4900,4825,
		4917,4655,4712,4646,4509,4230,4385,4226,4247,4312
	};
	
	double[] results = new double[] {
		5497.34,5513.56,5443.71,5287.80,5158.54,
		5047.36,5003.90,4887.60,4829.07,4768.05,
		4681.70
	};

	@Test
	void test() {
		int size = results.length;
		Indicator ema5 = new ExpMovingAverage(5, ValueType.CLOSE);
		for(int i=0; i<size+4; i++) {
			ema5.onBar(BarField.newBuilder().setClosePrice(sample[i]).build());
		}
		for(int i=0; i<5; i++) {
			assertThat(ema5.value(i)).isCloseTo(results[results.length - 1 - i], offset(2D));
		}
	}

}
