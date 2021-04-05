package tech.xuanwu.northstar.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import xyz.redtorch.pb.CoreField.TradeField;

public class TradeDayTransactionTest {
	
	TradeDayTransaction tdt = new TradeDayTransaction();
	

	@Test
	public void testUpdate() {
		TradeField trade1 = TradeField.newBuilder()
				.setTradeId("123")
				.build();
		TradeField trade2 = TradeField.newBuilder()
				.setTradeId("456")
				.build();
		
		tdt.update(trade1);
		tdt.update(trade2);
		assertThat(tdt.getTransactions().size()).isEqualTo(2);
	}

}
