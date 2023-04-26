package org.dromara.northstar.indicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RingArrayTest {

	private RingArray<Integer> ringArray;

    @BeforeEach
    void setUp() {
        ringArray = new RingArray<>(5);
    }

    @Test
    void testGet() {
        assertNull(ringArray.get());
        ringArray.update(1, false);
        assertEquals(1, ringArray.get().intValue());
        ringArray.update(2, true);
        assertEquals(2, ringArray.get(0).intValue());
        assertEquals(1, ringArray.get(-1).intValue());
    }

    @Test
    void testUpdate() {
        assertFalse(ringArray.update(1, false).isPresent());
        Optional<Integer> oldVal = ringArray.update(2, true);
        assertFalse(oldVal.isPresent());
        oldVal = ringArray.update(3, true);
        assertTrue(oldVal.isPresent());
        assertEquals(2, oldVal.get().intValue());
    }

    @Test
    void testToArray() {
    	RingArray<Num> ringArray = new RingArray<>(5);
    	assertThat(ringArray.toArray()).hasSize(5);
    	assertThat(ringArray.size()).isZero();
        ringArray.update(Num.of(1), false);
        ringArray.update(Num.of(2), false);
        ringArray.update(Num.of(3), false);
        Num[] expected = new Num[] {Num.of(3), Num.of(2), Num.of(1), null, null};
        assertArrayEquals(expected, ringArray.toArray());
    }

    @Test
    void testSize() {
        assertEquals(0, ringArray.size());
        ringArray.update(1, false);
        assertEquals(1, ringArray.size());
        ringArray.update(2, true);
        assertEquals(2, ringArray.size());
    }
    
	@Test
	void test() {
		int[] sample = new int[] {10, 11, 12, 13, 14, 15};
		RingArray<Object> ring = new RingArray<>(4);
		for(int i=0; i<sample.length; i++) {
			for(int j=0; j<3; j++) {
				ring.update(sample[i] + (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextInt(3), true);
			}
			ring.update(sample[i], false);
		}
		
		assertThat(ring.get()).isEqualTo(15);
		assertThat(ring.get(0)).isEqualTo(15);
		assertThat(ring.get(1)).isEqualTo(12);
		assertThat(ring.get(2)).isEqualTo(13);
		assertThat(ring.get(-2)).isEqualTo(13);
		assertThat(ring.get(-1)).isEqualTo(14);
		
		assertThat(ring.update(16, true)).hasValue(12);
		assertThat(ring.update(16, false)).hasValue(16);
		assertThat(ring.update(17, false)).hasValue(13);
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
