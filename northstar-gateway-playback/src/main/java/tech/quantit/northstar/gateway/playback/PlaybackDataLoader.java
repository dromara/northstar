package tech.quantit.northstar.gateway.playback;

import java.util.List;
import java.util.Map;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackDataLoader {

	private IMarketDataRepository mdRepo;
	
	private IContractManager contractMgr;
	
	public Map<ContractField, List<BarField>> loadData(long fromStartTimestamp, String contractGroup){
		return null;
	}
}
