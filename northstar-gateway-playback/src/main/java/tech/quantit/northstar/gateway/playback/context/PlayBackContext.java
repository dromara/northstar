package tech.quantit.northstar.gateway.playback.context;

import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.common.model.SimAccountDescription;
import xyz.redtorch.pb.CoreField;

import java.util.List;

public class PlayBackContext {

    /**
     * 回放设置
     */
    private PlaybackDescription playbackDescription;

    // TICK
    private List<CoreField.TickField> tickList;

    public List<CoreField.TickField> getTickList() {

        return tickList;
    }
}
