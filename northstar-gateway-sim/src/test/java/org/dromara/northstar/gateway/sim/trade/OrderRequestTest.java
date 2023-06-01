package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

class OrderRequestTest {

	OrderRequest orderRequest;
    SimGatewayAccount mockAccount = mock(SimGatewayAccount.class);
    @SuppressWarnings("unchecked")
	Consumer<OrderField> mockOnOrderCallback = mock(Consumer.class);
    @SuppressWarnings("unchecked")
	Consumer<Transaction> mockOnTradeCallback = mock(Consumer.class);
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	TickField mockTick = factory.makeTickField("rb2205", 5111);
    ContractField mockContract = factory.makeContract("rb2205");
    
    SubmitOrderReqField.Builder mockSubmitOrderReq = SubmitOrderReqField.newBuilder()
			.setOriginOrderId("testId")
			.setContract(mockContract)
			.setVolume(10)
			.setPrice(1000)
			.setOrderPriceType(OrderPriceTypeEnum.OPT_AnyPrice);

    @Test
    void testOriginOrderId() {
    	SubmitOrderReqField req = mockSubmitOrderReq.setDirection(DirectionEnum.D_Buy).setOffsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
        String originOrderId = orderRequest.originOrderId();
        assertEquals(mockSubmitOrderReq.getOriginOrderId(), originOrderId);
    }

    @Test
    void testOnTick() {
    	SubmitOrderReqField req = mockSubmitOrderReq.setDirection(DirectionEnum.D_Buy).setOffsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
        orderRequest.onTick(mockTick);

        verify(mockOnOrderCallback, times(1)).accept(any(OrderField.class));
        verify(mockOnTradeCallback, times(1)).accept(any(Transaction.class));
    }
    
    @Test 
    void testTotalVolume() {
    	SubmitOrderReqField req = mockSubmitOrderReq.setDirection(DirectionEnum.D_Buy).setOffsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	assertThat(req.getVolume()).isEqualTo(10);
    }
    
    @Test 
    void testValidate() {
    	SubmitOrderReqField req = mockSubmitOrderReq.setDirection(DirectionEnum.D_Buy).setOffsetFlag(OffsetFlagEnum.OF_Open).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	when(mockAccount.available()).thenReturn(1000000D);
    	assertThat(orderRequest.validate()).isTrue();
    }
    
    @Test 
    void testValidateFailOpen() {
    	SubmitOrderReqField req = mockSubmitOrderReq.setDirection(DirectionEnum.D_Buy).setOffsetFlag(OffsetFlagEnum.OF_Open).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	assertThat(orderRequest.validate()).isFalse();
    }
    
    @Test 
    void testValidateFailClose() {
    	SubmitOrderReqField req = mockSubmitOrderReq.setDirection(DirectionEnum.D_Buy).setOffsetFlag(OffsetFlagEnum.OF_Close).build();
    	orderRequest = new OrderRequest(mockAccount, req, mockOnOrderCallback, mockOnTradeCallback);
    	PositionManager posMgr = mock(PositionManager.class);
    	when(mockAccount.getPositionManager()).thenReturn(posMgr);
    	assertThat(orderRequest.validate()).isFalse();
    }
    
}
