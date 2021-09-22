package tech.xuanwu.northstar.main.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;
import tech.xuanwu.northstar.strategy.common.StrategyModule;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleManagerTest {
	
	ModuleManager mdlMgr;
	
	StrategyModule module;
	
	final String NAME = "testModule";
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	@Before
	public void setUp() {
		ModuleRepository mdlRepo = mock(ModuleRepository.class);
		mdlMgr = new ModuleManager(mdlRepo);
		
		module = mock(StrategyModule.class);
		when(module.getName()).thenReturn(NAME);
	}

	@Test
	public void shouldAddSuccessfully() {
		mdlMgr.addModule(module);
		assertThat(mdlMgr.getModule(NAME)).isEqualTo(module);
	}
	
	public void shouldRemoveSuccessfully() {
		mdlMgr.addModule(module);
		assertThat(mdlMgr.removeModule(NAME)).isEqualTo(module);
	}

	@Test
	public void testToggleState() {
		mdlMgr.addModule(module);
		mdlMgr.toggleState(NAME);
		verify(module).toggleRunningState();
	}

	@Test
	public void testOnTick() {
		mdlMgr.addModule(module);
		TickField tick = factory.makeTickField("rb2210", 1213);
		mdlMgr.onTick(tick);
		verify(module).onTick(tick);
	}

	@Test
	public void testOnBar() {
		mdlMgr.addModule(module);
		BarField bar = BarField.newBuilder().setUnifiedSymbol("rb2210@SHFE@FUTURES").build();
		mdlMgr.onBar(bar);
		verify(module).onBar(bar);
	}

	@Test
	public void testOnOrder() {
		mdlMgr.addModule(module);
		OrderField order = OrderField.newBuilder().build();
		mdlMgr.onOrder(order);
		verify(module).onOrder(order);
	}

	@Test
	public void testOnTrade() {
		mdlMgr.addModule(module);
		TradeField trade = factory.makeTradeField("rb2210", 1345, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		mdlMgr.onTrade(trade);
		verify(module).onTrade(trade);
	}

	@Test
	public void testOnAccount() {
		mdlMgr.addModule(module);
		AccountField account = AccountField.newBuilder().build();
		mdlMgr.onAccount(account);
		verify(module).onAccount(account);
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

}
