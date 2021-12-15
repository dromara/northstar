package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.ContractField;

class IndexContractTest {

	private TestFieldFactory factory = new TestFieldFactory("gateway");
	
	Set<ContractField> contracts = Set.of(factory.makeContract("rb2201"), factory.makeContract("rb2205"), factory.makeContract("rb2210"));
	IndexContract idxContract = new IndexContract("rb0000@SHFE@FUTURES", contracts);

	@Test
	void testIndexTicker() {
		assertThat(idxContract.indexTicker()).isNotNull();
	}

	@Test
	void testMonthlyContractSymbols() {
		assertThat(idxContract.monthlyContractSymbols()).hasSize(3);
	}

}
