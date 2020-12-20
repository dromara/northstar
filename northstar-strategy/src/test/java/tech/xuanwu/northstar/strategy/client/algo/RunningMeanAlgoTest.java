package tech.xuanwu.northstar.strategy.client.algo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.Random;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.math.Stats;

import tech.xuanwu.northstar.strategy.trade.algo.RunningMeanAlgo;

public class RunningMeanAlgoTest {
	
	static RunningMeanAlgo algo = new RunningMeanAlgo(10);
	
	static Random r = new Random();
	
	static double[] sampleData = new double[] {10.5,9,8,8,9.5,8.5,9,10,11,12};
	
	@BeforeClass
	public void init() {
		algo.init(sampleData);
		
	}

	@Test(dataProvider = "dp")
	public void correctionTest(Double n, Double res) {
		algo.update(n);
		assertThat(algo.getResult()).isCloseTo(res, offset(0.00001));
	}
	
	@Test
	public void performanceTest() {
		for(int i=0; i<10000000; i++) {
			algo.update(r.nextInt(20));
			algo.getResult();
		}
	}
	
	@Test
	public void performanceBenchMark() {
		for(int i=0; i<10000000; i++) {
			sampleData[i%10] = r.nextInt(20);
			Stats.meanOf(sampleData);
		}
	}

	@DataProvider
	public Object[][] dp() {
		return new Object[][] { 
			new Object[] { 12.0, 9.7 }, 
			new Object[] { 11.0, 9.9 },
			new Object[] { 10.0, 10.1},
			new Object[] { 15.0, 10.8 },
			new Object[] { 13.0, 11.15 },
			new Object[] { 11.0, 11.4 },
			new Object[] { 12.0, 11.7 },
			new Object[] { 9.0, 11.6 },
			new Object[] { 7.0, 11.2 },
			new Object[] { 6.0, 10.6 },
			new Object[] { 9.0, 10.3 },
			new Object[] { 8.0, 10.0 }
			};
	}
}
