package org.dromara.northstar.gateway.binance;

import org.dromara.northstar.gateway.IMarketCenter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.impl.UMFuturesClientImpl;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class BinanceContractProvider {
	
	private IMarketCenter mktCenter;
	
	private BinanceGatewaySettings settings;

	public BinanceContractProvider(BinanceGatewaySettings settings, IMarketCenter mktCenter) {
		this.mktCenter = mktCenter;
		this.settings = settings;
	}
	
	public void loadContractOptions() {
		UMFuturesClientImpl client = new UMFuturesClientImpl(settings.getApiKey(), settings.getSecretKey());
		try {
            String result = client.market().exchangeInfo();
            JSONObject json = JSON.parseObject(result);
            JSONArray symbols = json.getJSONArray("symbols");
            for(int i=0; i<symbols.size(); i++) {
            	JSONObject obj = symbols.getJSONObject(i);
            	mktCenter.addInstrument(new BinanceContract(obj));
            }
        } catch (BinanceConnectorException e) {
            log.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (BinanceClientException e) {
            log.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
	}
}
