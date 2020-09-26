package tech.xuanwu.northstar.persistance.po;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import xyz.redtorch.pb.CoreField.TickField;

public class TickPOTest {

	@Test
	public void testConvertFrom() {
		TickField tick = TickField.newBuilder()
				.setActionDay("20100306")
				.addAllAskPrice(List.of(2D, 3D))
				.build();
		Tick tickPO = Tick.convertFrom(tick);
		assertThat(tickPO.getActionDay()).isEqualTo("20100306");
		assertThat(tickPO.getAskPriceList()).isNotNull().hasAtLeastOneElementOfType(Double.class);
	}
	
	
	@Test
	public void testConvertTo() {
		TickField tick = TickField.newBuilder()
				.setActionDay("20100306")
				.addAllAskPrice(List.of(2D, 3D))
				.build();
		Tick tickPO = Tick.convertFrom(tick);
		TickField t = tickPO.convertTo();
		assertThat(t.getActionDay()).isEqualTo(tick.getActionDay());
		assertThat(t.getAskPriceList()).hasAtLeastOneElementOfType(Double.class);
	}
}
