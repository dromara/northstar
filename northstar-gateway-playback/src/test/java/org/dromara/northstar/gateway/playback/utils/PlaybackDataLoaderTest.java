package org.dromara.northstar.gateway.playback.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.gateway.utils.MarketDataRepoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
		MarketDataRepoFactory mdRepoFactory = mock(MarketDataRepoFactory.class);
		IMarketDataRepository mdRepo = mock(IMarketDataRepository.class);
		when(mdRepoFactory.getInstance(any(ChannelType.class))).thenReturn(mdRepo);
		when(mdRepo.loadBars(eq(c1), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(b1,b2,b3));
		
		loader = new PlaybackDataLoader("testGateway", mdRepoFactory);
	}

	@Test
	void test() {
		List<BarField> resultList = loader.loadMinuteData(LocalDateTime.now(), c1);
		assertThat(resultList).hasSize(1);
	}

}
