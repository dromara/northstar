package org.dromara.northstar.gateway.okx;

import cn.hutool.core.text.StrPool;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.ztnozdormu.common.exceptions.ExchangeClientException;
import io.github.ztnozdormu.common.exceptions.ExchangeConnectorException;
import io.github.ztnozdormu.okx.impl.OKXSpotClientImpl;
import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import xyz.redtorch.pb.CoreEnum;
import xyz.redtorch.pb.CoreField;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Objects;


@Slf4j
public class OkxContractProvider {

    private IMarketCenter mktCenter;

    private OkxGatewaySettings settings;

    public OkxContractProvider(OkxGatewaySettings settings, IMarketCenter mktCenter) {
        this.mktCenter = mktCenter;
        this.settings = settings;
    }

    // 加载OKX网关合约
    public void loadContractOptions() {

        OKXSpotClientImpl client = OKXSpotClientImpl.builder()
                .baseUrl("https://www.okx.com")
                .apiKey(settings.getApiKey())
                .secretKey(settings.getSecretKey())
                .build();

        doLoadContracts(client);

    }

    private void doLoadContracts(OKXSpotClientImpl client) {
        try {
            LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
            // instType -》 SPOT：币币 MARGIN：币币杠杆 SWAP：永续合约 FUTURES：交割合约 OPTION：期权
            parameters.put("instType", "SWAP");
            String result = client.createPubMarket().exchangeInfo(parameters);
            JSONObject json = JSON.parseObject(result);
            JSONArray symbols = json.getJSONArray("data");
            for (int i = 0; i < symbols.size(); i++) {
                JSONObject obj = symbols.getJSONObject(i);
                CoreField.ContractField contractField = json2ContractField(obj);
                if(!Objects.isNull(contractField)){
                    mktCenter.addInstrument(new OkxContract(json2ContractField(obj)));
                }
            }
            log.info("加载OKX网关 [{}] 的合约{}个", "OKX交易市场", symbols.size());
        } catch (ExchangeConnectorException e) {
            log.error("fullErrMessage: {}", e.getMessage(), e);
        } catch (ExchangeClientException e) {
            log.error("fullErrMessage: {} \nerrMessage: {} \nerrCode: {} \nHTTPStatusCode: {}",
                    e.getMessage(), e.getErrMsg(), e.getErrorCode(), e.getHttpStatusCode(), e);
        }
    }

    public CoreField.ContractField json2ContractField(JSONObject obj){
        CoreEnum.ExchangeEnum exchange = CoreEnum.ExchangeEnum.OKX;
        String instId = obj.getString("instId");
        // 排除非稳定币结算币种的合约交易对
        CoreEnum.CurrencyEnum currencyEnum = null;
        try{
            currencyEnum = CoreEnum.CurrencyEnum.valueOf(obj.getString("ctValCcy").toUpperCase());
        }catch (Exception e){

        }
        if (currencyEnum == null) {
            return null;
        }
        // 暂只获取USDT的交易对
        if(!currencyEnum.equals(CoreEnum.CurrencyEnum.USDT)){
            return null;
        }
        String symbol = obj.getString("instFamily").toUpperCase();
        Long listTime = obj.getLongValue("listTime");
        LocalDateTime listDate = LocalDateTime.ofEpochSecond(listTime / 1000, 0, ZoneOffset.ofHours(8));
        CoreField.ContractField contract = CoreField.ContractField.newBuilder()
                .setUnifiedSymbol(instId.replaceAll(StrPool.DASHED,StrPool.AT))
                .setSymbol(symbol)
                .setExchange(CoreEnum.ExchangeEnum.OKX)
                .setCurrency(currencyEnum)
                .setFullName(instId)
                .setName(obj.getString("settleCcy").toUpperCase())
                .setContractId(instId + "@" + exchange.name())
                .setGatewayId(exchange.name())
                .setThirdPartyId(symbol + "@" + exchange.name())
                .setLastTradeDateOrContractMonth(listDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .setLongMarginRatio(0.1)
                .setShortMarginRatio(0.1)
                .setProductClass(CoreEnum.ProductClassEnum.SWAP)
                .setMultiplier(obj.getDoubleValue("lever")) // 倍数
                .setPriceTick(0.1)
                .build();
        return contract;
    }
}
