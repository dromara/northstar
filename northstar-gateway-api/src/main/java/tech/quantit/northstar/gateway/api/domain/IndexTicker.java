package tech.quantit.northstar.gateway.api.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import tech.quantit.northstar.common.constant.GatewayType;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 指数TICK生成器
 * @author KevinHuangwl
 *
 */
public class IndexTicker {

	private IndexContract idxContract;
	
	public IndexTicker(IndexContract idxContract) {
		this.idxContract = idxContract;
	}

	public void setOnTickCallback(Consumer<TickField> object) {
		// TODO Auto-generated method stub
		
	}

	public List<String> dependencySymbols() {
		// TODO Auto-generated method stub
		return null;
	}

	public void update(TickField tick) {
		// TODO Auto-generated method stub
		
	}
	
	
}
