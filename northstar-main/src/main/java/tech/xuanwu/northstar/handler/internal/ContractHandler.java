package tech.xuanwu.northstar.handler.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.domain.ContractManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.engine.index.IndexEngine;
import tech.xuanwu.northstar.gateway.api.MarketGateway;
import tech.xuanwu.northstar.handler.AbstractEventHandler;
import tech.xuanwu.northstar.handler.GenericEventHandler;
import tech.xuanwu.northstar.model.GatewayAndConnectionManager;
import tech.xuanwu.northstar.persistence.MarketDataRepository;
import tech.xuanwu.northstar.persistence.po.ContractPO;
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
		if(NorthstarEventType.CONTRACT_LOADED == e.getEvent()) {
			idxEngine.start();
			String accountId = (String) e.getData();
			GatewayConnection conn = gatewayConnMgr.getGatewayConnectionById(accountId);
			Map<String, ContractField> gatewayContractMap = contractMgr.getContractMapByGateway(conn.getGwDescription().getRelativeGatewayId());
			List<ContractPO> contractList = new ArrayList<>(gatewayContractMap.size());
			long curTime = System.currentTimeMillis();
			for(Entry<String, ContractField> entry : gatewayContractMap.entrySet()) {
				ContractPO contract = new ContractPO();
				contract.setFullName(entry.getValue().getFullName());
				contract.setGatewayId(entry.getValue().getGatewayId());
				contract.setName(entry.getValue().getName());
				contract.setUnifiedSymbol(entry.getValue().getUnifiedSymbol());
				contract.setRecordTimestamp(curTime);
				contractList.add(contract);
			}
			CompletableFuture.runAsync(() -> mdRepo.batchSaveContracts(contractList));
			return;
		}
		ContractField contract = (ContractField) e.getData();
		
		if(NorthstarEventType.IDX_CONTRACT == e.getEvent()) {
			contractMgr.addContract(contract);
			return;
		}
		
		// 把合约的账户gatewayId替换为行情gatewayId
		String originalGatewayId = contract.getGatewayId();
		String relativeGatewayId = gatewayConnMgr.getGatewayConnectionById(originalGatewayId).getGwDescription().getRelativeGatewayId();
		
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
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return NorthstarEventType.CONTRACT == eventType 
				|| NorthstarEventType.IDX_CONTRACT == eventType
				|| NorthstarEventType.CONTRACT_LOADED == eventType;
	}

}
