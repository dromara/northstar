package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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
	}
    
	@Test
	void testInsert() {
		assertDoesNotThrow(() -> {
			repo.insert(gd);
		});
	}
	
	@Test
	void testInsertFail() {
		repo.insert(gd);
		assertThrows(IllegalStateException.class, () -> {
			repo.insert(gd);
		});
	}

	@Test
	void testSave() {
		assertDoesNotThrow(() -> {
			repo.save(gd2);
		});
	}

	@Test
	void testDeleteById() {
		repo.insert(gd);
		repo.deleteById(gatewayId);
		assertThrows(NoSuchElementException.class, () -> {
			repo.findById(gatewayId);
		});
	}

	@Test
	void testFindAll() {
		repo.insert(gd);
		repo.insert(gd2);
		assertThat(repo.findAll()).hasSize(2);
	}

	@Test
	void testFindById() {
		repo.insert(gd);
		assertThat(repo.findById(gatewayId)).isNotNull();
	}

}
