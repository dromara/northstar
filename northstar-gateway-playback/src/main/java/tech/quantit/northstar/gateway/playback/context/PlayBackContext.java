package tech.quantit.northstar.gateway.playback.context;

import tech.quantit.northstar.common.model.SimAccountDescription;
import xyz.redtorch.pb.CoreField;

import java.util.List;

public class PlayBackContext {

    /**
     * 查找账户信息
     * @param accountId
     * @return
     */
    SimAccountDescription findById(String accountId);

    // TICK
    private List<CoreField.TickField> tickList;

    public List<CoreField.TickField> getTickList() {

        return tickList;
    }
}
