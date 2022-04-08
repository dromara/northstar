package tech.quantit.northstar.domain.strategy;

import java.util.List;
import java.util.function.Consumer;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 负责行情数据更新
 * @author KevinHuangwl
 *
 */
public interface MarketDataStore {

	void onTick(TickField tick);
	
	void onBar(BarField bar);
	
	void initWithBars(List<BarField> bars);
	
	void setModuleEnabled(boolean enabled);
	
	void addModuleEnableStateListener(Consumer<Boolean> listener);
}
