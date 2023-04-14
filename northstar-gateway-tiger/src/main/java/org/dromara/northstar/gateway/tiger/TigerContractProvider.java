package org.dromara.northstar.gateway.tiger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.model.ContractsModel;
import com.tigerbrokers.stock.openapi.client.https.domain.quote.item.SymbolNameItem;
import com.tigerbrokers.stock.openapi.client.https.request.contract.ContractsRequest;
import com.tigerbrokers.stock.openapi.client.https.request.quote.QuoteSymbolNameRequest;
import com.tigerbrokers.stock.openapi.client.https.response.contract.ContractsResponse;
import com.tigerbrokers.stock.openapi.client.https.response.quote.QuoteSymbolNameResponse;
import com.tigerbrokers.stock.openapi.client.struct.enums.Language;
import com.tigerbrokers.stock.openapi.client.struct.enums.Market;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.gateway.api.IMarketCenter;

@Slf4j
public class TigerContractProvider {

	private TigerGatewaySettings settings;
	
	private IMarketCenter mktCenter;
	
	public TigerContractProvider(TigerGatewaySettings settings, IMarketCenter mktCenter) {
		this.mktCenter = mktCenter;
		this.settings = settings;
	}
	
	/**
	 * 加载可用合约
	 */
	public void loadContractOptions() {
		ClientConfig clientConfig = ClientConfig.DEFAULT_CONFIG;
		clientConfig.tigerId = settings.getTigerId();
        clientConfig.defaultAccount = settings.getAccountId();
        clientConfig.privateKey = settings.getPrivateKey();
        clientConfig.license = settings.getLicense();
        clientConfig.secretKey = settings.getSecretKey();
        clientConfig.language = Language.zh_CN;
		TigerHttpClient client = TigerHttpClient.getInstance().clientConfig(clientConfig);
		doLoadContracts(Market.CN, client);
		doLoadContracts(Market.HK, client);
		doLoadContracts(Market.US, client);
	}
	
	private void doLoadContracts(Market market, TigerHttpClient client) {
		QuoteSymbolNameResponse response = client.execute(QuoteSymbolNameRequest.newRequest(market));
		if(!response.isSuccess()) {
			log.warn("TIGER 加载 [{}] 市场合约失败", market);
			return;
		}
		Map<String, String> symbolNameMap = response.getSymbolNameItems().stream().collect(Collectors.toMap(item -> item.getSymbol(), item -> item.getName()));
		List<String> symbols = response.getSymbolNameItems().stream().map(SymbolNameItem::getSymbol).toList();
		ContractsRequest contractsRequest = ContractsRequest.newRequest(new ContractsModel(symbols));
    	ContractsResponse contractsResponse = client.execute(contractsRequest);
    	contractsResponse.getItems().stream().map(item -> {
    		item.setName(symbolNameMap.get(item.getSymbol()));
    		return new TigerContract(item);
    	}).forEach(c -> mktCenter.addInstrument(c));
    	log.info("加载TIGER网关 [{}] 的合约{}个", market, contractsResponse.getItems().size());
	}
	
}
