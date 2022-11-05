package tech.quantit.northstar.gateway.playback.ticker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.gateway.api.domain.BarGenerator;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.api.domain.time.CnFtComTradeTime1;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelper;
import tech.quantit.northstar.gateway.api.domain.time.PeriodHelperFactory;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 验证不同精度所生成的TICK数据是否能还原为原来的BAR
 * @author KevinHuangwl
 *
 */

class TrigonometricTickSimulationTest {
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	ContractField contract = factory.makeContract("rb2210");
	
	IContractManager contractMgr = mock(IContractManager.class);
	
	BarGenerator barGen;
	
	@SuppressWarnings("unchecked")
	@BeforeEach
	void prepare() {
		PeriodHelperFactory phFactory = mock(PeriodHelperFactory.class);
		when(phFactory.newInstance(anyInt(), anyBoolean(), any(ContractField.class))).thenReturn(new PeriodHelper(60, new CnFtComTradeTime1()));
		barGen = new BarGenerator(new NormalContract(contract, System.currentTimeMillis()), (Consumer<BarField>) mock(Consumer.class), phFactory);
		when(contractMgr.getContract(anyString())).thenReturn(contract);
	}
	
	@Test
	void testPrecisionLow1() {
		// 阳线场景
		BarField bar = BarField.newBuilder()
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setOpenPrice(5000)
				.setClosePrice(5050)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.LOW, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(4);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5000);
		assertThat(ticks.get(1).getLastPrice()).isEqualTo(4950);
		assertThat(ticks.get(2).getLastPrice()).isEqualTo(5100);
		assertThat(ticks.get(3).getLastPrice()).isEqualTo(5050);
	}
	
	@Test
	void testPrecisionLow2() {
		// 阴线场景
		BarField bar = BarField.newBuilder()
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setOpenPrice(5050)
				.setClosePrice(5000)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.LOW, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(4);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5050);
		assertThat(ticks.get(1).getLastPrice()).isEqualTo(5100);
		assertThat(ticks.get(2).getLastPrice()).isEqualTo(4950);
		assertThat(ticks.get(3).getLastPrice()).isEqualTo(5000);
	}
	
	@Test
	void testPrecisionMedium1() {
		// 阳线场景
		BarField bar = BarField.newBuilder()
				.setGatewayId("testGateway")
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.of(21, 15).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 15)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.setOpenPrice(5000)
				.setClosePrice(5050)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.MEDIUM, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(30);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5000);
		assertThat(ticks.get(29).getLastPrice()).isEqualTo(5050);
		ticks.forEach(tick -> {
			barGen.update(mapToNow(tick));
		});
		assertThat(barGen.finishOfBar()).isEqualTo(bar);
	}
	
	@Test
	void testPrecisionMedium2() {
		// 阴线场景
		BarField bar = BarField.newBuilder()
				.setGatewayId("testGateway")
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.of(21, 15).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 15)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.setOpenPrice(5050)
				.setClosePrice(5000)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.MEDIUM, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(30);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5050);
		assertThat(ticks.get(29).getLastPrice()).isEqualTo(5000);
		ticks.forEach(tick -> {
			barGen.update(mapToNow(tick));
		});
		assertThat(barGen.finishOfBar()).isEqualTo(bar);
	}
	
	@Test
	void testPrecisionHigh1() {
		// 阳线场景
		BarField bar = BarField.newBuilder()
				.setGatewayId("testGateway")
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.of(21, 15).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 15)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.setOpenPrice(5000)
				.setClosePrice(5050)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.HIGH, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(120);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5000);
		assertThat(ticks.get(119).getLastPrice()).isEqualTo(5050);
		ticks.forEach(tick -> {
			barGen.update(mapToNow(tick));
		});
		assertThat(barGen.finishOfBar()).isEqualTo(bar);
	}
	
	@Test
	void testPrecisionHigh2() {
		// 阴线场景
		BarField bar = BarField.newBuilder()
				.setGatewayId("testGateway")
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.of(21, 15).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 15)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.setOpenPrice(5050)
				.setClosePrice(5000)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.HIGH, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(120);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5050);
		assertThat(ticks.get(119).getLastPrice()).isEqualTo(5000);
		ticks.forEach(tick -> {
			barGen.update(mapToNow(tick));
		});
		assertThat(barGen.finishOfBar()).isEqualTo(bar);
	}
	
	@Test
	void testPrecisionHigh3() {
		// 更多数据
		BarField bar = BarField.newBuilder()
				.setGatewayId("testGateway")
				.setUnifiedSymbol(contract.getUnifiedSymbol())
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.of(21, 15).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setActionTimestamp(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 15)).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.setOpenPrice(5050)
				.setClosePrice(5000)
				.setHighPrice(5100)
				.setLowPrice(4950)
				.setPreClosePrice(5000)
				.setPreOpenInterest(230000)
				.setPreSettlePrice(5001)
				.setVolume(1800000)
				.setVolumeDelta(0)
				.setOpenInterest(1800000)
				.setOpenInterestDelta(7200)
				.setNumTrades(2400000)
				.setNumTradesDelta(0)
				.setTurnover(360000000)
				.setTurnoverDelta(0)
				.build();
		TrigonometricTickSimulation ticker = new TrigonometricTickSimulation(contract.getGatewayId(), PlaybackPrecision.HIGH, contractMgr);
		List<TickField> ticks = ticker.generateFrom(bar);
		assertThat(ticks).hasSize(120);
		assertThat(ticks.get(0).getLastPrice()).isEqualTo(5050);
		assertThat(ticks.get(119).getLastPrice()).isEqualTo(5000);
		ticks.forEach(tick -> {
			barGen.update(mapToNow(tick));
		});
		assertThat(barGen.finishOfBar()).isEqualTo(bar);
	}
	
	private TickField mapToNow(TickField t) {
		return t.toBuilder()
				.setActionTimestamp(System.currentTimeMillis())
				.build();
	}
}
