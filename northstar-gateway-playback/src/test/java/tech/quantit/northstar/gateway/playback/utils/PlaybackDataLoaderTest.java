package tech.quantit.northstar.gateway.playback.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.data.IMarketDataRepository;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 验证数据加载是否解析为通用的时间段，以便于缓存命中；以及过期数据的过滤
 * @author KevinHuangwl
 *
 */
class PlaybackDataLoaderTest {
	
	PlaybackDataLoader loader;
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	BarField b1 = factory.makeBarField("rb2210", 5000, 20, LocalDateTime.now().minusMinutes(1));
	BarField b2 = factory.makeBarField("rb2210", 5000, 20, LocalDateTime.now().minusSeconds(1));
	BarField b3 = factory.makeBarField("rb2210", 5000, 20, LocalDateTime.now().plusSeconds(1));
	
	ContractField c1 = factory.makeContract("rb2210");
	
	@BeforeEach
	void prepare() {
		IMarketDataRepository mdRepo = mock(IMarketDataRepository.class);
		when(mdRepo.loadBars(anyString(), anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(b1,b2,b3));
		loader = new PlaybackDataLoader(mdRepo);
	}

	@Test
	void test() {
		List<BarField> resultList = loader.loadData(System.currentTimeMillis(), c1);
		assertThat(resultList).hasSize(1);
	}

}
