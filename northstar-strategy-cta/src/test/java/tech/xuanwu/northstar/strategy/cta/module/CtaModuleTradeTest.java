package tech.xuanwu.northstar.strategy.cta.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import tech.xuanwu.northstar.strategy.common.ModuleTrade;
import tech.xuanwu.northstar.strategy.common.model.TradeDescription;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModuleTradeTest {

	private ModuleTrade mt = new CtaModuleTrade();

	private ContractField contract = ContractField.newBuilder().setUnifiedSymbol("rb2110@SHFE@FUTURES")
			.setSymbol("rb2110")
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.setPriceTick(10)
			.setMultiplier(10)
			.build();

	private TradeField openTrade = TradeField.newBuilder().setDirection(DirectionEnum.D_Buy)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setContract(contract)
			.setPrice(1234)
			.setVolume(1)
			.build();

	private TradeField openTrade2 = TradeField.newBuilder().setDirection(DirectionEnum.D_Buy)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setContract(contract)
			.setPrice(1234)
			.setVolume(2)
			.build();

	private TradeField closeTrade = TradeField.newBuilder().setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Close)
			.setContract(contract)
			.setPrice(1204)
			.setVolume(1)
			.build();

	private TradeField closeTrade2 = TradeField.newBuilder().setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Close)
			.setContract(contract)
			.setPrice(1204)
			.setVolume(2)
			.build();

	@Test
	public void testGetDealRecordsAsEvenBuyAndSell() {
		assertThat(mt.getDealRecords()).isEmpty();
		mt.updateTrade(TradeDescription.convertFrom("testModule", openTrade));
		mt.updateTrade(TradeDescription.convertFrom("testModule", closeTrade));

		assertThat(mt.getDealRecords()).hasSize(1);
	}

	@Test
	public void testGetDealRecordsAsBigBuyAndSmallSell() {
		assertThat(mt.getDealRecords()).isEmpty();
		mt.updateTrade(TradeDescription.convertFrom("testModule", openTrade2));
		mt.updateTrade(TradeDescription.convertFrom("testModule", closeTrade));
		mt.updateTrade(TradeDescription.convertFrom("testModule", closeTrade));

		assertThat(mt.getDealRecords()).hasSize(2);
	}

	@Test
	public void testGetDealRecordsAsSmallBuyAndBigSell() {
		assertThat(mt.getDealRecords()).isEmpty();
		mt.updateTrade(TradeDescription.convertFrom("testModule", openTrade));
		mt.updateTrade(TradeDescription.convertFrom("testModule", openTrade));
		mt.updateTrade(TradeDescription.convertFrom("testModule", closeTrade2));

		assertThat(mt.getDealRecords()).hasSize(2);
	}

	@Test
	public void testGetTotalCloseProfit() {
		assertThat(mt.getTotalCloseProfit()).isEqualTo(0);
		mt.updateTrade(TradeDescription.convertFrom("testModule", openTrade));
		mt.updateTrade(TradeDescription.convertFrom("testModule", closeTrade));

		assertThat(mt.getTotalCloseProfit()).isEqualTo(-300);
	}

}
