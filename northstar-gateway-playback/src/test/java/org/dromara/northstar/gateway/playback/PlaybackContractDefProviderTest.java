package org.dromara.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaybackContractDefProviderTest {

	@Test
	void test() {
		PlaybackContractDefProvider pvd = new PlaybackContractDefProvider();
		assertThat(pvd.get()).isNotEmpty();
	}

}
