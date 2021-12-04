package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.domain.strategy.ModuleManager;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import test.common.TestFieldFactory;

public class ModuleManagerTest {

	ModuleManager mdlMgr;
	
	StrategyModule module;
	
	final String NAME = "testModule";
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	@Before
	public void setUp() {
		mdlMgr = new ModuleManager();
		
		module = mock(StrategyModule.class);
		when(module.getName()).thenReturn(NAME);
	}

	@Test
	public void shouldAddSuccessfully() {
		mdlMgr.addModule(module);
		assertThat(mdlMgr.getModule(NAME)).isEqualTo(module);
	}
	
	@Test
	public void shouldRemoveSuccessfully() {
		mdlMgr.addModule(module);
		assertThat(mdlMgr.removeModule(NAME)).isEqualTo(module);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowIfModuleEnableWhenRemoving() {
		when(module.isEnabled()).thenReturn(true);
		mdlMgr.addModule(module);
		assertThat(mdlMgr.removeModule(NAME)).isEqualTo(module);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowIfNotExist() {
		mdlMgr.getModule("any");
	}
	
	@Test
	public void testCanHandle() {
		assertThat(mdlMgr.canHandle(NorthstarEventType.TICK)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.BAR)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.ORDER)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.TRADE)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.IDX_TICK)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.EXT_MSG)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.ACCOUNT)).isTrue();
	}

	@Test
	public void testDoHandle() {
		mdlMgr.addModule(module);
		mdlMgr.doHandle(mock(NorthstarEvent.class));
		verify(module).onEvent(ArgumentMatchers.any(NorthstarEvent.class));
	}
}
