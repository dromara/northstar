package tech.quantit.northstar.domain.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

class DailyBarMergerTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");
	
	ContractField contract = factory.makeContract("rb2205");

	@Test
	void test() {
		AtomicInteger counter = new AtomicInteger();
		List<BarField> samples = new ArrayList<>();
		List<BarField> results = new ArrayList<>();
		Consumer<BarField> callback = bar -> {
			counter.incrementAndGet();
			results.add(bar);
		};
		DailyBarMerger merger = new DailyBarMerger(contract, callback);
		String[] dates = {"20220906", "20220906", "20220907", "20220907", "20220908"};
		
		for(String date : dates) {
			BarField bar = factory.makeBarField("rb2205", 5000, 10, LocalDateTime.of(LocalDate.parse(date, DateTimeConstant.D_FORMAT_INT_FORMATTER), LocalTime.of(0, 0)));
			merger.updateBar(bar);
			samples.add(bar);
		}
		assertThat(counter.get()).isEqualTo(2);
		
		assertThat(results.get(0).getTradingDay()).isEqualTo("20220906");
		assertThat(results.get(1).getTradingDay()).isEqualTo("20220907");
		
		assertThat(results.get(0).getOpenPrice()).isCloseTo(samples.get(0).getOpenPrice(), offset(1e-6));
		assertThat(results.get(0).getHighPrice()).isCloseTo(Math.max(samples.get(0).getHighPrice(), samples.get(1).getHighPrice()), offset(1e-6));
		assertThat(results.get(0).getLowPrice()).isCloseTo(Math.min(samples.get(0).getLowPrice(), samples.get(1).getLowPrice()), offset(1e-6));
		assertThat(results.get(0).getClosePrice()).isCloseTo(samples.get(1).getClosePrice(), offset(1e-6));
		
	}

}
