package org.dromara.northstar.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.dromara.northstar.gateway.api.domain.contract.ContractDefinition;
import org.dromara.northstar.support.utils.ContractDefinitionReader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;


class ContractDefinitionReaderTest {

	ContractDefinitionReader reader = new ContractDefinitionReader();
	
	@Test
	void test() throws IOException {
		String directoryName = System.getProperty("user.dir");
		Resource res = new FileUrlResource(directoryName + "/../northstar-main/src/main/resources/ContractDefinition.csv");
		List<ContractDefinition> results = reader.load(res.getInputStream());
		assertThat(results).isNotEmpty();
	}

}
