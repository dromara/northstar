package tech.quantit.northstar.strategy.api.indicator;

import org.junit.jupiter.api.Test;
import xyz.redtorch.pb.CoreField.BarField;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class BollTest {
	String symbol = "rb2205";
	
	// 数据来源于RB2205在22年1月19日（包含18日夜盘）的一分钟数据
	double[] sample = new double[] {
			4632,4633,4638,4636,4632,4640,4637,4643,4649,4655,4660,4664,4672,4676,4676,4672,4669,
			4674,4668,4669,4671,4675,4679,4678,4679,4686,4683,4683,4680,4678,4684,4684,4684,4686,
			4687,4692,4693,4696,4702,4702,4697,4699,4697,4697,4699,4694,4698,4697,4695,4697,4694,
			4694,4692,4691,4694,4697,4698,4699,4702,4704,4696,4701,4704,4702,4704,4702,4704,4705,
			4701,4703,4706,4706,4706,4709,4712,4714,4717,4715,4718,4722,4722,4720,4720,4723,4719,
			4720,4721,4721,4721,4718,4723,4727,4731,4728,4726,4725,4723,4723,4723,4730,4731,4731,
			4730,4734,4733,4732,4734,4735,4735,4729,4733,4730,4725,4728,4720,4719,4716,4712,4711,
			4716,4713,4715,4714,4709,4705,4714,4720,4716,4717,4715,4716,4712,4709,4708,4712,4714,
	};

	@Test
	void test() {
		MultiValueIndicator boll = new Boll(symbol);
		for (int i1 = 0; i1 < sample.length; i1++) {
			boll.onBar(BarField.newBuilder().setUnifiedSymbol(symbol).setClosePrice(sample[i1]).build());
			int index = i1 % 20;
			// 布林周期前20条为0, 其后叠加求平均
			String format = String.format("验证下标：%s, Upper: %s, Lower: %s, Mid: %s", index,
					boll.value(Boll.UPPER, 0), boll.value(Boll.LOWER, 0), boll.value(Boll.MID, 0));
			System.out.println(format);
		}
	}

}
