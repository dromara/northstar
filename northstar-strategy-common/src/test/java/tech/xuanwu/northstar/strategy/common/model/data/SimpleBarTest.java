package tech.xuanwu.northstar.strategy.common.model.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SimpleBarTest {
	
	SimpleBar bar;
	
	@Before
	public void setUp() {
		bar = new SimpleBar(1000);
	}

	@Test
	public void testBarRange() {
		bar.update(1500);
		bar.update(1200);
		assertThat(bar.barRange()).isEqualTo(500);
	}

	@Test
	public void testUpperShadow() {
		bar.update(1500);
		bar.update(1200);
		assertThat(bar.upperShadow()).isEqualTo(300);
		
		bar.update(1600);
		assertThat(bar.upperShadow()).isZero();
		
		bar.update(900);
		assertThat(bar.upperShadow()).isEqualTo(600);
	}

	@Test
	public void testLowerShadow() {
		bar.update(1500);
		bar.update(1200);
		assertThat(bar.lowerShadow()).isZero();
		
		bar.update(990);
		assertThat(bar.lowerShadow()).isZero();
		
		bar.update(1100);
		assertThat(bar.lowerShadow()).isEqualTo(10);
	}

	@Test
	public void testIsPositive() {
		bar.update(1500);
		bar.update(1200);
		assertThat(bar.isPositive()).isTrue();
	}

	@Test
	public void testActualDiff() {
		bar.update(1500);
		bar.update(1200);
		assertThat(bar.actualDiff()).isEqualTo(200);
	}
}
