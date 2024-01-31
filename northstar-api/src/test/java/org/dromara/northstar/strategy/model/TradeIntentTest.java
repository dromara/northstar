package org.dromara.northstar.strategy.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

class TradeIntentTest {

	private IModuleContext context;
    private Logger logger;
    private Contract contract;
    private Predicate<Double> priceDiffConditionToAbort;
    private TradeIntent tradeIntent;
    private Tick tick;
    private Order order;
    private Trade trade;

    @BeforeEach
    void setUp() {
        context = mock(IModuleContext.class);
        logger = mock(Logger.class);
        contract = mock(Contract.class);
        priceDiffConditionToAbort = mock(Predicate.class);
        tick = mock(Tick.class);
        order = mock(Order.class);
        trade = mock(Trade.class);

        when(context.getLogger(any())).thenReturn(logger);
        when(tick.contract()).thenReturn(contract);
        when(tick.lastPrice()).thenReturn(100.0);
        when(tick.actionTimestamp()).thenReturn(100000L);
        when(order.originOrderId()).thenReturn("order1");
        when(trade.originOrderId()).thenReturn("order1");
        when(trade.volume()).thenReturn(10);

        tradeIntent = new TradeIntent(contract, SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 100.0, 10, 10000, priceDiffConditionToAbort);
        tradeIntent.setContext(context);
    }

    @Test
    void testConstructor() {
        assertNotNull(tradeIntent);
    }

    @Test
    void testOnTickNotMatchingContract() {
        when(tick.contract()).thenReturn(mock(Contract.class)); // A different contract
        tradeIntent.onTick(tick);
        verify(context, never()).submitOrderReq(any(), any(), any(), anyInt(), anyDouble());
    }

    @Test
    void testOnTickInitialPriceSet() {
    	when(context.getState()).thenReturn(ModuleState.PLACING_ORDER);
    	assertDoesNotThrow(() -> {
    		tradeIntent.onTick(tick);
    	});
    }

    @Test
    void testOnTickPriceDiffAbort() {
        when(priceDiffConditionToAbort.test(anyDouble())).thenReturn(true);
        tradeIntent.onTick(tick);
        assertTrue(tradeIntent.hasTerminated());
        verify(context, never()).submitOrderReq(any(), any(), any(), anyInt(), anyDouble());
    }

    @Test
    void testOnTickSubmitOrder() {
        when(context.getState()).thenReturn(ModuleState.EMPTY);
        tradeIntent.onTick(tick);
        verify(context).submitOrderReq(eq(contract), eq(SignalOperation.BUY_OPEN), eq(PriceType.ANY_PRICE), eq(10), eq(100.0));
    }

    @Test
    void testOnTickCancelOrder() {
    	when(context.submitOrderReq(eq(contract), eq(SignalOperation.BUY_OPEN), eq(PriceType.ANY_PRICE), eq(10), eq(100.0))).thenReturn(Optional.of("order1"));
    	when(context.getState()).thenReturn(ModuleState.EMPTY);
        when(context.isOrderWaitTimeout(anyString(), anyLong())).thenReturn(true);
        tradeIntent.onTick(tick); // Should set the orderIdRef
        tradeIntent.onTick(tick); // Should trigger cancelOrder

        verify(context).cancelOrder("order1");
    }

    @Test
    void testOnTrade() {
    	when(context.submitOrderReq(eq(contract), eq(SignalOperation.BUY_OPEN), eq(PriceType.ANY_PRICE), eq(10), eq(100.0))).thenReturn(Optional.of("order1"));
    	when(context.getState()).thenReturn(ModuleState.EMPTY, ModuleState.PENDING_ORDER);
        tradeIntent.onTick(tick); // To set orderIdRef
        tradeIntent.onTrade(trade);
        assertTrue(tradeIntent.hasTerminated());
    }

    @Test
    void testHasTerminated() {
        when(priceDiffConditionToAbort.test(anyDouble())).thenReturn(true);
        tradeIntent.onTick(tick);
        assertTrue(tradeIntent.hasTerminated());
    }

    @Test
    void testToString() {
        String expected = String.format("TradeIntent [contract=%s, operation=%s, priceType=%s, price=%s, volume=%s, timeout=%s]",
                contract.contractId(), SignalOperation.BUY_OPEN, PriceType.ANY_PRICE, 100.0, 10, 10000);
        assertEquals(expected, tradeIntent.toString());
    }
}
