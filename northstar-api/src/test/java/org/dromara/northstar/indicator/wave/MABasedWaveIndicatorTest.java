package org.dromara.northstar.indicator.wave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.wave.MABasedWaveIndicator.EndpointType;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

class MABasedWaveIndicatorTest {
	Contract contract = Contract.builder().build();
	// 单元测试中的简化配置，不能在实际场景使用
	Configuration cfg = Configuration.builder().contract(contract).cacheLength(3).build();
	SimpleValueIndicator maLine = new SimpleValueIndicator(cfg);
	MABasedWaveIndicator indicator = new MABasedWaveIndicator(cfg, maLine, 2, EndpointType.HIGH_LOW);
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void test() {
		String srcData = "[{\"open\":3739,\"low\":3735,\"high\":3746,\"close\":3737,\"volume\":35662,\"openInterestDelta\":0,\"openInterest\":1996209,\"timestamp\":1682520300000,\"MA10_15m\":3733.5},{\"open\":3737,\"low\":3735,\"high\":3748,\"close\":3745,\"volume\":40419,\"openInterestDelta\":0,\"openInterest\":1991873,\"timestamp\":1682521200000,\"MA10_15m\":3736.5},{\"open\":3738,\"low\":3733,\"high\":3746,\"close\":3745,\"volume\":69107,\"openInterestDelta\":0,\"openInterest\":1996081,\"timestamp\":1682558100000,\"MA10_15m\":3739.1},{\"open\":3745,\"low\":3733,\"high\":3746,\"close\":3736,\"volume\":38481,\"openInterestDelta\":0,\"openInterest\":1998628,\"timestamp\":1682559000000,\"MA10_15m\":3740.8},{\"open\":3735,\"low\":3722,\"high\":3737,\"close\":3728,\"volume\":63575,\"openInterestDelta\":0,\"openInterest\":1996359,\"timestamp\":1682559900000,\"MA10_15m\":3739},{\"open\":3727,\"low\":3719,\"high\":3728,\"close\":3721,\"volume\":50318,\"openInterestDelta\":0,\"openInterest\":1995385,\"timestamp\":1682560800000,\"MA10_15m\":3736.3},{\"open\":3720,\"low\":3712,\"high\":3725,\"close\":3720,\"volume\":68209,\"openInterestDelta\":0,\"openInterest\":2001619,\"timestamp\":1682561700000,\"MA10_15m\":3734.4},{\"open\":3721,\"low\":3708,\"high\":3723,\"close\":3713,\"volume\":52419,\"openInterestDelta\":0,\"openInterest\":2005542,\"timestamp\":1682563500000,\"MA10_15m\":3731.8},{\"open\":3713,\"low\":3704,\"high\":3717,\"close\":3713,\"volume\":53799,\"openInterestDelta\":0,\"openInterest\":2014215,\"timestamp\":1682564400000,\"MA10_15m\":3729.6},{\"open\":3712,\"low\":3704,\"high\":3717,\"close\":3716,\"volume\":42954,\"openInterestDelta\":0,\"openInterest\":2012238,\"timestamp\":1682565300000,\"MA10_15m\":3727.4},{\"open\":3715,\"low\":3714,\"high\":3734,\"close\":3727,\"volume\":103711,\"openInterestDelta\":0,\"openInterest\":2006177,\"timestamp\":1682566200000,\"MA10_15m\":3726.4},{\"open\":3735,\"low\":3718,\"high\":3741,\"close\":3727,\"volume\":119518,\"openInterestDelta\":0,\"openInterest\":2004104,\"timestamp\":1682574300000,\"MA10_15m\":3724.6},{\"open\":3729,\"low\":3728,\"high\":3744,\"close\":3737,\"volume\":96706,\"openInterestDelta\":0,\"openInterest\":2013631,\"timestamp\":1682575200000,\"MA10_15m\":3723.8},{\"open\":3737,\"low\":3713,\"high\":3737,\"close\":3717,\"volume\":100653,\"openInterestDelta\":0,\"openInterest\":2009433,\"timestamp\":1682576100000,\"MA10_15m\":3721.9},{\"open\":3717,\"low\":3707,\"high\":3720,\"close\":3709,\"volume\":78035,\"openInterestDelta\":0,\"openInterest\":2019686,\"timestamp\":1682577000000,\"MA10_15m\":3720},{\"open\":3708,\"low\":3705,\"high\":3716,\"close\":3708,\"volume\":58141,\"openInterestDelta\":0,\"openInterest\":2019783,\"timestamp\":1682577900000,\"MA10_15m\":3718.7},{\"open\":3708,\"low\":3690,\"high\":3709,\"close\":3691,\"volume\":139266,\"openInterestDelta\":0,\"openInterest\":2003400,\"timestamp\":1682578800000,\"MA10_15m\":3715.8},{\"open\":3691,\"low\":3681,\"high\":3695,\"close\":3688,\"volume\":147130,\"openInterestDelta\":0,\"openInterest\":1997531,\"timestamp\":1682601300000,\"MA10_15m\":3713.3},{\"open\":3688,\"low\":3678,\"high\":3693,\"close\":3680,\"volume\":66346,\"openInterestDelta\":0,\"openInterest\":1997562,\"timestamp\":1682602200000,\"MA10_15m\":3710},{\"open\":3679,\"low\":3665,\"high\":3681,\"close\":3669,\"volume\":98508,\"openInterestDelta\":0,\"openInterest\":2002081,\"timestamp\":1682603100000,\"MA10_15m\":3705.3},{\"open\":3669,\"low\":3667,\"high\":3682,\"close\":3680,\"volume\":61671,\"openInterestDelta\":0,\"openInterest\":2002493,\"timestamp\":1682604000000,\"MA10_15m\":3700.6},{\"open\":3679,\"low\":3678,\"high\":3694,\"close\":3692,\"volume\":71754,\"openInterestDelta\":0,\"openInterest\":1997291,\"timestamp\":1682604900000,\"MA10_15m\":3697.1},{\"open\":3693,\"low\":3683,\"high\":3701,\"close\":3685,\"volume\":67491,\"openInterestDelta\":0,\"openInterest\":1994135,\"timestamp\":1682605800000,\"MA10_15m\":3691.9},{\"open\":3686,\"low\":3675,\"high\":3687,\"close\":3677,\"volume\":56816,\"openInterestDelta\":0,\"openInterest\":1984226,\"timestamp\":1682606700000,\"MA10_15m\":3687.9},{\"open\":3677,\"low\":3677,\"high\":3692,\"close\":3686,\"volume\":58823,\"openInterestDelta\":0,\"openInterest\":1976676,\"timestamp\":1682607600000,\"MA10_15m\":3685.6},{\"open\":3691,\"low\":3691,\"high\":3705,\"close\":3699,\"volume\":86462,\"openInterestDelta\":0,\"openInterest\":1981211,\"timestamp\":1682644500000,\"MA10_15m\":3684.7},{\"open\":3700,\"low\":3692,\"high\":3700,\"close\":3698,\"volume\":32209,\"openInterestDelta\":0,\"openInterest\":1982878,\"timestamp\":1682645400000,\"MA10_15m\":3685.4},{\"open\":3699,\"low\":3693,\"high\":3701,\"close\":3699,\"volume\":32143,\"openInterestDelta\":0,\"openInterest\":1983720,\"timestamp\":1682646300000,\"MA10_15m\":3686.5},{\"open\":3700,\"low\":3698,\"high\":3709,\"close\":3705,\"volume\":54126,\"openInterestDelta\":0,\"openInterest\":1982662,\"timestamp\":1682647200000,\"MA10_15m\":3689},{\"open\":3707,\"low\":3698,\"high\":3709,\"close\":3699,\"volume\":35274,\"openInterestDelta\":0,\"openInterest\":1981002,\"timestamp\":1682648100000,\"MA10_15m\":3692},{\"open\":3699,\"low\":3691,\"high\":3701,\"close\":3692,\"volume\":41600,\"openInterestDelta\":0,\"openInterest\":1979088,\"timestamp\":1682649900000,\"MA10_15m\":3693.2},{\"open\":3692,\"low\":3685,\"high\":3694,\"close\":3693,\"volume\":43111,\"openInterestDelta\":0,\"openInterest\":1977974,\"timestamp\":1682650800000,\"MA10_15m\":3693.3},{\"open\":3692,\"low\":3684,\"high\":3694,\"close\":3687,\"volume\":29125,\"openInterestDelta\":0,\"openInterest\":1978858,\"timestamp\":1682651700000,\"MA10_15m\":3693.5},{\"open\":3685,\"low\":3680,\"high\":3691,\"close\":3684,\"volume\":36866,\"openInterestDelta\":0,\"openInterest\":1976030,\"timestamp\":1682652600000,\"MA10_15m\":3694.2},{\"open\":3681,\"low\":3675,\"high\":3692,\"close\":3678,\"volume\":75410,\"openInterestDelta\":0,\"openInterest\":1976029,\"timestamp\":1682660700000,\"MA10_15m\":3693.4},{\"open\":3678,\"low\":3672,\"high\":3682,\"close\":3673,\"volume\":44756,\"openInterestDelta\":0,\"openInterest\":1975994,\"timestamp\":1682661600000,\"MA10_15m\":3690.8},{\"open\":3672,\"low\":3666,\"high\":3675,\"close\":3673,\"volume\":69009,\"openInterestDelta\":0,\"openInterest\":1983388,\"timestamp\":1682662500000,\"MA10_15m\":3688.3},{\"open\":3673,\"low\":3663,\"high\":3680,\"close\":3671,\"volume\":71508,\"openInterestDelta\":0,\"openInterest\":1982813,\"timestamp\":1682663400000,\"MA10_15m\":3685.5},{\"open\":3671,\"low\":3663,\"high\":3679,\"close\":3674,\"volume\":68910,\"openInterestDelta\":0,\"openInterest\":1977265,\"timestamp\":1682664300000,\"MA10_15m\":3682.4},{\"open\":3674,\"low\":3660,\"high\":3677,\"close\":3660,\"volume\":109143,\"openInterestDelta\":0,\"openInterest\":1955066,\"timestamp\":1682665200000,\"MA10_15m\":3678.5}]";
		JSONArray data = JSON.parseArray(srcData);
		Consumer[] asserts = new Consumer[] {
				checkpoint1, checkpoint2, checkpoint3, checkpoint4
		};
		
		for(int i=0; i<data.size(); i++) {
			JSONObject obj = data.getJSONObject(i);
			Num close = Num.of(obj.getDoubleValue("close"), obj.getLongValue("timestamp"));
			Num high = Num.of(obj.getDoubleValue("high"), obj.getLongValue("timestamp"));
			Num low = Num.of(obj.getDoubleValue("low"), obj.getLongValue("timestamp"));
			Num ma = Num.of(obj.getDoubleValue("MA10_15m"), obj.getLongValue("timestamp"));
			maLine.update(ma);
			for(Indicator inner : indicator.dependencies()) {
				switch(inner.getConfiguration().valueType()) {
				case CLOSE -> inner.update(close);
				case HIGH -> inner.update(high);
				case LOW -> inner.update(low);
				default -> throw new IllegalArgumentException("Unexpected value: " + inner.getConfiguration().valueType());
				}
			}
			indicator.update(close);
			
			assertThat(maLine.value(0)).isCloseTo(obj.getDoubleValue("MA10_15m"), offset(1e-4));
			for(Consumer<JSONObject> con : asserts) {
				con.accept(obj);
			}
		}
		
		assertThat(indicator.value(0)).isCloseTo(3709, offset(1e-4));
		assertThat(indicator.value(-1)).isCloseTo(3665, offset(1e-4));
		assertThat(indicator.value(-2)).isCloseTo(3744, offset(1e-4));
	}
	
	Consumer<JSONObject> checkpoint1 = (json) -> {
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2023, 4, 27), LocalTime.of(9, 45));
		long t = ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		if(json.getLong("timestamp") == t) {
			System.out.println("checkpoint1 checking");
			assertThat(indicator.value(0)).isCloseTo(3746, offset(1e-4));
			assertThat(indicator.get(0).timestamp()).isEqualTo(t);
		}
	};

	Consumer<JSONObject> checkpoint2 = (json) -> {
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2023, 4, 27), LocalTime.of(13, 45));
		long t = ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		if(json.getLong("timestamp") == t) {
			System.out.println("checkpoint2 checking");
			assertThat(indicator.value(0)).isCloseTo(3704, offset(1e-4));
			assertThat(indicator.get(0).timestamp()).isEqualTo(t);
		}
	};
	
	Consumer<JSONObject> checkpoint3 = (json) -> {
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2023, 4, 27), LocalTime.of(14, 30));
		long t = ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		if(json.getLong("timestamp") == t) {
			System.out.println("checkpoint3 checking");
			assertThat(indicator.value(0)).isCloseTo(3744, offset(1e-4));
			assertThat(indicator.get(0).timestamp()).isEqualTo(t);
		}
	};
	
	Consumer<JSONObject> checkpoint4 = (json) -> {
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2023, 4, 28), LocalTime.of(9, 15));
		long t = ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		if(json.getLong("timestamp") == t) {
			System.out.println("checkpoint4 checking");
			assertThat(indicator.value(0)).isCloseTo(3665, offset(1e-4));
			assertThat(indicator.get(0).timestamp()).isEqualTo(t);
		}
	};
}
