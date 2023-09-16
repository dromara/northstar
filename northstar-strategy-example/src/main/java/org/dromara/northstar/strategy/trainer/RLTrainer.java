package org.dromara.northstar.strategy.trainer;

import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModule;


public class RLTrainer extends AbstractSerialTrainer {

    protected RLTrainer(ObjectManager<Gateway> gatewayMgr, ObjectManager<IModule> moduleMgr,
            IContractManager contractMgr, IGatewayService gatewayService, IModuleService moduleService) {
        super(gatewayMgr, moduleMgr, contractMgr, gatewayService, moduleService);
        //TODO Auto-generated constructor stub
        
        
    }

    @Override
    public DynamicParams strategyParams(ContractSimpleInfo csi) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'strategyParams'");
    }

    @Override
    public ComponentMetaInfo strategy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'strategy'");
    }

    @Override
    public int[] testPeriods() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'testPeriods'");
    }

}