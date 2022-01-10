package tech.quantit.northstar.main.engine.event.handler;

import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.PlaybackEventBus;
import xyz.redtorch.pb.CoreField.AccountField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.TradeField;

public class PlaybackDispatcher implements NorthstarEventDispatcher{
	
	private PlaybackEventBus eventBus;
	
	public PlaybackDispatcher(PlaybackEventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		boolean isPlaybackAccount = event.getData() instanceof AccountField account && account.getGatewayId().contains(Constants.PLAYBACK_GATEWAY);
		boolean isPlaybackOrder = event.getData() instanceof OrderField order && order.getGatewayId().contains(Constants.PLAYBACK_GATEWAY);
		boolean isPlaybackTrade = event.getData() instanceof TradeField trade && trade.getGatewayId().contains(Constants.PLAYBACK_GATEWAY);
		boolean isPlaybackPosition = event.getData() instanceof PositionField position && position.getGatewayId().contains(Constants.PLAYBACK_GATEWAY);
		if(isPlaybackAccount || isPlaybackOrder || isPlaybackTrade || isPlaybackPosition) {
			eventBus.post(event);
		}
	}

}
