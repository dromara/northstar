package tech.quantit.northstar.gateway.api;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public interface MarketDataBuffer {

	void save(BarField bar, List<TickField> ticks);
}
