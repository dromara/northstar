package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.strategy.api.StateChangeListener;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;

public class ModuleStateMachineTest {


	/****************/ 
	/** 开仓成功场景 **/
	/****************/
	//多开
	@Test
	public void testBuyOpen() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.BUY_TRADED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.HOLDING_LONG);
	}
	
	//空开
	@Test
	public void testSellOpen() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.SELL_TRADED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.HOLDING_SHORT);
	}
	
	/****************/ 
	/** 开仓失败场景 **/
	/****************/
	@Test
	public void testOpenFallback() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_REQ_RETAINED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.EMPTY);
		
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.REJECT_RISK_ALERTED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.RETRIEVING_FOR_CANCEL);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.EMPTY);
		
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.RETRY_RISK_ALERTED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.RETRIEVING_FOR_RETRY);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.EMPTY);
		
	}
	
	/****************/ 
	/** 平仓成功场景 **/
	/****************/
	//多平成功
	@Test
	public void testBuyClose() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.HOLDING_SHORT);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.BUY_TRADED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.EMPTY);
	}
	
	//空平成功
	@Test
	public void testSellClose() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.HOLDING_LONG);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.SELL_TRADED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.EMPTY);
	}
	
	/****************/ 
	/** 平仓失败场景 **/
	/****************/
	//多平失败
	@Test
	public void testBuyCloseFallback() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.HOLDING_SHORT);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.REJECT_RISK_ALERTED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.RETRIEVING_FOR_CANCEL);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.HOLDING_SHORT);
		
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.RETRY_RISK_ALERTED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.RETRIEVING_FOR_RETRY);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.HOLDING_SHORT);
	}
	
	//空平失败
	@Test
	public void testSellCloseFallback() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.HOLDING_LONG);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.REJECT_RISK_ALERTED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.RETRIEVING_FOR_CANCEL);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.HOLDING_LONG);
		
		state.transformForm(ModuleEventType.SIGNAL_CREATED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.RETRY_RISK_ALERTED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.RETRIEVING_FOR_RETRY);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CANCELLED);
		assertThat(state.getCurState()).isEqualTo(ModuleState.HOLDING_LONG);
		
	}
	
	/***************/ 
	/**  止损场景  **/
	/***************/
	@Test
	public void testLongPositionStopLoss() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.HOLDING_LONG);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.STOP_LOSS);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
	}
	
	@Test
	public void testShortPositionStopLoss() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.HOLDING_SHORT);
		StateChangeListener listener = mock(StateChangeListener.class);
		state.addStateChangeListener(listener);
		state.transformForm(ModuleEventType.STOP_LOSS);
		assertThat(state.getCurState()).isEqualTo(ModuleState.PLACING_ORDER);
	}
	
	/****************/
	/**	其他异常情况 **/
	/****************/
	@Test
	public void testCreateSignal() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.PENDING_ORDER);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.SIGNAL_CREATED);
		});
	}
	
	@Test
	public void testOrderReqCreated() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.PENDING_ORDER);
		state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		state.transformForm(ModuleEventType.ORDER_REQ_CREATED);
		assertThat(state.getState()).isEqualTo(ModuleState.PENDING_ORDER);
	}
	
	@Test
	public void testOrderReqRetained() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.ORDER_REQ_RETAINED);
		});
	}
	
	@Test
	public void testSellTraded() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{
			state.transformForm(ModuleEventType.SELL_TRADED);
		});
	}
	
	@Test
	public void testBuyTraded() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.BUY_TRADED);
		});
	}
	
	@Test
	public void testOrderConfirmed() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.ORDER_CONFIRMED);
		});
	}
	
	@Test
	public void testRetryRiskAlerted() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.RETRY_RISK_ALERTED);
		});
	}
	
	@Test
	public void testRejectRiskAlerted() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.REJECT_RISK_ALERTED);
		});
	}
	
	@Test
	public void testOrderCancelled() {
		ModuleStateMachine state = new ModuleStateMachine("test", ModuleState.EMPTY);
		assertThrows(IllegalStateException.class, ()->{			
			state.transformForm(ModuleEventType.ORDER_CANCELLED);
		});
	}
}
