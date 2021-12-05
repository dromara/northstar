package tech.quantit.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import test.common.TestFieldFactory;

class ModuleManagerTest {

	ModuleManager mdlMgr;
	
	StrategyModule module;
	
	final String NAME = "testModule";
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	@BeforeEach
	void setUp() {
		mdlMgr = new ModuleManager();
		
		module = mock(StrategyModule.class);
		when(module.canHandle(any())).thenReturn(true);
		when(module.getName()).thenReturn(NAME);
	}

	@Test
	void shouldAddSuccessfully() {
		mdlMgr.addModule(module);
		assertThat(mdlMgr.getModule(NAME)).isEqualTo(module);
	}
	
	@Test
	void shouldRemoveSuccessfully() {
		mdlMgr.addModule(module);
		assertThat(mdlMgr.removeModule(NAME)).isEqualTo(module);
	}
	
	@Test
	void shouldThrowIfModuleEnableWhenRemoving() {
		when(module.isEnabled()).thenReturn(true);
		mdlMgr.addModule(module);
		assertThrows(IllegalStateException.class, ()->{			
			mdlMgr.removeModule(NAME);
		});
	}
	
	@Test
	void shouldThrowIfNotExist() {
		assertThrows(IllegalStateException.class, ()->{			
			mdlMgr.getModule("any");
		});
	}
	
	@Test
	void testCanHandle() {
		assertThat(mdlMgr.canHandle(NorthstarEventType.TICK)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.BAR)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.ORDER)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.TRADE)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.IDX_TICK)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.EXT_MSG)).isTrue();
		assertThat(mdlMgr.canHandle(NorthstarEventType.ACCOUNT)).isTrue();
	}

	@Test
	void testDoHandle() {
		mdlMgr.addModule(module);
		mdlMgr.doHandle(mock(NorthstarEvent.class));
		verify(module).onEvent(ArgumentMatchers.any(NorthstarEvent.class));
	}
}
