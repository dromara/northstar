package tech.xuanwu.northstar.persistance;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import tech.xuanwu.northstar.persistance.po.ContractProfit;

@Repository
public interface ContractProfitRepo extends MongoRepository<ContractProfit, String>{

	ContractProfit findByGatewayIdAndUnifiedSymbol(String gatewayId, String unifiedSymbol);
}
