package tech.xuanwu.northstar.main.handler.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import tech.xuanwu.northstar.common.event.AbstractEventHandler;
import tech.xuanwu.northstar.common.event.GenericEventHandler;
import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.MarketGateway;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.po.ContractPO;
import tech.xuanwu.northstar.main.utils.ProtoBeanUtils;
import xyz.redtorch.gateway.ctp.index.IndexEngine;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 处理普通合约相关操作
 * @author KevinHuangwl
 *
 */
public class ContractHandler extends AbstractEventHandler implements GenericEventHandler{

	private ContractManager contractMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private IndexEngine idxEngine;
	
	private MarketDataRepository mdRepo;
	
	public ContractHandler(ContractManager contractMgr, GatewayAndConnectionManager gatewayConnMgr,
			IndexEngine idxEngine, MarketDataRepository mdRepo) {
		this.contractMgr = contractMgr;
		this.gatewayConnMgr = gatewayConnMgr;
		this.idxEngine = idxEngine;
		this.mdRepo = mdRepo;
	}
	
	@Override
	public void doHandle(NorthstarEvent e) {
		switch(e.getEvent()) {
		case CONTRACT:
			handleContractEvent((ContractField) e.getData());
			break;
		case IDX_CONTRACT:
			handleIdxContractEvent((ContractField) e.getData());
			break;
		case CONTRACT_LOADED:
			handleContractLoadedEvent((String) e.getData());
			break;
		default:
			throw new IllegalArgumentException("未定义该事件的处理:" + e.getEvent());
		}
		
	}
	
	private void handleContractEvent(ContractField contract) {
		// 把合约的账户gatewayId替换为行情gatewayId
		String originalGatewayId = contract.getGatewayId();
		String relativeGatewayId = gatewayConnMgr.getGatewayConnectionById(originalGatewayId).getGwDescription().getBindedMktGatewayId();
		
		ContractField contractNew = contract.toBuilder()
				.setGatewayId(relativeGatewayId)
				.build();
		if(contractMgr.addContract(contractNew)) {			
			idxEngine.registerContract(contractNew);
			MarketGateway gateway = (MarketGateway) gatewayConnMgr.getGatewayById(relativeGatewayId);
			if(gateway.isConnected()) {				
				gateway.subscribe(contractNew);
			}
		}
	}
	
	private void handleIdxContractEvent(ContractField contract) {
		long curTime = System.currentTimeMillis();
		ContractPO c = ProtoBeanUtils.toPojoBean(ContractPO.class, contract);
		c.setRecordTimestamp(curTime);
		mdRepo.saveContract(c);
		contractMgr.addContract(contract);
	}
	
	private void handleContractLoadedEvent(String accountId) {
		idxEngine.start();
		long curTime = System.currentTimeMillis();
		GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(accountId);
		Map<String, ContractField> gatewayContractMap = contractMgr.getContractMapByGateway(conn.getGwDescription().getBindedMktGatewayId());
		List<ContractPO> contractList = new ArrayList<>(gatewayContractMap.size());
		for(Entry<String, ContractField> entry : gatewayContractMap.entrySet()) {
			ContractPO contract = ProtoBeanUtils.toPojoBean(ContractPO.class, entry.getValue());
			contract.setRecordTimestamp(curTime);
			contract.setGatewayId(conn.getGwDescription().getBindedMktGatewayId());
			contract.setContractId(contract.getUnifiedSymbol() + "@" + contract.getGatewayId());
			contractList.add(contract);
		}
		CompletableFuture.runAsync(() -> mdRepo.batchSaveContracts(contractList));
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.CONTRACT == eventType 
				|| NorthstarEventType.IDX_CONTRACT == eventType
				|| NorthstarEventType.CONTRACT_LOADED == eventType;
	}

}
