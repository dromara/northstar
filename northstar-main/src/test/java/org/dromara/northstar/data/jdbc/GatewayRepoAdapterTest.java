package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.dromara.northstar.data.IGatewayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.MessageSource;

@DataJpaTest
class GatewayRepoAdapterTest {

    @Autowired
    private GatewayDescriptionRepository delegate;
    
    private IGatewayRepository repo;
    
    String gatewayId = "testGateway";
	
	GatewayDescription gd = GatewayDescription.builder()
			.gatewayId(gatewayId)
			.settings(new Object())
			.build();
	
	GatewayDescription gd2 = GatewayDescription.builder()
			.gatewayId(gatewayId + "2")
			.settings(new Object())
			.build();
	
	@BeforeEach
	void prepare() {
		repo = new GatewayRepoAdapter(delegate);
		MessageSourceHolder.setMessageSource(mock(MessageSource.class));
	}
    
	@Test
	void testSave() {
		assertDoesNotThrow(() -> {
			repo.save(gd2);
		});
	}

	@Test
	void testDeleteById() {
		repo.save(gd);
		repo.deleteById(gatewayId);
		assertThrows(NoSuchElementException.class, () -> {
			repo.findById(gatewayId);
		});
	}

	@Test
	void testFindAll() {
		repo.save(gd);
		repo.save(gd2);
		assertThat(repo.findAll()).hasSize(2);
	}

	@Test
	void testFindById() {
		repo.save(gd);
		assertThat(repo.findById(gatewayId)).isNotNull();
	}

}
