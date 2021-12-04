package tech.quantit.northstar.main.handler.data;

import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.GenericEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import xyz.redtorch.gateway.ctp.index.IndexEngine;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 处理指数合约相关操作
 * @author KevinHuangwl
 *
 */
public class IndexContractHandler extends AbstractEventHandler implements GenericEventHandler{
	
	private IndexEngine idxEngine;
	
	public IndexContractHandler(IndexEngine idxEngine) {
		this.idxEngine = idxEngine;
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		TickField tick = (TickField) e.getData();
		idxEngine.updateTick(tick);
	}
	
}
