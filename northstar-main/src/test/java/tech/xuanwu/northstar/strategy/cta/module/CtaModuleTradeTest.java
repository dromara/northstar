package tech.xuanwu.northstar.strategy.cta.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Test;

import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModuleTradeTest {
	
	private ModuleTrade mt = new CtaModuleTrade();
	
	private ContractField contract = ContractField.newBuilder()
										.setUnifiedSymbol("rb2110@SHFE@FUTURES")
										.setSymbol("rb2110")
										.setLongMarginRatio(0.08)
										.setShortMarginRatio(0.08)
										.setPriceTick(10)
										.setMultiplier(10)
										.build();
	
	private TradeField openTrade = TradeField.newBuilder()
										.setDirection(DirectionEnum.D_Buy)
										.setOffsetFlag(OffsetFlagEnum.OF_Open)
										.setContract(contract)
										.setPrice(1234)
										.build();
	private TradeField closeTrade = TradeField.newBuilder()
										.setDirection(DirectionEnum.D_Buy)
										.setOffsetFlag(OffsetFlagEnum.OF_Open)
										.setContract(contract)
										.setPrice(1204)
										.build();
	
	
	@Test
	public void testGetDealRecords() {
		assertThat(mt.getDealRecords()).isEmpty();
		mt.updateTrade(openTrade);
		mt.updateTrade(closeTrade);
		
		assertThat(mt.getDealRecords()).hasSize(1);
	}

	@Test
	public void testGetTotalCloseProfit() {
		assertThat(mt.getTotalCloseProfit()).isEqualTo(0);
		mt.updateTrade(openTrade);
		mt.updateTrade(closeTrade);
		
		assertThat(mt.getTotalCloseProfit()).isEqualTo(-300);
	}

	@Test
	public void testGetOriginRecords() {
		assertThat(mt.getOriginRecords()).isEmpty();
		mt.updateTrade(openTrade);
		mt.updateTrade(closeTrade);
		
		assertThat(mt.getOriginRecords()).hasSize(2);
		
	}

}
