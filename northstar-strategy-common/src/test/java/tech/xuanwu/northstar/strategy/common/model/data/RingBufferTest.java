package tech.xuanwu.northstar.strategy.common.model.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class RingBufferTest {

	@Test
	public void testRingBufferInt() {
		RingBuffer<Double> prices = new RingBuffer<>(3);
		assertThat(prices.buf).hasSize(3);
	}

	@Test
	public void testRingBufferListOfT() {
		List<Double> list = List.of(3D, 3D, 3D);
		RingBuffer<Double> prices = new RingBuffer<>(list);
		assertThat(prices.buf).hasSize(3);
	}

	@Test
	public void testOffer() {
		List<Double> list = List.of(3D, 3D, 3D);
		RingBuffer<Double> prices = new RingBuffer<>(list);
		prices.offer(100D);
		prices.offer(100D);
		prices.offer(100D);
		prices.offer(100D);
		assertThat(prices.cursor).isEqualTo(1);
	}

	@Test
	public void testToList() {
		List<Double> list = List.of(3D, 3D, 3D);
		RingBuffer<Double> prices = new RingBuffer<>(list);
		assertThat(prices.toList()).hasOnlyElementsOfType(Double.class);
	}

	@Test
	public void testToArray() {
		List<Double> list = List.of(3D, 3D, 3D);
		RingBuffer<Double> prices = new RingBuffer<>(list);
		assertThat(prices.toArray(new Double[3])).hasOnlyElementsOfType(Double.class);
	}

}
