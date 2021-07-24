package tech.xuanwu.northstar.strategy.cta.module.signal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import tech.xuanwu.northstar.common.utils.CommonUtils;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;

public class PAExtSignalPolicyTest {
	
	private PAExtSignalPolicy policy = new PAExtSignalPolicy();

	@Test
	public void testOnExtMsg() {
		PAExtSignalPolicy.InitParams initParam = new PAExtSignalPolicy.InitParams();
		initParam.unifiedSymbol = "rb2110@SHFE@FUTURES";
		policy.initWithParams(initParam);
		ModuleEventBus meb = mock(ModuleEventBus.class);
		
		String textReverse = "【XX期货】：RB2110在5314.0的价格平空单，在5314.0的价格开多单，止损价：5271.0，目前持有多单1手（+1），仅供参考。";
		policy.onExtMsg(textReverse);
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent>() {
			@Override
			public boolean matches(ModuleEvent event) {
				CtaSignal signal = (CtaSignal) event.getData();
				return event.getEventType() == ModuleEventType.SIGNAL_CREATED 
						&& signal.getState() == SignalOperation.ReversingBuy && CommonUtils.isEquals(signal.getSignalPrice(), 5314.0);
			}
		}));
		
		String textClose = "【XX期货】：RB2110在5314.0的价格平空单，仅供参考。";
		policy.onExtMsg(textClose);
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent>() {
			@Override
			public boolean matches(ModuleEvent event) {
				CtaSignal signal = (CtaSignal) event.getData();
				return event.getEventType() == ModuleEventType.SIGNAL_CREATED 
						&& signal.getState() == SignalOperation.BuyClose && CommonUtils.isEquals(signal.getSignalPrice(), 5314.0);
			}
		}));
		
		String textOpen = "【XX期货】：RB2110在5314.0的价格开多单，止损价：5271.0，目前持有多单1手（+1），仅供参考。";
		policy.onExtMsg(textOpen);
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent>() {
			@Override
			public boolean matches(ModuleEvent event) {
				CtaSignal signal = (CtaSignal) event.getData();
				return event.getEventType() == ModuleEventType.SIGNAL_CREATED 
						&& signal.getState() == SignalOperation.BuyOpen && CommonUtils.isEquals(signal.getSignalPrice(), 5314.0);
			}
		}));
	}

}
