package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.contract.IndexContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

class IndexTickerTest {
	
	IndexTicker ticker;
	
	Consumer<Tick> consumer;
	
	AtomicReference<Tick> ref = new AtomicReference<Tick>();
	
	IContract c0 = mock(IContract.class);
	IContract c1 = mock(IContract.class);
	IContract c2 = mock(IContract.class);
	IContract c3 = mock(IContract.class);
	IContract c4 = mock(IContract.class);
	IContract c5 = mock(IContract.class);
	IContract c6 = mock(IContract.class);
	IContract c7 = mock(IContract.class);
	IContract c8 = mock(IContract.class);
	IContract c9 = mock(IContract.class);
	
	Contract cc0 = Contract.builder().unifiedSymbol("test0").build();
	Contract cc1 = Contract.builder().unifiedSymbol("test1").build();
	Contract cc2 = Contract.builder().unifiedSymbol("test2").build();
	Contract cc3 = Contract.builder().unifiedSymbol("test3").build();
	Contract cc4 = Contract.builder().unifiedSymbol("test4").build();
	Contract cc5 = Contract.builder().unifiedSymbol("test5").build();
	Contract cc6 = Contract.builder().unifiedSymbol("test6").build();
	Contract cc7 = Contract.builder().unifiedSymbol("test7").build();
	Contract cc8 = Contract.builder().unifiedSymbol("test8").build();
	Contract cc9 = Contract.builder().unifiedSymbol("test9").build();
	
	List<Contract> contracts = List.of(cc0, cc1, cc2, cc3, cc4, cc5, cc6, cc7, cc8, cc9);
	
	@BeforeEach
	void prepare() {
		IndexContract c = mock(IndexContract.class);
		when(c.contract()).thenReturn(Contract.builder().unifiedSymbol("test0000").name("测试指数").priceTick(0.01).build());
		when(c.memberContracts()).thenReturn(List.of(c0, c1, c2, c3, c4, c5, c6, c7, c8, c9));
		when(c0.contract()).thenReturn(cc0);
		when(c1.contract()).thenReturn(cc1);
		when(c2.contract()).thenReturn(cc2);
		when(c3.contract()).thenReturn(cc3);
		when(c4.contract()).thenReturn(cc4);
		when(c5.contract()).thenReturn(cc5);
		when(c6.contract()).thenReturn(cc6);
		when(c7.contract()).thenReturn(cc7);
		when(c8.contract()).thenReturn(cc8);
		when(c9.contract()).thenReturn(cc9);
		
		ticker = new IndexTicker(c, t -> {
			ref.set(t);
		});
	}

	@Test
	void test() {
		List<Tick> ticks = new ArrayList<>();
		
		for(int i=0; i<10; i++) {
			Tick t = createTick(contracts.get(i), 1);
			ticks.add(t);
			ticker.update(t);
		}
		ticker.update(createTick(contracts.get(0), 2));
		
		assertThat(ref.get().volume()).isEqualTo(ticks.stream().mapToLong(Tick::volume).sum());
		assertThat(ref.get().volumeDelta()).isEqualTo(ticks.stream().mapToLong(Tick::volumeDelta).sum());
		assertThat(ref.get().turnover()).isCloseTo(ticks.stream().mapToDouble(Tick::turnover).sum(), offset(1D));
		assertThat(ref.get().turnoverDelta()).isCloseTo(ticks.stream().mapToDouble(Tick::turnoverDelta).sum(), offset(1D));
		assertThat(ref.get().openInterest()).isCloseTo(ticks.stream().mapToDouble(Tick::openInterest).sum(), offset(1e-2));
		assertThat(ref.get().openInterestDelta()).isCloseTo(ticks.stream().mapToDouble(Tick::openInterestDelta).sum(), offset(1e-2));
		assertThat(ref.get().preOpenInterest()).isCloseTo(ticks.stream().mapToDouble(Tick::preOpenInterest).sum(), offset(1e-2));
		
		INDArray tickMatrix = matrix(ticks);
		INDArray weight = oiWeighted(ticks);
		INDArray weightedTickMatrix = tickMatrix.mul(weight);
		assertThat(ref.get().openPrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(1).toDoubleVector()).sum(), offset(1e-2));
		assertThat(ref.get().highPrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(2).toDoubleVector()).sum(), offset(1e-2));
		assertThat(ref.get().lowPrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(3).toDoubleVector()).sum(), offset(1e-2));
		assertThat(ref.get().lastPrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(4).toDoubleVector()).sum(), offset(1e-2));
		assertThat(ref.get().settlePrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(5).toDoubleVector()).sum(), offset(1e-2));
		assertThat(ref.get().preClosePrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(6).toDoubleVector()).sum(), offset(1e-2));
		assertThat(ref.get().preSettlePrice()).isCloseTo(DoubleStream.of(weightedTickMatrix.getColumn(7).toDoubleVector()).sum(), offset(1e-2));
	}
	
	@Test
	void testMatrix() {
		Tick t0 = createTick(cc0, 0);
		Tick t1 = createTick(cc1, 0);
		INDArray nd = matrix(List.of(t0, t1));
		assertThat(nd.getRow(0).toDoubleVector()[0]).isCloseTo(t0.openInterest(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[1]).isCloseTo(t0.openPrice(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[2]).isCloseTo(t0.highPrice(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[3]).isCloseTo(t0.lowPrice(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[4]).isCloseTo(t0.lastPrice(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[5]).isCloseTo(t0.settlePrice(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[6]).isCloseTo(t0.preClosePrice(), offset(1e-6));
		assertThat(nd.getRow(0).toDoubleVector()[7]).isCloseTo(t0.preSettlePrice(), offset(1e-6));
		assertThat(nd.getRow(1).toDoubleVector()[2]).isCloseTo(t1.highPrice(), offset(1e-6));
		System.out.println(nd);
		System.out.println(nd.getColumn(0));
		System.out.println(nd.getColumn(2));
	}
	
	@Test
	void testOIWeighted() {
		Tick t0 = Tick.builder().openInterest(6).build();
		Tick t1 = Tick.builder().openInterest(4).build();
		INDArray nd = oiWeighted(List.of(t0, t1));
		assertThat(nd.toDoubleVector()[0]).isEqualTo(0.6);
		assertThat(nd.toDoubleVector()[1]).isEqualTo(0.4);
		System.out.println(nd);
	}
	
	@Test
	void testWeighted() {
		Tick t0 = Tick.builder().lastPrice(5).openInterest(6).build();
		Tick t1 = Tick.builder().lastPrice(10).openInterest(4).build();
		INDArray matrix = matrix(List.of(t0, t1));
		INDArray weight = oiWeighted(List.of(t0, t1));
		System.out.println(matrix);
		System.out.println(weight);
		INDArray result = matrix.mul(weight);
		assertThat(result.getColumn(4).toDoubleVector()[0]).isEqualTo(3);
		assertThat(result.getColumn(4).toDoubleVector()[1]).isEqualTo(4);
	}
	
	@Test
	void testMul() {
		INDArray nd = Nd4j.create(new float[]{1,2,3,4},new int[]{2,2});
		INDArray weight = Nd4j.create(new float[]{4,2},new int[]{2,1});
		System.out.println(nd);
		System.out.println(nd.mul(weight));
	}
	
	private INDArray oiWeighted(List<Tick> ticks) {
		INDArray nd = matrix(ticks);
		INDArray oi = nd.getColumn(0);
		double sumOI = DoubleStream.of(oi.toDoubleVector()).sum();
		return oi.div(sumOI).reshape(-1, 1);
	}
	
	private INDArray matrix(List<Tick> ticks) {
		if (ticks == null || ticks.isEmpty()) {
	        throw new IllegalArgumentException("The list of ticks must not be null or empty");
	    }

	    int numFeatures = 8; // Open, High, Low, Last, Settle, Pre-Close, Pre-Settle prices
	    int numTicks = ticks.size();

	    // 使用流来创建一个连续的double数组
	    double[] data = ticks.stream()
	                         .flatMapToDouble(t -> DoubleStream.of(vectorize(t)))
	                         .toArray();

	    // 创建一个基于连续double数组的INDArray
	    INDArray tickMatrix = Nd4j.create(data, new int[]{numTicks, numFeatures});

	    return tickMatrix;
	}
	
	private double[] vectorize(Tick t) {
		return new double[] {
			t.openInterest(),
			t.openPrice(),
			t.highPrice(),
			t.lowPrice(),
			t.lastPrice(),
			t.settlePrice(), 
			t.preClosePrice(),
			t.preSettlePrice()
		};
	}
	
	private Tick createTick(Contract c, long time) {
		Random r = new Random(System.currentTimeMillis());
		double lastPrice = r.nextDouble(5000);
		return Tick.builder()
				.actionDay(LocalDate.now())
				.actionTime(LocalTime.now())
				.actionTimestamp(time)
				.contract(c)
				.lastPrice(lastPrice)
				.settlePrice(r.nextDouble(5000))
				.preClosePrice(r.nextDouble(5000))
				.preSettlePrice(r.nextDouble(5000))
				.highPrice(Math.max(r.nextDouble(6000), lastPrice))
				.lowPrice(Math.min(r.nextDouble(4000), lastPrice))
				.openPrice(r.nextDouble(5000))
				.volume(r.nextLong(10000))
				.volumeDelta(r.nextLong(1000))
				.turnover(r.nextDouble(10000))
				.turnoverDelta(r.nextDouble(1000))
				.openInterestDelta(r.nextDouble(1000))
				.openInterest(r.nextDouble(10000))
				.preOpenInterest(r.nextDouble(10000))
				.type(TickType.MARKET_TICK)
				.build();
	}
	
}
