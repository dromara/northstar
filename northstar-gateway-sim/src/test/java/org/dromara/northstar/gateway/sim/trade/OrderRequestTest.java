package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.function.Consumer;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;

class OrderRequestTest {

	OrderRequest orderRequest;
    SimGatewayAccount mockAccount = mock(SimGatewayAccount.class);
    @SuppressWarnings("unchecked")
	Consumer<Order> mockOnOrderCallback = mock(Consumer.class);
    @SuppressWarnings("unchecked")
	Consumer<Transaction> mockOnTradeCallback = mock(Consumer.class);
	LocalDate today = LocalDate.now();
	Contract c1 = Contract.builder().symbol("rb2205@SHFE").multiplier(10).longMarginRatio(0.08).shortMarginRatio(0.08).build();
	Tick tick1 = Tick.builder().tradingDay(today).contract(c1).lastPrice(5111).build();
	SubmitOrderReq mockSubmitOrderReq = SubmitOrderReq.builder()
			.originOrderId("testId")
			.contract(c1)
			.volume(10)
			.price(1000)
			.orderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)
			.build();

    @Test
    void testOriginOrderId() {
    	SubmitOrderReq req = mockSubmitOrderReq.toBuilder().direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
        String originOrderId = orderRequest.originOrderId();
        assertEquals(mockSubmitOrderReq.originOrderId(), originOrderId);
    }

    @Test
    void testOnTick() {
    	SubmitOrderReq req = mockSubmitOrderReq.toBuilder().direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
        orderRequest.onTick(tick1);

        verify(mockOnOrderCallback, times(1)).accept(any(Order.class));
        verify(mockOnTradeCallback, times(1)).accept(any(Transaction.class));
    }
    
    @Test 
    void testTotalVolume() {
    	SubmitOrderReq req = mockSubmitOrderReq.toBuilder().direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	assertThat(req.volume()).isEqualTo(10);
    }
    
    @Test 
    void testValidate() {
    	SubmitOrderReq req = mockSubmitOrderReq.toBuilder().direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Open).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	when(mockAccount.available()).thenReturn(1000000D);
    	assertThat(orderRequest.validate()).isTrue();
    }
    
    @Test 
    void testValidateFailOpen() {
    	SubmitOrderReq req = mockSubmitOrderReq.toBuilder().direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Open).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	assertThat(orderRequest.validate()).isFalse();
    }
    
    @Test 
    void testValidateFailClose() {
    	SubmitOrderReq req = mockSubmitOrderReq.toBuilder().direction(DirectionEnum.D_Buy).offsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	PositionManager posMgr = mock(PositionManager.class);
    	when(mockAccount.getPositionManager()).thenReturn(posMgr);
    	assertThat(orderRequest.validate()).isFalse();
    }
    
}
