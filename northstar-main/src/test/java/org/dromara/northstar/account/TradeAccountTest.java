package org.dromara.northstar.account;

import org.dromara.northstar.common.exception.IllegalOrderRequestException;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.TradeGateway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TradeAccountTest {

    private TradeGateway tradeGateway;
    private TradeAccount tradeAccount;
    private GatewayDescription gatewayDescription;

    private Contract contract;
    private ContractDefinition contractDefinition;

    @BeforeAll
    static void initMessageSource() {
        MessageSourceHolder.setMessageSource(mock(MessageSource.class));
    }

    @BeforeEach
    void setUp() {
        gatewayDescription = GatewayDescription.builder()
                .gatewayId("TEST_GATEWAY")
                .build();
        tradeGateway = mock(TradeGateway.class);
        when(tradeGateway.getDescription()).thenReturn(gatewayDescription);
        when(tradeGateway.cancelOrder(any())).thenReturn(true);

        tradeAccount = new TradeAccount(tradeGateway);

        contractDefinition = ContractDefinition.builder()
                .marginRatio(0.1)
                .build();
        contract = Contract.builder()
                .contractId("rb2505@SHFE@FUTURES")
                .unifiedSymbol("rb2505@SHFE@FUTURES")
                .symbol("rb2505")
                .multiplier(10)
                .contractDefinition(contractDefinition)
                .build();
    }

    @Test
    void shouldReturnGatewayId() {
        assertEquals("TEST_GATEWAY", tradeAccount.getId());
    }

    @Test
    void shouldReturnTradeGateway() {
        assertSame(tradeGateway, tradeAccount.getTradeGateway());
    }

    @Test
    void shouldReturnZeroBalanceWhenNoAccount() {
        assertEquals(0, tradeAccount.accountBalance());
        assertEquals(0, tradeAccount.availableBalance());
    }

    @Test
    void shouldUpdateAndReturnPosition() {
        Position position = Position.builder()
                .positionDirection(PositionDirectionEnum.PD_Long)
                .position(10)
                .frozen(2)
                .contract(contract)
                .build();

        tradeAccount.onPosition(position);

        assertTrue(tradeAccount.getPosition(PositionDirectionEnum.PD_Long, contract).isPresent());
        assertEquals(10, tradeAccount.getPosition(PositionDirectionEnum.PD_Long, contract).get().position());
        assertFalse(tradeAccount.getPosition(PositionDirectionEnum.PD_Short, contract).isPresent());
    }

    @Test
    void shouldSubmitOpenOrderWhenSufficientBalance() {
        Account account = Account.builder()
                .balance(100000)
                .available(100000)
                .build();
        tradeAccount.onAccount(account);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_001")
                .contract(contract)
                .volume(1)
                .price(5000)
                .direction(DirectionEnum.D_Buy)
                .offsetFlag(OffsetFlagEnum.OF_Open)
                .build();

        tradeAccount.submitOrder(orderReq);

        verify(tradeGateway, times(1)).submitOrder(orderReq);
    }

    @Test
    void shouldThrowExceptionWhenOpenOrderWithInsufficientBalance() {
        Account account = Account.builder()
                .balance(1000)
                .available(1000)
                .build();
        tradeAccount.onAccount(account);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_002")
                .contract(contract)
                .volume(10)
                .price(5000)
                .direction(DirectionEnum.D_Buy)
                .offsetFlag(OffsetFlagEnum.OF_Open)
                .build();

        assertThrows(IllegalOrderRequestException.class, () -> tradeAccount.submitOrder(orderReq));
        verify(tradeGateway, never()).submitOrder(any());
    }

    @Test
    void shouldDeductLocalFrozenAfterOpenOrderSubmitted() {
        Account account = Account.builder()
                .balance(100000)
                .available(100000)
                .build();
        tradeAccount.onAccount(account);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_003")
                .contract(contract)
                .volume(1)
                .price(5000)
                .direction(DirectionEnum.D_Buy)
                .offsetFlag(OffsetFlagEnum.OF_Open)
                .build();

        tradeAccount.submitOrder(orderReq);

        // 成本 = 5000 * 1 * 10 * 0.1 = 5000
        assertEquals(95000, tradeAccount.availableBalance());
    }

    @Test
    void shouldSubmitCloseOrderWhenSufficientPosition() {
        Position position = Position.builder()
                .positionDirection(PositionDirectionEnum.PD_Long)
                .position(5)
                .frozen(0)
                .contract(contract)
                .build();
        tradeAccount.onPosition(position);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_004")
                .contract(contract)
                .volume(3)
                .price(5100)
                .direction(DirectionEnum.D_Sell)
                .offsetFlag(OffsetFlagEnum.OF_Close)
                .build();

        tradeAccount.submitOrder(orderReq);

        verify(tradeGateway, times(1)).submitOrder(orderReq);
    }

    @Test
    void shouldThrowExceptionWhenCloseOrderWithInsufficientPosition() {
        Position position = Position.builder()
                .positionDirection(PositionDirectionEnum.PD_Long)
                .position(2)
                .frozen(0)
                .contract(contract)
                .build();
        tradeAccount.onPosition(position);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_005")
                .contract(contract)
                .volume(3)
                .price(5100)
                .direction(DirectionEnum.D_Sell)
                .offsetFlag(OffsetFlagEnum.OF_Close)
                .build();

        assertThrows(IllegalOrderRequestException.class, () -> tradeAccount.submitOrder(orderReq));
        verify(tradeGateway, never()).submitOrder(any());
    }

    @Test
    void shouldConsiderFrozenPositionWhenClosing() {
        Position position = Position.builder()
                .positionDirection(PositionDirectionEnum.PD_Long)
                .position(5)
                .frozen(3)
                .contract(contract)
                .build();
        tradeAccount.onPosition(position);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_006")
                .contract(contract)
                .volume(3)
                .price(5100)
                .direction(DirectionEnum.D_Sell)
                .offsetFlag(OffsetFlagEnum.OF_Close)
                .build();

        assertThrows(IllegalOrderRequestException.class, () -> tradeAccount.submitOrder(orderReq));
        verify(tradeGateway, never()).submitOrder(any());
    }

    @Test
    void shouldAddPendingOrderOnUnfinishedOrder() {
        Order order = Order.builder()
                .orderId("ORDER_001")
                .originOrderId("ORIGIN_007")
                .orderStatus(OrderStatusEnum.OS_NoTradeQueueing)
                .contract(contract)
                .build();

        tradeAccount.onOrder(order);

        assertEquals(1, tradeAccount.getPendingOrders().size());
        assertEquals("ORDER_001", tradeAccount.getPendingOrders().get(0).orderId());
    }

    @Test
    void shouldRemovePendingOrderOnDoneOrder() {
        Order pendingOrder = Order.builder()
                .orderId("ORDER_002")
                .originOrderId("ORIGIN_008")
                .orderStatus(OrderStatusEnum.OS_NoTradeQueueing)
                .contract(contract)
                .build();
        tradeAccount.onOrder(pendingOrder);
        assertEquals(1, tradeAccount.getPendingOrders().size());

        Order doneOrder = Order.builder()
                .orderId("ORDER_002")
                .originOrderId("ORIGIN_008")
                .orderStatus(OrderStatusEnum.OS_AllTraded)
                .contract(contract)
                .build();
        tradeAccount.onOrder(doneOrder);

        assertTrue(tradeAccount.getPendingOrders().isEmpty());
    }

    @Test
    void shouldReleaseLocalFrozenWhenOrderDone() {
        Account account = Account.builder()
                .balance(100000)
                .available(100000)
                .build();
        tradeAccount.onAccount(account);

        SubmitOrderReq orderReq = SubmitOrderReq.builder()
                .originOrderId("ORIGIN_009")
                .contract(contract)
                .volume(1)
                .price(5000)
                .direction(DirectionEnum.D_Buy)
                .offsetFlag(OffsetFlagEnum.OF_Open)
                .build();
        tradeAccount.submitOrder(orderReq);
        assertEquals(95000, tradeAccount.availableBalance());

        Order doneOrder = Order.builder()
                .orderId("ORDER_003")
                .originOrderId("ORIGIN_009")
                .orderStatus(OrderStatusEnum.OS_AllTraded)
                .contract(contract)
                .build();
        tradeAccount.onOrder(doneOrder);

        assertEquals(100000, tradeAccount.availableBalance());
    }

    @Test
    void shouldRecordTrade() {
        Trade trade = Trade.builder()
                .tradeId("TRADE_001")
                .orderId("ORDER_004")
                .volume(1)
                .price(5000)
                .contract(contract)
                .build();

        tradeAccount.onTrade(trade);
    }

    @Test
    void shouldReturnFalseWhenCancelNonExistingOrder() {
        CancelOrderReq cancelReq = CancelOrderReq.builder()
                .orderId("NON_EXIST_ORDER")
                .originOrderId("NON_EXIST_ORIGIN")
                .build();

        assertFalse(tradeAccount.cancelOrder(cancelReq));
        verify(tradeGateway, never()).cancelOrder(any());
    }

    @Test
    void shouldCancelExistingOrder() {
        Order pendingOrder = Order.builder()
                .orderId("ORDER_005")
                .originOrderId("ORIGIN_010")
                .orderStatus(OrderStatusEnum.OS_NoTradeQueueing)
                .contract(contract)
                .build();
        tradeAccount.onOrder(pendingOrder);

        CancelOrderReq cancelReq = CancelOrderReq.builder()
                .orderId("ORDER_005")
                .originOrderId("ORIGIN_010")
                .build();

        assertTrue(tradeAccount.cancelOrder(cancelReq));
        verify(tradeGateway, times(1)).cancelOrder(cancelReq);
    }

    @Test
    void shouldHandleCanceledOrderStatus() {
        Order pendingOrder = Order.builder()
                .orderId("ORDER_006")
                .originOrderId("ORIGIN_011")
                .orderStatus(OrderStatusEnum.OS_NoTradeQueueing)
                .contract(contract)
                .build();
        tradeAccount.onOrder(pendingOrder);
        assertEquals(1, tradeAccount.getPendingOrders().size());

        Order canceledOrder = Order.builder()
                .orderId("ORDER_006")
                .originOrderId("ORIGIN_011")
                .orderStatus(OrderStatusEnum.OS_Canceled)
                .contract(contract)
                .build();
        tradeAccount.onOrder(canceledOrder);

        assertTrue(tradeAccount.getPendingOrders().isEmpty());
    }

    @Test
    void shouldUpdateExistingPendingOrder() {
        Order order = Order.builder()
                .orderId("ORDER_007")
                .originOrderId("ORIGIN_012")
                .orderStatus(OrderStatusEnum.OS_NoTradeQueueing)
                .totalVolume(10)
                .tradedVolume(0)
                .contract(contract)
                .build();
        tradeAccount.onOrder(order);

        Order partialOrder = Order.builder()
                .orderId("ORDER_007")
                .originOrderId("ORIGIN_012")
                .orderStatus(OrderStatusEnum.OS_PartTradedQueueing)
                .totalVolume(10)
                .tradedVolume(5)
                .contract(contract)
                .build();
        tradeAccount.onOrder(partialOrder);

        assertEquals(1, tradeAccount.getPendingOrders().size());
        assertEquals(5, tradeAccount.getPendingOrders().get(0).tradedVolume());
    }

    @Test
    void shouldHandleNullAccountState() {
        // 未设置账户状态时，余额应为0
        assertEquals(0, tradeAccount.accountBalance());
        assertEquals(0, tradeAccount.availableBalance());
    }

    @Test
    void shouldHandleMultiplePositions() {
        Contract contract2 = Contract.builder()
                .contractId("m2505@DCE@FUTURES")
                .unifiedSymbol("m2505@DCE@FUTURES")
                .symbol("m2505")
                .multiplier(10)
                .build();

        Position longPos = Position.builder()
                .positionDirection(PositionDirectionEnum.PD_Long)
                .position(10)
                .frozen(0)
                .contract(contract)
                .build();
        Position shortPos = Position.builder()
                .positionDirection(PositionDirectionEnum.PD_Short)
                .position(5)
                .frozen(0)
                .contract(contract2)
                .build();

        tradeAccount.onPosition(longPos);
        tradeAccount.onPosition(shortPos);

        assertEquals(10, tradeAccount.getPosition(PositionDirectionEnum.PD_Long, contract).get().position());
        assertEquals(5, tradeAccount.getPosition(PositionDirectionEnum.PD_Short, contract2).get().position());
    }
}
