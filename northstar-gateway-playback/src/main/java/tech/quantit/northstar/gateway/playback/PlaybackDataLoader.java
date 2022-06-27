package tech.quantit.northstar.gateway.playback;

import java.util.List;

import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;

public class PlaybackDataLoader {

	private IMarketDataRepository mdRepo;
	
	public List<BarField> loadData(long fromStartTimestamp){
		return null;
	}
}
