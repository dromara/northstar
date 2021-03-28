package tech.xuanwu.northstar.engin.broadcast;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOServer;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.EventEngine;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class SocketIOMessageEngineTest {
	
	SocketIOMessageEngine ngin;
	
	@Before
	public void init() {
		EventEngine ee = mock(EventEngine.class);
		SocketIOServer server = mock(SocketIOServer.class);
		BroadcastOperations bo = mock(BroadcastOperations.class);
		when(server.getBroadcastOperations()).thenReturn(bo);
		
		ngin = new SocketIOMessageEngine(ee, server);
	}

	@Test
	public void testTickFieldOnEvent() throws Exception {
		TickField t = TickField.newBuilder().build();
		ngin.onEvent(new NorthstarEvent(NorthstarEventType.TICK, t), 0L, true);
	}
	
	@Test
	public void testBarFieldOnEvent() throws Exception {
		BarField b = BarField.newBuilder().build();
		ngin.onEvent(new NorthstarEvent(NorthstarEventType.BAR, b),  0L, true);
	}

	@Test
	public void testAccountFieldOnEvent() throws Exception {
		
	}
}
