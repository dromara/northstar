package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModulePositionTest {
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	String NAME = "MODULE";
	
	String SYMBOL = "rb2210";
	TradeField buyTrade = factory.makeTradeField(SYMBOL, 2000, 10, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField buyTrade2 = factory.makeTradeField(SYMBOL, 1800, 8, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField buyTrade3 = factory.makeTradeField("rb2110", 1800, 8, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField sellTrade = factory.makeTradeField(SYMBOL, 2000, 10, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField sellTrade2 = factory.makeTradeField(SYMBOL, 2000, 8, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
	TickField tick = factory.makeTickField(SYMBOL, 2100);
	TickField tick2 = factory.makeTickField("rb2110", 2100);
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldUpdateProfit() {
		ModulePosition p1 = new ModulePosition(NAME, buyTrade, 0, mock(Consumer.class));
		p1.onTick(tick);
		assertThat(p1.getProfit()).isEqualTo(10000);
		
		ModulePosition p2 = new ModulePosition(NAME, sellTrade, 0, mock(Consumer.class));
		p2.onTick(tick);
		assertThat(p2.getProfit()).isEqualTo(-10000);
	}
	
	@Test
	public void shouldNotUpdateProfit() {
		ModulePosition p1 = new ModulePosition(NAME, buyTrade, 0, mock(Consumer.class));
		p1.onTick(tick2);
		assertThat(p1.getProfit()).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldTriggerStopLoss() {
		Consumer<ModuleDealRecord> callback = mock(Consumer.class);
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2100, callback);
		p1.meb = mock(ModuleEventBus.class);
		p1.onTick(tick);
		verify(p1.meb).post(any(ModuleEvent.class));
		verify(callback, times(0)).accept(any(ModuleDealRecord.class));
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2200, mock(Consumer.class));
		p1.meb = mock(ModuleEventBus.class);
		p1.onTick(tick);
		verify(p1.meb, times(0)).post(any(ModuleEvent.class));
	}
	
	@Test
	public void testOpenPrice() {
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2100, mock(Consumer.class));
		assertThat(p1.openPrice()).isEqualTo(2000);
	}
	
	@Test
	public void testSetEventBus() {
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2100, mock(Consumer.class));
		ModuleEventBus meb = mock(ModuleEventBus.class);
		p1.setEventBus(meb);
		assertThat(p1.meb).isEqualTo(meb);
	}
	
	@Test
	public void shouldSkipTheEvent() {
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2100, mock(Consumer.class));
		ModuleEventBus meb = mock(ModuleEventBus.class);
		p1.setEventBus(meb);
		
		p1.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, SubmitOrderReqField.newBuilder()
				.setContract(sellTrade.getContract())
				.setDirection(DirectionEnum.D_Sell)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.build()));
		verify(meb, times(0)).post(any());
		
		p1.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, SubmitOrderReqField.newBuilder()
				.setContract(sellTrade.getContract())
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Open)
				.build()));
		verify(meb, times(0)).post(any());
	}
	
	@Test
	public void shouldEmitOrderPassed() {
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2100, mock(Consumer.class));
		p1.lastTick = TickField.newBuilder().setTradingDay(sellTrade.getTradingDay()).build();
		ModuleEventBus meb = mock(ModuleEventBus.class);
		p1.setEventBus(meb);
		
		p1.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, SubmitOrderReqField.newBuilder()
				.setContract(sellTrade.getContract())
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_CloseYesterday)
				.build()));
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {
			@Override
			public boolean matches(ModuleEvent<?> argument) {
				return argument.getEventType() == ModuleEventType.ORDER_REQ_ACCEPTED;
			}
		}));
		
		p1.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, SubmitOrderReqField.newBuilder()
				.setContract(sellTrade.getContract())
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_CloseToday)
				.build()));
		verify(meb, times(2)).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {
			@Override
			public boolean matches(ModuleEvent<?> argument) {
				return argument.getEventType() == ModuleEventType.ORDER_REQ_ACCEPTED;
			}
		}));
		
		p1.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, SubmitOrderReqField.newBuilder()
				.setContract(sellTrade.getContract())
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_Close)
				.build()));
		verify(meb, times(3)).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {
			@Override
			public boolean matches(ModuleEvent<?> argument) {
				return argument.getEventType() == ModuleEventType.ORDER_REQ_ACCEPTED;
			}
		}));
	}
	
	@Test
	public void shouldEmitOrderRetained() {
		ModulePosition p1 = new ModulePosition(NAME, sellTrade, 2100, mock(Consumer.class));
		p1.lastTick = TickField.newBuilder().setTradingDay(sellTrade.getTradingDay()).build();
		ModuleEventBus meb = mock(ModuleEventBus.class);
		p1.setEventBus(meb);
		
		p1.onEvent(new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, SubmitOrderReqField.newBuilder()
				.setContract(sellTrade.getContract())
				.setDirection(DirectionEnum.D_Buy)
				.setOffsetFlag(OffsetFlagEnum.OF_CloseYesterday)
				.setVolume(sellTrade.getVolume() + 1)
				.build()));
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {
			@Override
			public boolean matches(ModuleEvent<?> argument) {
				return argument.getEventType() == ModuleEventType.ORDER_REQ_RETAINED;
			}
		}));
	}
	
	@Test
	public void testConvertAndConstruction() {
		ModulePosition mp1 = new ModulePosition(NAME, sellTrade, 2100, mock(Consumer.class));
		ModulePosition mp2 = new ModulePosition(mp1.convertTo(), sellTrade.getContract(), mock(Consumer.class));
		assertThat(mp1.getDirection()).isEqualTo(mp2.getDirection());
		assertThat(mp1.getVolume()).isEqualTo(mp2.getVolume());
		assertThat(mp1.contract()).isEqualTo(mp2.contract());
		assertThat(mp1.openPrice()).isCloseTo(mp2.openPrice(), offset(1e-6));
	}
	
	@Test
	public void shouldAddPosition() {
		ModulePosition mp1 = new ModulePosition(NAME, buyTrade, 0, mock(Consumer.class));
		ModulePosition mp2 = new ModulePosition(NAME, buyTrade2, 0, mock(Consumer.class));
		ModulePosition mp = mp1.merge(mp2);
		assertThat(mp.getDirection()).isEqualTo(PositionDirectionEnum.PD_Long);
		assertThat(mp.getVolume()).isEqualTo(18);
		assertThat(mp.contract()).isEqualTo(mp2.contract());
		assertThat(mp.openPrice()).isCloseTo(1911.111111, offset(1e-6));
	}
	
	@Test
	public void shouldReducePosition() {
		ModulePosition mp1 = new ModulePosition(NAME, buyTrade, 0, mock(Consumer.class));
		ModulePosition mp2 = new ModulePosition(NAME, sellTrade2, 0, mock(Consumer.class));
		ModulePosition mp = mp1.merge(mp2);
		assertThat(mp.getDirection()).isEqualTo(PositionDirectionEnum.PD_Long);
		assertThat(mp.getVolume()).isEqualTo(2);
		assertThat(mp.contract()).isEqualTo(mp2.contract());
		assertThat(mp.openPrice()).isCloseTo(2000, offset(1e-6));
	}
	
	@Test
	public void shouldThrowIfNotMatchSymbol() {
		ModulePosition mp1 = new ModulePosition(NAME, buyTrade, 0, mock(Consumer.class));
		ModulePosition mp2 = new ModulePosition(NAME, buyTrade3, 0, mock(Consumer.class));
		assertThrows(IllegalStateException.class, ()->{			
			mp1.merge(mp2);
		});
	}
	
	@Test
	public void shouldThrowIfNotMatchDirection() {
		ModulePosition mp1 = new ModulePosition(NAME, buyTrade, 0, mock(Consumer.class));
		ModulePosition mp2 = new ModulePosition(NAME, sellTrade, 0, mock(Consumer.class));
		assertThrows(IllegalStateException.class, ()->{			
			mp1.merge(mp2);
		});
	}
}
