package tech.xuanwu.northstar.strategy.common.model.state;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;

public class ModuleStateMachineTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/****************/ 
	/** 开仓成功场景 **/
	/****************/
	//多开
	@Test
	public void testBuyOpen() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.EMPTY);
		assertThat(state.transformForm(ModuleEventType.OPENING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.BUY_TRADED)).isEqualTo(ModuleState.HOLDING_LONG);
	}
	
	//空开
	@Test
	public void testSellOpen() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.EMPTY);
		assertThat(state.transformForm(ModuleEventType.OPENING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.SELL_TRADED)).isEqualTo(ModuleState.HOLDING_SHORT);
	}
	
	/****************/ 
	/** 开仓失败场景 **/
	/****************/
	@Test
	public void testOpenFallback() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.EMPTY);
		assertThat(state.transformForm(ModuleEventType.OPENING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.SIGNAL_RETAINED)).isEqualTo(ModuleState.EMPTY);
		
		assertThat(state.transformForm(ModuleEventType.OPENING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.REJECT_RISK_ALERTED)).isEqualTo(ModuleState.RETRIEVING_FOR_CANCAL);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.EMPTY);
		
		assertThat(state.transformForm(ModuleEventType.OPENING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.RETRY_RISK_ALERTED)).isEqualTo(ModuleState.RETRIEVING_FOR_RETRY);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.EMPTY);
	}
	
	/****************/ 
	/** 平仓成功场景 **/
	/****************/
	//多平成功
	@Test
	public void testBuyClose() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.HOLDING_SHORT);
		assertThat(state.transformForm(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.BUY_TRADED)).isEqualTo(ModuleState.EMPTY);
	}
	
	//空平成功
	@Test
	public void testSellClose() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.HOLDING_LONG);
		assertThat(state.transformForm(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.SELL_TRADED)).isEqualTo(ModuleState.EMPTY);
	}
	
	/****************/ 
	/** 平仓失败场景 **/
	/****************/
	//多平失败
	@Test
	public void testBuyCloseFallback() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.HOLDING_SHORT);
		assertThat(state.transformForm(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.REJECT_RISK_ALERTED)).isEqualTo(ModuleState.RETRIEVING_FOR_CANCAL);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.HOLDING_SHORT);
		
		assertThat(state.transformForm(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.RETRY_RISK_ALERTED)).isEqualTo(ModuleState.RETRIEVING_FOR_RETRY);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.HOLDING_SHORT);
	}
	
	//空平失败
	@Test
	public void testSellCloseFallback() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.HOLDING_LONG);
		assertThat(state.transformForm(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.REJECT_RISK_ALERTED)).isEqualTo(ModuleState.RETRIEVING_FOR_CANCAL);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.HOLDING_LONG);
		
		assertThat(state.transformForm(ModuleEventType.CLOSING_SIGNAL_CREATED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.RETRY_RISK_ALERTED)).isEqualTo(ModuleState.RETRIEVING_FOR_RETRY);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.PLACING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_SUBMITTED)).isEqualTo(ModuleState.PENDING_ORDER);
		assertThat(state.transformForm(ModuleEventType.ORDER_CANCELLED)).isEqualTo(ModuleState.HOLDING_LONG);
	}
	
	/***************/ 
	/**  止损场景  **/
	/***************/
	@Test
	public void testLongPositionStopLoss() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.HOLDING_LONG);
		assertThat(state.transformForm(ModuleEventType.STOP_LOSS)).isEqualTo(ModuleState.EMPTY);
	}
	
	@Test
	public void testShortPositionStopLoss() {
		ModuleStateMachine state = new ModuleStateMachine(ModuleState.HOLDING_SHORT);
		assertThat(state.transformForm(ModuleEventType.STOP_LOSS)).isEqualTo(ModuleState.EMPTY);
	}
	
	/****************/
	/**	其他异常情况 **/
	/****************/
	//
}
