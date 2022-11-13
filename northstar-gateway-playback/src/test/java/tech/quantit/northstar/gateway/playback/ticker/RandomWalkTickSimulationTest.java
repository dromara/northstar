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
				.build();
		
		List<TickEntry> results = rws.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(30);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5005D, offset(1.0));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(1.0));
		assertThat(results.get(29).price()).isCloseTo(5000D, offset(0.5));
	}
	
	@Test
	void testLargeRange() {
		BarField bar = BarField.newBuilder()
				.setOpenPrice(5000)
				.setHighPrice(5055)
				.setLowPrice(4998)
				.setClosePrice(5000)
				.build();
		
		List<TickEntry> results = rws.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(30);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5055D, offset(1.0));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(1.0));
		assertThat(results.get(29).price()).isCloseTo(5000D, offset(0.5));
	}
	
	@Test
	void testSmallPriceTick() {
		BarField bar = BarField.newBuilder()
				.setOpenPrice(5000)
				.setHighPrice(5005)
				.setLowPrice(4998)
				.setClosePrice(5000)
				.build();
		
		List<TickEntry> results = rws2.generateFrom(bar);
		for(TickEntry e : results) {			
			System.out.println(e);
		}
		assertThat(results).hasSize(120);
		assertThat(results.stream().mapToDouble(TickEntry::price).max().getAsDouble()).isCloseTo(5005D, offset(0.2));
		assertThat(results.stream().mapToDouble(TickEntry::price).min().getAsDouble()).isCloseTo(4998D, offset(0.2));
		assertThat(results.get(119).price()).isCloseTo(5000D, offset(0.2));
	}
}
