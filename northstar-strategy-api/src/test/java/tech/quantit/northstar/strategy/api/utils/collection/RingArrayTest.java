package tech.quantit.northstar.strategy.api.utils.collection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RingArrayTest {

	@Test
	void test() {
		int[] sample = new int[] {10, 11, 12, 13, 14, 15};
		RingArray<Object> ring = new RingArray<>(4);
		for(int i=0; i<sample.length - 1; i++) {
			ring.update(sample[i], false);
		}
		ring.update(sample[sample.length - 1], true);
		
		assertThat(ring.get()).isEqualTo(15);
		assertThat(ring.get(0)).isEqualTo(15);
		assertThat(ring.get(1)).isEqualTo(12);
		assertThat(ring.get(2)).isEqualTo(13);
		assertThat(ring.get(-2)).isEqualTo(13);
		assertThat(ring.get(-1)).isEqualTo(14);
	}

	
	@Test
	void test1() {
		int[] sample = new int[] {10, 11, 12, 13, 14, 15};
		RingArray<Object> ring = new RingArray<>(4);
		for(int i=0; i<sample.length; i++) {
			ring.update(sample[i], false);
		}
		
		assertThat(ring.get()).isEqualTo(15);
		assertThat(ring.get(0)).isEqualTo(15);
		assertThat(ring.get(1)).isEqualTo(12);
		assertThat(ring.get(2)).isEqualTo(13);
		assertThat(ring.get(-2)).isEqualTo(13);
		assertThat(ring.get(-1)).isEqualTo(14);
	}
}
