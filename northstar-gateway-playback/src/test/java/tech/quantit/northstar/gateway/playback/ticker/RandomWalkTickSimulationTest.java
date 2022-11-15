package tech.quantit.northstar.gateway.playback.ticker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 由于生成算法的随机性，本单元测试不排除出现失败的可能
 * @author KevinHuangwl
 *
 */
class RandomWalkTickSimulationTest {

	RandomWalkTickSimulation rws = new RandomWalkTickSimulation(30, 1);
	
	RandomWalkTickSimulation rws2 = new RandomWalkTickSimulation(120, 0.2);
	
	@Test
	void testSmallRange() {
		BarField bar = BarField.newBuilder()
				.setOpenPrice(5000)
				.setHighPrice(5005)
				.setLowPrice(4998)
				.setClosePrice(5000)
				.setOpenInterestDelta(31)
				.setVolume(2000)
				.build();
		
		List<TickEntry> results = rws.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(30);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5005D, offset(0.6));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(0.6));
		assertThat(results.get(29).price()).isCloseTo(5000D, offset(0.6));
		assertThat(results.stream().mapToDouble(TickEntry::openInterestDelta).sum()).isCloseTo(31, offset(0.1));
		assertThat(results.stream().mapToLong(TickEntry::volume).sum()).isEqualTo(2000);
		assertThat(results.get(29).volume()).isPositive();
	}
	
	@Test
	void testLargeRange() {
		BarField bar = BarField.newBuilder()
				.setOpenPrice(5000)
				.setHighPrice(5055)
				.setLowPrice(4998)
				.setClosePrice(5000)
				.setOpenInterestDelta(500)
				.setVolume(50000)
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
		BarField bar = BarField.newBuilder()
				.setOpenPrice(5000)
				.setHighPrice(5005)
				.setLowPrice(4998)
				.setClosePrice(5000)
				.setOpenInterestDelta(-50)
				.setVolume(10000)
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
