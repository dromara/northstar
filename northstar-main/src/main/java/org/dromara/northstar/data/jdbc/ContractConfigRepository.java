package org.dromara.northstar.data.jdbc;

import java.util.List;

import org.dromara.northstar.data.jdbc.entity.ContractConfigDO;
import org.springframework.data.repository.CrudRepository;

public interface ContractConfigRepository extends CrudRepository<ContractConfigDO, String> {
    
    /**
     * 根据合约ID查询
     */
    ContractConfigDO findByContractId(String contractId);
    
    /**
     * 根据交易所查询
     */
    List<ContractConfigDO> findByExchange(String exchange);
    
    /**
     * 根据合约ID删除
     */
    void deleteByContractId(String contractId);
    
    /**
     * 查询所有
     */
    @Override
    List<ContractConfigDO> findAll();
}
