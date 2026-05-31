package org.dromara.northstar.config;

import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.data.jdbc.AccountRepository;
import org.dromara.northstar.data.jdbc.AccountRepositoryAdapter;
import org.dromara.northstar.data.jdbc.DealRecordRepository;
import org.dromara.northstar.data.jdbc.GatewayDescriptionRepository;
import org.dromara.northstar.data.jdbc.GatewayRepoAdapter;
import org.dromara.northstar.data.jdbc.HoldingsRepository;
import org.dromara.northstar.data.jdbc.MarketDataRepoAdapter;
import org.dromara.northstar.data.jdbc.MarketDataRepository;
import org.dromara.northstar.data.jdbc.ModuleDescriptionRepository;
import org.dromara.northstar.data.jdbc.ModuleRepoAdapter;
import org.dromara.northstar.data.jdbc.ModuleRuntimeDescriptionRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    IGatewayRepository gatewayRepo(GatewayDescriptionRepository delelgate) {
    	return new GatewayRepoAdapter(delelgate);
    }
    
    @Bean
    IModuleRepository moduleRepo(ModuleDescriptionRepository mdRepo, ModuleRuntimeDescriptionRepository mrdRepo) {
    	return new ModuleRepoAdapter(mdRepo, mrdRepo);
    }
    
    @Bean
    IMarketDataRepository marketDataRepo(MarketDataRepository mdRepo, IContractManager contractMgr) {
    	return new MarketDataRepoAdapter(mdRepo, contractMgr);
    }
    
    @Bean
    IAccountRepository accountRepo(DealRecordRepository dealRepo, HoldingsRepository holdingsRepo, AccountRepository accountRepo,
    		MarketCenter mktCenter) {
    	return new AccountRepositoryAdapter(dealRepo, holdingsRepo, accountRepo, mktCenter);
    }
    
}
