package tech.quantit.northstar.strategy.api.utils.collection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RingArrayTest {

	@Test
	void test() {
		Object[] sample = new Object[] {new Object(), new Object(), new Object(), new Object(), new Object(), new Object()};
		RingArray<Object> ring = new RingArray<>(4);
		for(Object obj : sample) {
			ring.update(obj);
		}
		
		assertThat(ring.get()).isEqualTo(sample[5]);
		assertThat(ring.get(1)).isEqualTo(sample[2]);
		assertThat(ring.get(2)).isEqualTo(sample[3]);
		assertThat(ring.get(-2)).isEqualTo(sample[3]);
		assertThat(ring.get(-1)).isEqualTo(sample[4]);
		assertThat(ring.update(sample[0])).contains(sample[2]);
	}

}
