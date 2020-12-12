package tech.xuanwu.northstar.trader.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalTime;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import tech.xuanwu.northstar.constant.CommonConstant;
import tech.xuanwu.northstar.trader.domain.contract.IndexContract.TickEventHandler;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public class IndexContractTest {
	
	TickEventHandler handler = mock(TickEventHandler.class);
	
	IndexContract idxContract;
	
	static final String symbol1 = "rb2101";
	static final String symbol2 = "rb2105";
	static final String symbol3 = "rb2109";
	static final String symbol4 = "rb2112";
	static final ExchangeEnum exchange = ExchangeEnum.SHFE;
	static final String gatewayId = "gateway";
	
	static final Offset precise = Offset.offset(0.5);

	@Before
	public void beforeClass() {
		
		ContractField c1 = ContractField.newBuilder()
				.setGatewayId(gatewayId)
				.setSymbol(symbol1)
				.setExchange(exchange)
				.setPriceTick(1)
				.build();
		ContractField c2 = c1.toBuilder()
				.setSymbol(symbol2)
				.build();
		ContractField c3 = c1.toBuilder()
				.setSymbol(symbol3)
				.build();
		ContractField c4 = c1.toBuilder()
				.setSymbol(symbol4)
				.build();
		
		idxContract = new IndexContract(Lists.newArrayList(c1,c2,c3,c4), handler);
	}
	
	@Test
	public void updateByTickTest() throws InterruptedException {
		String actionDay = "20201212";
		double[] lastPrices = new double[] {2826,2536,2468,2333};
		double[] highPrices = new double[] {2955,2733,2658,2456};
		double[] lowPrices = new double[] {2701,2522,2323,2111};
		double[] openPrices = new double[] {2841,2522,2500,2333};
		long[] volumes = new long[] {2135446,4565645,4445687,5668997};
		double[] openInterests = new double[] {1234567,21345687,2121544,12564879};
		TickField t0 = TickField.newBuilder()
				.setUnifiedSymbol(symbol1)
				.setActionDay(actionDay)
				.setActionTime(LocalTime.now().format(CommonConstant.T_FORMAT_INT_FORMATTER))
				.setLastPrice(lastPrices[0])
				.setHighPrice(highPrices[0])
				.setLowPrice(lowPrices[0])
				.setOpenPrice(openPrices[0])
				.setVolume(volumes[0])
				.setOpenInterest(openInterests[0])
				.build();
		TickField t1 = TickField.newBuilder()
				.setUnifiedSymbol(symbol2)
				.setActionDay(actionDay)
				.setActionTime(LocalTime.now().format(CommonConstant.T_FORMAT_INT_FORMATTER))
				.setLastPrice(lastPrices[1])
				.setHighPrice(highPrices[1])
				.setLowPrice(lowPrices[1])
				.setOpenPrice(openPrices[1])
				.setVolume(volumes[1])
				.setOpenInterest(openInterests[1])
				.build();
		TickField t2 = TickField.newBuilder()
				.setUnifiedSymbol(symbol3)
				.setActionDay(actionDay)
				.setActionTime(LocalTime.now().format(CommonConstant.T_FORMAT_INT_FORMATTER))
				.setLastPrice(lastPrices[2])
				.setHighPrice(highPrices[2])
				.setLowPrice(lowPrices[2])
				.setOpenPrice(openPrices[2])
				.setVolume(volumes[2])
				.setOpenInterest(openInterests[2])
				.build();
		TickField t3 = TickField.newBuilder()
				.setUnifiedSymbol(symbol4)
				.setActionDay(actionDay)
				.setActionTime(LocalTime.now().format(CommonConstant.T_FORMAT_INT_FORMATTER))
				.setLastPrice(lastPrices[3])
				.setHighPrice(highPrices[3])
				.setLowPrice(lowPrices[3])
				.setOpenPrice(openPrices[3])
				.setVolume(volumes[3])
				.setOpenInterest(openInterests[3])
				.build();
		
		idxContract.updateByTick(t0);
		idxContract.updateByTick(t1);
		idxContract.updateByTick(t2);
		Thread.sleep(300);
		idxContract.updateByTick(t3);
		
		assertThat(idxContract.tickBuilder.getActionDay()).isEqualTo(actionDay);
		
		long totalVol = LongStream.of(volumes).sum();
		assertThat(idxContract.tickBuilder.getVolume()).isEqualTo(totalVol);
		
		double totalOpenInterest = DoubleStream.of(openInterests).sum();
		assertThat(idxContract.tickBuilder.getOpenInterest()).isCloseTo(totalOpenInterest, precise);
		
		double[] weights = DoubleStream.of(openInterests).map(v -> v / totalOpenInterest).toArray();
		double[] weightLastPrice = new double[weights.length];
		double[] weightHighPrice = new double[weights.length];
		double[] weightLowPrice = new double[weights.length];
		double[] weightOpenPrice = new double[weights.length];
		for(int i=0; i<weights.length; i++) {
			weightLastPrice[i] = weights[i] * lastPrices[i];
			weightOpenPrice[i] = weights[i] * openPrices[i];
			weightHighPrice[i] = weights[i] * highPrices[i];
			weightLowPrice[i] = weights[i] * lowPrices[i];
		}
		assertThat(idxContract.tickBuilder.getLastPrice()).isCloseTo(DoubleStream.of(weightLastPrice).sum(), precise);
		assertThat(idxContract.tickBuilder.getOpenPrice()).isCloseTo(DoubleStream.of(weightOpenPrice).sum(), precise);
		assertThat(idxContract.tickBuilder.getHighPrice()).isCloseTo(DoubleStream.of(weightHighPrice).sum(), precise);
		assertThat(idxContract.tickBuilder.getLowPrice()).isCloseTo(DoubleStream.of(weightLowPrice).sum(), precise);
	}
	
}
