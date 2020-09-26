package tech.xuanwu.northstar.strategy.client.indicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import tech.xuanwu.northstar.strategy.trade.DataRef;
import tech.xuanwu.northstar.strategy.trade.indicator.MovingAverageIndicator;
import xyz.redtorch.pb.CoreField.BarField;

public class MovingAverageIndicatorTest {
	
	DataRef<BarField> dataRef;
	
	double[] prices = new double[] {3356.2,3358,3358.6,3355,3326};
	double[] mkPrices = new double[] {3355,3369,3354,3333,3312,3345,3346,3347,3348,3354,3343,3336,3395,3375,3314,3377,3366,3345,3354,3364,3365};
	double[] avgPrices = new double[] {3350.52,3352.72,3351.8,3347.4,3344.6,3342.6,3338,3336.6,3339.6,3348,3347.6,3345.6,3355.2,3360.6,3352.6,3359.4,3365.4,3355.4,3351.2,3361.2,3358.8};
	
	@BeforeClass
	public void prepare() {
		dataRef = mock(DataRef.class);
		
		List<BarField> barData = new ArrayList<>();
		for(double v : prices) {
			barData.add(BarField.newBuilder().setClosePrice(v).build());
		}
		when(dataRef.getDataRef()).thenReturn(barData);
	}
	
	
	@Test
	public void test() {
		MovingAverageIndicator ma5 = new MovingAverageIndicator(dataRef, DataRef.PriceType.CLOSE, 5, 6);
		ma5.init();
		for(int i=0; i<mkPrices.length; i++) {
			BarField.Builder bb = BarField.newBuilder();
			bb.setClosePrice(mkPrices[i]);
			ma5.update(bb.build());
			assertThat(ma5.getValue()).isCloseTo(avgPrices[i], offset(0.00001));
		}
		
		assertThat(ma5.getMaxRef()).isEqualTo(6);
		for(int i=0; i<7; i++) {
			assertThat(ma5.getValue(i)).isCloseTo(avgPrices[avgPrices.length-1-i], offset(0.00001));			
		}
		
	}
}
