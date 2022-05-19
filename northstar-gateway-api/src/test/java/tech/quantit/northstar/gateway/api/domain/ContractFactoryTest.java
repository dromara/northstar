package tech.quantit.northstar.gateway.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.GatewayType;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

class ContractFactoryTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");
	List<ContractField> list = new ArrayList<>();
	
	@BeforeEach
	void prepare() {
		String[] names = {"rb2210@SHFE@FUTURES", "rb2212@SHFE@FUTURES", "AP110@CZE@FUTURES", "AP210@CZE@FUTURES", "M2210@DZE@FUTURES", "M2209@DZE@FUTURES"};
		list.clear();
		for(String name : names) {
			ContractField c = ContractField.newBuilder()
					.setGatewayId("gateway")
					.setCurrency(CurrencyEnum.CNY)
					.setContractId(name + "@gateway")
					.setExchange(ExchangeEnum.SHFE)
					.setThirdPartyId("gateway#" + GatewayType.CTP)
					.setFullName(name)
					.setLongMarginRatio(0.08)
					.setShortMarginRatio(0.08)
					.setMultiplier(10)
					.setPriceTick(1)
					.setProductClass(ProductClassEnum.FUTURES)
					.setUnifiedSymbol(name)
					.setSymbol(name)
					.build();
			list.add(c);
		}
	}

	@Test
	void testMakeIndexContract() {
		ContractFactory contractFactory = new ContractFactory(list);
		List<IndexContract> idxList = contractFactory.makeIndexContract();
		String[] expectedName = {"rb0000@SHFE@FUTURES", "AP0000@CZE@FUTURES", "M0000@DZE@FUTURES"};
		Set<String> nameSet = Set.of(expectedName);
		assertThat(idxList).hasSize(3);
		for(IndexContract idxContract : idxList) {
			assertThat(nameSet.contains(idxContract.unifiedSymbol())).isTrue();
			System.out.println(idxContract.field);
		}
	}

}
