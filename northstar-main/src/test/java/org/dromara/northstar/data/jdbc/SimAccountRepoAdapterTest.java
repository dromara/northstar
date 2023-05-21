package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.data.ISimAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@DataJpaTest
class SimAccountRepoAdapterTest {
	
	@Autowired
	SimAccountRepository delegate;
	
	static ISimAccountRepository repo;
	
	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	String accountId = "testAccount";
	
	TradeField trade = fieldFactory.makeTradeField("rb2210", 1000, 2, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	
	SimAccountDescription simAcc = SimAccountDescription.builder()
			.gatewayId(accountId)
			.totalDeposit(10000)
			.totalCommission(30)
			.openTrades(List.of(trade.toByteArray()))
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
		assertThat(repo.findById(accountId)).isEqualTo(simAcc);
	}

	@Test
	void testDeleteById() {
		repo.save(simAcc);
		repo.deleteById(accountId);
		assertThat(delegate.findAll().iterator().hasNext()).isFalse();
	}

}
