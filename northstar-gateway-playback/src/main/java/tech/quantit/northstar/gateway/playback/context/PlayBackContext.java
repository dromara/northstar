package tech.quantit.northstar.gateway.playback.context;

import tech.quantit.northstar.common.model.PlaybackDescription;
import xyz.redtorch.pb.CoreField;

import java.util.List;

/**
 * 回放数据上下文
 *
 * @author changsong
 */
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
