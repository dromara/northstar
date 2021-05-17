package tech.xuanwu.northstar.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.utils.ContractNameResolver;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.IndexContract;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 处理指数合约相关操作
 * @author KevinHuangwl
 *
 */
public class IndexContractHandler extends AbstractEventHandler implements InternalEventHandler{
	
	private ContractManager contractMgr;
	
	private FastEventEngine feEngine;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private SocketIOMessageEngine msgEngine;
	
	/**
	 * gateway -> symbolGroup -> idxContract
	 */
	private Table<String, String, IndexContract> idxContractTbl = HashBasedTable.create();
	
	public IndexContractHandler(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr,
			FastEventEngine fastEventEngine, SocketIOMessageEngine msgEngine) {
		this.contractMgr = contractMgr;
		this.feEngine = fastEventEngine;
		this.msgEngine = msgEngine;
		this.gatewayConnMgr = gatewayConnMgr;
	}

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.TICK == eventType || NorthstarEventType.CONTRACT_LOADED == eventType;
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		if(NorthstarEventType.TICK == e.getEvent()) {
			TickField tick = (TickField) e.getData();
			handleIndexContractUpdate(tick);
		} else if(NorthstarEventType.CONTRACT_LOADED == e.getEvent()) {
			String accGatewayId = (String) e.getData();
			GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(accGatewayId);
			String mktGatewayId = conn.getGwDescription().getRelativeGatewayId();
			generateIndexContract(mktGatewayId);
		}
	}
	
	private void generateIndexContract(String gatewayId) {
		NorthstarEvent event = new NorthstarEvent(NorthstarEventType.CONTRACT, null);
		for(String symbolGroup : contractMgr.getContractGroup(gatewayId)) {
			List<ContractField> groupContracts = contractMgr.getContractsByGroup(gatewayId, symbolGroup);
			//构造指数合约
			IndexContract idxContract = new IndexContract(groupContracts, (tick)->{
				feEngine.emitEvent(NorthstarEventType.IDX_TICK, tick);
			});
			idxContractTbl.put(gatewayId, symbolGroup, idxContract);
			ContractField contract = idxContract.getContract();
			if(contractMgr.addContract(contract)) {
				event.setData(contract);
				try {
					msgEngine.emitEvent(event, ContractField.class);
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}
	
	private void handleIndexContractUpdate(TickField tick) {
		String gatewayId = tick.getGatewayId();
		String unifiedSymbol = tick.getUnifiedSymbol();
		String symbolGroup = ContractNameResolver.unifiedSymbolToSymbolGroup(unifiedSymbol);
		if(idxContractTbl.contains(gatewayId, symbolGroup)) {
			idxContractTbl.get(gatewayId, symbolGroup).updateByTick(tick);
		}
	}

}
