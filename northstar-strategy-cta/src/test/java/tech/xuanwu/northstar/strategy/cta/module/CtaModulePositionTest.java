package tech.xuanwu.northstar.strategy.cta.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import tech.xuanwu.northstar.strategy.common.ModulePosition;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class CtaModulePositionTest {

	private ModulePosition mp = new CtaModulePosition();

	private ContractField contract = ContractField.newBuilder()
			.setUnifiedSymbol("rb2110@SHFE@FUTURES")
			.setSymbol("rb2110")
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.setPriceTick(10)
			.setMultiplier(10)
			.build();
	private ContractField contract2 = ContractField.newBuilder()
			.setUnifiedSymbol("rb2111@SHFE@FUTURES")
			.setSymbol("rb2111")
			.setLongMarginRatio(0.08)
			.setShortMarginRatio(0.08)
			.setPriceTick(10)
			.setMultiplier(10)
			.build();

	private TradeField openTrade = TradeField.newBuilder()
			.setDirection(DirectionEnum.D_Buy)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setContract(contract)
			.setTradeTimestamp(LocalDateTime.of(2021, 6, 30, 21, 01).toEpochSecond(ZoneOffset.ofHours(8)) * 1000)
			.setPrice(1234)
			.setVolume(1)
			.build();
	private TradeField openTrade2 = TradeField.newBuilder()
			.setDirection(DirectionEnum.D_Buy)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setContract(contract2)
			.setTradeTimestamp(LocalDateTime.of(2021, 6, 30, 21, 03).toEpochSecond(ZoneOffset.ofHours(8)) * 1000)
			.setPrice(1234)
			.setVolume(1)
			.build();
	private TradeField openTrade3 = TradeField.newBuilder()
			.setDirection(DirectionEnum.D_Buy)
			.setOffsetFlag(OffsetFlagEnum.OF_Open)
			.setTradeTimestamp(LocalDateTime.of(2021, 6, 30, 21, 10).toEpochSecond(ZoneOffset.ofHours(8)) * 1000)
			.setContract(contract)
			.setPrice(1334)
			.setVolume(1)
			.build();
	private TradeField closeTrade = TradeField.newBuilder().setDirection(DirectionEnum.D_Sell)
			.setOffsetFlag(OffsetFlagEnum.OF_Close)
			.setContract(contract)
			.setPrice(1204)
			.setVolume(2)
			.build();

	@Test
	public void testGetOpenningTrade() {
		assertThat(mp.getOpenningTrade()).hasSize(0);
		mp.onTrade(openTrade);
		mp.onTrade(openTrade3);
		
		assertThat(mp.getOpenningTrade()).hasSize(2);
		
		mp.onTrade(closeTrade);
		assertThat(mp.getOpenningTrade()).hasSize(0);
	}

	@Test
	public void testGetPositionDuration() {
		assertThat(mp.getPositionDuration()).isEqualTo(Duration.ZERO);
		mp.onTrade(openTrade);
		mp.onTrade(openTrade3);
		
		Duration expect = Duration.between(LocalDateTime.of(2021, 6, 30, 21, 01), LocalDateTime.now());
		assertThat(expect.toSeconds()).isEqualTo(mp.getPositionDuration().toSeconds());
	}

	@Test
	public void testGetPositionProfit() {
		assertThat(mp.getPositionProfit()).isEqualTo(0);
		mp.onTrade(openTrade);
		mp.onTrade(openTrade3);
		mp.onTick(TickField.newBuilder()
				.setUnifiedSymbol("rb2110@SHFE@FUTURES")
				.setLastPrice(1434)
				.build());
		
		assertThat(mp.getPositionProfit()).isEqualTo(3000);
	}
	
	@Test
	public void testTradeUpdateForDiffTrade() {
		assertThat(mp.getOpenningTrade()).hasSize(0);
		mp.onTrade(openTrade);
		mp.onTrade(openTrade2);
		
		assertThat(mp.getOpenningTrade()).hasSize(1);
	}

}
