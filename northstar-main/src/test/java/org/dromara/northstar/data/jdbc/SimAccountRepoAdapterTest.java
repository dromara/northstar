package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;

import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.google.protobuf.InvalidProtocolBufferException;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@DataJpaTest
class SimAccountRepoAdapterTest {

	@Autowired
	SimAccountRepository delegate;

	static ISimAccountRepository repo;

	String accountId = "testAccount";
	
	Contract contract = Contract.builder().contractId("001").unifiedSymbol("rb2401").build();

	Trade trade = Trade.builder()
			.contract(contract)
			.volume(1000)
			.price(2)
			.tradeDate(LocalDate.now())
			.tradeTime(LocalTime.now())
			.tradingDay(LocalDate.now())
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.build();

	SimAccountDescription simAcc = SimAccountDescription.builder()
			.gatewayId(accountId)
			.totalDeposit(10000)
			.totalCommission(30)
			.openTrades(List.of(trade.toTradeField().toByteArray()))
			.build();

	@BeforeEach
	void prepare() {
		repo = new SimAccountRepoAdapter(delegate);
	}

	@Test
	void testSave() {
		repo.save(simAcc);
		Iterator<?> it = delegate.findAll().iterator();
		assertThat(it.next()).isNotNull();
	}

	@Test
	void testFindById() {
		testSave();
		SimAccountDescription result = repo.findById(accountId);
		assertThat(result).isEqualToIgnoringGivenFields(simAcc, "openTrades");
	}
	
	TradeField parse(byte[] data) {
		try {
			return TradeField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			return null;
		}
		
	}

	@Test
	void testDeleteById() {
		repo.save(simAcc);
		repo.deleteById(accountId);
		assertThat(delegate.findAll().iterator().hasNext()).isFalse();
	}

}
