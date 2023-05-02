package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.dromara.northstar.common.model.PlaybackRuntimeDescription;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PlaybackRuntimeRepoAdapterTest {
	
	@Autowired
	PlaybackRuntimeRepository delegate;
	
	IPlaybackRuntimeRepository repo;
	
	@BeforeEach
	void prepare() {
		repo = new PlaybackRuntimeRepoAdapter(delegate);
	}

	@Test
	void testSave() {
		PlaybackRuntimeDescription pbrt = PlaybackRuntimeDescription.builder()
				.gatewayId("testGateway")
				.playbackTimeState(LocalDateTime.now())
				.build();
		assertDoesNotThrow(() -> {
			repo.save(pbrt);
		});
	}

	@Test
	void testFindById() {
		testSave();
		assertThat(repo.findById("testGateway")).isNotNull();
	}

	@Test
	void testDeleteById() {
		testSave();
		assertDoesNotThrow(() -> {
			repo.deleteById("testGateway");
		});
		assertThrows(NoSuchElementException.class, () -> {
			repo.findById("testGateway");
		});
	}

}
