package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.dromara.northstar.common.model.SimAccountDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.ISimAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

@DataJpaTest
class SimAccountRepoAdapterTest {

	@Autowired
	SimAccountRepository delegate;

	static ISimAccountRepository repo;

	String accountId = "testAccount";

	Trade trade = Trade.builder()
			.contract(Contract.builder().unifiedSymbol("rb2401").build())
			.volume(1000)
			.price(2)
			.direction(DirectionEnum.D_Buy)
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.build();

	SimAccountDescription simAcc = SimAccountDescription.builder()
			.gatewayId(accountId)
			.totalDeposit(10000)
			.totalCommission(30)
			.openTrades(List.of(trade))
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
