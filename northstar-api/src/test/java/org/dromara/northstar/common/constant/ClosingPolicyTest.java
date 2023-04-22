package org.dromara.northstar.common.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

class ClosingPolicyTest {
	
	TestFieldFactory factory = new TestFieldFactory("testAccount");
	ContractField contract = factory.makeContract("rb2210");
	
	TradeField trade1 = factory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, "20230421");
	TradeField trade2 = factory.makeTradeField("rb2210", 1000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, "20230422");
	
    @Test
    void testFirstInFirstOutOpen() {
        String tradingDay = "20230421";
        List<TradeField> nonclosedTrades = List.of();

        OffsetFlagEnum result = ClosingPolicy.FIRST_IN_FIRST_OUT.resolveOperation(SignalOperation.BUY_OPEN, contract, nonclosedTrades, tradingDay);
        assertEquals(OffsetFlagEnum.OF_Open, result);
    }

    @Test
    void testFirstInFirstOutCloseToday() {
        String tradingDay = "20230422";
        List<TradeField> nonclosedTrades = List.of(trade2);

        OffsetFlagEnum result = ClosingPolicy.FIRST_IN_FIRST_OUT.resolveOperation(SignalOperation.BUY_CLOSE, contract, new ArrayList<>(nonclosedTrades), tradingDay);
        assertEquals(OffsetFlagEnum.OF_CloseToday, result);
    }

    @Test
    void testFirstInFirstOutClose() {
        String tradingDay = "20230422";
        
        OffsetFlagEnum result = ClosingPolicy.FIRST_IN_FIRST_OUT.resolveOperation(SignalOperation.SELL_CLOSE, contract, new ArrayList<>(List.of(trade1)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Close, result);
        
        OffsetFlagEnum result2 = ClosingPolicy.FIRST_IN_FIRST_OUT.resolveOperation(SignalOperation.SELL_CLOSE, contract, new ArrayList<>(List.of(trade1, trade2)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Close, result2);
    }
    
    @Test
    void testFirstInLastOutOpen() {
        String tradingDay = "20230421";
        List<TradeField> nonclosedTrades = List.of();

        OffsetFlagEnum result = ClosingPolicy.FIRST_IN_LAST_OUT.resolveOperation(SignalOperation.BUY_OPEN, contract, nonclosedTrades, tradingDay);
        assertEquals(OffsetFlagEnum.OF_Open, result);
    }

    @Test
    void testFirstInLastOutCloseToday() {
        String tradingDay = "20230422";
        
        OffsetFlagEnum result = ClosingPolicy.FIRST_IN_LAST_OUT.resolveOperation(SignalOperation.BUY_CLOSE, contract, new ArrayList<>(List.of(trade2)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_CloseToday, result);
        
        OffsetFlagEnum result2 = ClosingPolicy.FIRST_IN_LAST_OUT.resolveOperation(SignalOperation.BUY_CLOSE, contract, new ArrayList<>(List.of(trade1, trade2)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_CloseToday, result2);
    }

    @Test
    void testFirstInLastOutClose() {
        String tradingDay = "20230422";
        List<TradeField> nonclosedTrades = List.of(trade1);

        OffsetFlagEnum result = ClosingPolicy.FIRST_IN_LAST_OUT.resolveOperation(SignalOperation.SELL_CLOSE, contract, new ArrayList<>(nonclosedTrades), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Close, result);
    }
    
    @Test
    void testCloseNonTodayHedgeTodayOpen() {
    	String tradingDay = "20230422";

        OffsetFlagEnum result = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolveOperation(SignalOperation.SELL_OPEN, contract, List.of(), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Open, result);
        
        OffsetFlagEnum result2 = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolveOperation(SignalOperation.SELL_CLOSE, contract, new ArrayList<>(List.of(trade2)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Open, result2);
    }

    @Test
    void testCloseNonTodayHedgeTodayClose() {
    	String tradingDay = "20230422";

        OffsetFlagEnum result = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolveOperation(SignalOperation.SELL_CLOSE, contract, new ArrayList<>(List.of(trade1)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Close, result);
        
        OffsetFlagEnum result2 = ClosingPolicy.CLOSE_NONTODAY_HEGDE_TODAY.resolveOperation(SignalOperation.SELL_CLOSE, contract, new ArrayList<>(List.of(trade1, trade2)), tradingDay);
        assertEquals(OffsetFlagEnum.OF_Close, result2);
    }
    
}
