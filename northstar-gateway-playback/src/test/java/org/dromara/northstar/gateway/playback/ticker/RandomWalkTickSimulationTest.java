package org.dromara.northstar.gateway.playback.ticker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.junit.jupiter.api.Test;

/**
 * 由于生成算法的随机性，本单元测试不排除出现失败的可能
 * @author KevinHuangwl
 *
 */
class RandomWalkTickSimulationTest {

	RandomWalkTickSimulation rws = new RandomWalkTickSimulation(30);
	
	RandomWalkTickSimulation rws2 = new RandomWalkTickSimulation(120);
	
	@Test
	void testSmallRange() {
		Bar bar = Bar.builder()
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(31)
				.volumeDelta(2000)
				.build();
		
		List<TickEntry> results = rws.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(30);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5005D, offset(1.2));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(1.2));
		assertThat(results.get(29).price()).isCloseTo(5000D, offset(0.6));
		assertThat(results.stream().mapToDouble(TickEntry::openInterestDelta).sum()).isCloseTo(31, offset(0.1));
		assertThat(results.stream().mapToLong(TickEntry::volume).sum()).isEqualTo(2000);
		assertThat(results.get(29).volume()).isPositive();
	}
	
	@Test
	void testLargeRange() {
		Bar bar = Bar.builder()
				.openPrice(5000)
				.highPrice(5055)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(500)
				.volumeDelta(50000)
				.build();
		
		List<TickEntry> results = rws.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(30);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5055D, offset(0.6));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(0.6));
		assertThat(results.get(29).price()).isCloseTo(5000D, offset(0.6));
		assertThat(results.stream().mapToDouble(TickEntry::openInterestDelta).sum()).isCloseTo(500, offset(0.6));
		assertThat(results.stream().mapToLong(TickEntry::volume).sum()).isEqualTo(50000);
		assertThat(results.get(29).volume()).isPositive();
	}
	
	@Test
	void testSmallPriceTick() {
		Bar bar = Bar.builder()
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(-50)
				.volumeDelta(10000)
				.build();
		
		List<TickEntry> results = rws2.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(120);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5005D, offset(0.25));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(0.25));
		assertThat(results.get(119).price()).isCloseTo(5000D, offset(0.25));
		assertThat(results.stream().mapToDouble(TickEntry::openInterestDelta).sum()).isCloseTo(-50, offset(0.6));
		assertThat(results.stream().mapToLong(TickEntry::volume).sum()).isEqualTo(10000);
		assertThat(results.get(119).volume()).isPositive();
	}
}
