package org.dromara.northstar.common.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Contract;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.PositionField;

class ClosingPolicyTest {
	
//	Contract contract = factory.makeContract("rb2210");
	
	PositionField pos0 = PositionField.newBuilder()
			.setContract(contract)
			.build();
	
	PositionField pos1 = PositionField.newBuilder()
			.setContract(contract)
			.setYdPosition(1)
			.setPosition(1)
			.build();
	
	PositionField pos2 = PositionField.newBuilder()
			.setContract(contract)
			.setTdPosition(1)
			.setPosition(1)
			.build();
	
	PositionField pos3 = PositionField.newBuilder()
			.setContract(contract)
			.setTdPosition(1)
			.setYdPosition(1)
			.setPosition(2)
			.build();
	
    @Test
    void testFirstInFirstOutOpen() {
        Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.FIRST_IN_FIRST_OUT.resolve(SignalOperation.BUY_OPEN, pos0, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_Open);
        assertThat(result.t2()).isEqualTo(1);
    }

    @Test
    void testFirstInFirstOutCloseToday() {
        Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.FIRST_IN_FIRST_OUT.resolve(SignalOperation.BUY_CLOSE, pos2, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_CloseToday);
        assertThat(result.t2()).isEqualTo(1);
        
        Tuple<OffsetFlagEnum, Integer> result2 = ClosingPolicy.FIRST_IN_FIRST_OUT.resolve(SignalOperation.BUY_CLOSE, pos2, 2);
        assertThat(result2.t1()).isEqualTo(OffsetFlagEnum.OF_CloseToday);
        assertThat(result2.t2()).isEqualTo(1);
    }

    @Test
    void testFirstInFirstOutCloseYesterday() {
    	Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.FIRST_IN_FIRST_OUT.resolve(SignalOperation.SELL_CLOSE, pos1, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_CloseYesterday);
        assertThat(result.t2()).isEqualTo(1);
        
        Tuple<OffsetFlagEnum, Integer> result2 = ClosingPolicy.FIRST_IN_FIRST_OUT.resolve(SignalOperation.SELL_CLOSE, pos1, 2);
        assertThat(result2.t1()).isEqualTo(OffsetFlagEnum.OF_CloseYesterday);
        assertThat(result2.t2()).isEqualTo(1);
    }
    
    @Test
    void testFirstInFirstOutException() {
        assertThrows(TradeException.class, () -> {
        	ClosingPolicy.FIRST_IN_FIRST_OUT.resolve(SignalOperation.SELL_CLOSE, pos0, 3);
        });
    }
    
    @Test
    void testFirstInLastOutOpen() {
    	Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.FIRST_IN_LAST_OUT.resolve(SignalOperation.BUY_OPEN, pos0, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_Open);
        assertThat(result.t2()).isEqualTo(1);
    }

    @Test
    void testFirstInLastOutCloseToday() {
    	 Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.FIRST_IN_LAST_OUT.resolve(SignalOperation.BUY_CLOSE, pos2, 1);
         assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_CloseToday);
         assertThat(result.t2()).isEqualTo(1);
         
         Tuple<OffsetFlagEnum, Integer> result2 = ClosingPolicy.FIRST_IN_LAST_OUT.resolve(SignalOperation.BUY_CLOSE, pos2, 2);
         assertThat(result2.t1()).isEqualTo(OffsetFlagEnum.OF_CloseToday);
         assertThat(result2.t2()).isEqualTo(1);
    }

    @Test
    void testFirstInLastOutCloseYesterday() {
    	Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.FIRST_IN_LAST_OUT.resolve(SignalOperation.SELL_CLOSE, pos1, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_CloseYesterday);
        assertThat(result.t2()).isEqualTo(1);
        
        Tuple<OffsetFlagEnum, Integer> result2 = ClosingPolicy.FIRST_IN_LAST_OUT.resolve(SignalOperation.SELL_CLOSE, pos1, 2);
        assertThat(result2.t1()).isEqualTo(OffsetFlagEnum.OF_CloseYesterday);
        assertThat(result2.t2()).isEqualTo(1);
    }
    
    @Test
    void testFirstInLastOutException() {
        assertThrows(IllegalStateException.class, () -> {
        	ClosingPolicy.FIRST_IN_LAST_OUT.resolve(SignalOperation.SELL_CLOSE, pos0, 3);
        });
    }
    
    @Test
    void testCloseNonTodayHedgeTodayOpen() {
    	Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolve(SignalOperation.SELL_CLOSE, pos2, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_Open);
        assertThat(result.t2()).isEqualTo(1);
        
        Tuple<OffsetFlagEnum, Integer> result2 = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolve(SignalOperation.BUY_OPEN, pos2, 1);
        assertThat(result2.t1()).isEqualTo(OffsetFlagEnum.OF_Open);
        assertThat(result2.t2()).isEqualTo(1);
    }

    @Test
    void testCloseNonTodayHedgeTodayClose() {
    	Tuple<OffsetFlagEnum, Integer> result = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolve(SignalOperation.SELL_CLOSE, pos1, 1);
        assertThat(result.t1()).isEqualTo(OffsetFlagEnum.OF_CloseYesterday);
        assertThat(result.t2()).isEqualTo(1);
    }
    
}
