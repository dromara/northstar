package tech.quantit.northstar.domain.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

class RiskControlPolicyTest {
	
	RiskControlRule r1 = mock(RiskControlRule.class);
	RiskControlRule r2 = mock(RiskControlRule.class);
	RiskControlRule r3 = mock(RiskControlRule.class);
	
	ModuleEventBus meb = mock(ModuleEventBus.class);
	TestFieldFactory factory = new TestFieldFactory("gateway");
	String name = "module";

	@BeforeEach
	void prepare() {
		when(r1.checkRisk(any(), any())).thenReturn(RiskAuditResult.ACCEPTED);
		when(r2.checkRisk(any(), any())).thenReturn(RiskAuditResult.REJECTED);
		when(r3.checkRisk(any(), any())).thenReturn(RiskAuditResult.RETRY);
	}
	
	@Test
	void shouldNotGetAnything() {
		ModuleEvent<?> event = new ModuleEvent<>(ModuleEventType.SIGNAL_CREATED, factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		TickField tick = factory.makeTickField("rb2210", 1000);
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r1, r2, r3));
		p.setEventBus(meb);
		p.onTick(tick);
		p.onEvent(event);
		
		verify(meb, times(0)).post(any());
	}

	@Test
	void shouldGetRetain() {
		ModuleEvent<?> event = new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		TickField tick = factory.makeTickField("rb2210", 1000);
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r1, r2, r3));
		p.setEventBus(meb);
		p.onTick(tick);
		p.onEvent(event);
		
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {

			@Override
			public boolean matches(ModuleEvent<?> arg) {
				return arg.getEventType() == ModuleEventType.ORDER_REQ_RETAINED;
			}
		}));
	}
	
	@Test
	void shouldGetRetain2() {
		ModuleEvent<?> event = new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r1, r2, r3));
		p.setEventBus(meb);
		p.onEvent(event);
		
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {

			@Override
			public boolean matches(ModuleEvent<?> arg) {
				return arg.getEventType() == ModuleEventType.ORDER_REQ_RETAINED;
			}
		}));
	}
	
	@Test
	void shouldGetRetry() {
		ModuleEvent<?> event = new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		TickField tick = factory.makeTickField("rb2210", 1000);
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r1, r3));
		p.setEventBus(meb);
		p.onTick(tick);
		p.onEvent(event);
		
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {

			@Override
			public boolean matches(ModuleEvent<?> arg) {
				return arg.getEventType() == ModuleEventType.ORDER_REQ_RETAINED;
			}
		}));
	}
	
	@Test
	void shouldGetAccept() {
		ModuleEvent<?> event = new ModuleEvent<>(ModuleEventType.ORDER_REQ_CREATED, factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 1000, 0));
		TickField tick = factory.makeTickField("rb2210", 1000);
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r1));
		p.setEventBus(meb);
		p.lastTick = tick;
		p.onEvent(event);
		
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {

			@Override
			public boolean matches(ModuleEvent<?> arg) {
				return arg.getEventType() == ModuleEventType.ORDER_REQ_ACCEPTED;
			}
		}));
	}
	
	@Test
	void shouldGetRejectOrder() {
		TickField tick = factory.makeTickField("rb2210", 1000);
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 0, 0, 0);
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r2));
		p.setEventBus(meb);
		p.currentOrderReq = orderReq;
		p.onChange(ModuleState.PENDING_ORDER);
		p.onTick(tick);
		
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {

			@Override
			public boolean matches(ModuleEvent<?> arg) {
				return arg.getEventType() == ModuleEventType.REJECT_RISK_ALERTED;
			}
		}));
	}
	
	@Test
	void shouldGetRetryOrder() {
		TickField tick = factory.makeTickField("rb2210", 1000);
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 0, 0, 0);
		RiskControlPolicy p = new RiskControlPolicy(name, List.of(r3));
		p.currentOrderReq = orderReq;
		p.setEventBus(meb);
		p.onChange(ModuleState.PENDING_ORDER);
		p.onTick(tick);
		
		verify(meb).post(argThat(new ArgumentMatcher<ModuleEvent<?>>() {

			@Override
			public boolean matches(ModuleEvent<?> arg) {
				return arg.getEventType() == ModuleEventType.RETRY_RISK_ALERTED;
			}
		}));
	}

}
