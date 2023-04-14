package org.dromara.northstar.data.ds;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ztnozdormu.common.enums.FrequencyType;
import io.github.ztnozdormu.common.utils.ExResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import tech.quantit.northstar.common.IDataServiceManager;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.utils.MarketDateTimeUtil;
import tech.quantit.northstar.gateway.api.IContractManager;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 币圈历史数据服务接口管理器
 *
 * @author zt
 */

@Slf4j
public class W3DataServiceManager implements IDataServiceManager {

    private String userToken;

    private String dummyToken;
    private String w3BaseUrl;

    private DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeFormatter dtfmt2 = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    private MarketDateTimeUtil dtUtil;

    private RestTemplate restTemplate;

    private IContractManager contractMgr;

    private EnumMap<ExchangeEnum, ChannelType> exchangeChannelType = new EnumMap<>(ExchangeEnum.class);

    public W3DataServiceManager(String w3BaseUrl, String secret, RestTemplate restTemplate, MarketDateTimeUtil dtUtil, IContractManager contractMgr) {
        this.w3BaseUrl = w3BaseUrl;
        this.userToken = secret;
        this.dtUtil = dtUtil;
        this.restTemplate = restTemplate;
        this.contractMgr = contractMgr;

        exchangeChannelType.put(ExchangeEnum.OKX, ChannelType.OKX);

        log.info("采用外部数据源加载历史数据");
    }


    /**
     * 获取1分钟K线数据
     *
     * @param unifiedSymbol
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<BarField> getMinutelyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        return commonGetData(FrequencyType.MIN_1.value(), unifiedSymbol, startDate, endDate);
    }

    /**
     * 获取15分钟K线数据
     *
     * @param unifiedSymbol
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<BarField> getQuarterlyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        return commonGetData("quarter", unifiedSymbol, startDate, endDate);
    }

    /**
     * 获取1小时K线数据
     *
     * @param unifiedSymbol
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<BarField> getHourlyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        return commonGetData("hour", unifiedSymbol, startDate, endDate);
    }

    /**
     * 获取日K线数据
     *
     * @param unifiedSymbol
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<BarField> getDailyData(String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        return commonGetData("day", unifiedSymbol, startDate, endDate);
    }

    /**
     * 暂不需要实现 TODO
     *
     * @param exchange
     * @param startDate
     * @param endDate
     * @return
     */
    @Override
    public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) {

        return null;
    }

    @Override
    public List<ContractField> getAllContracts(ExchangeEnum exchange) {
        ResponseEntity<W3DataSetVO> result = execute(URI.create(String.format("%s/dataex/tokenInfo/contracts?exchange=%s", w3BaseUrl, exchange)), W3DataSetVO.class);
        W3DataSetVO w3DataSetVO = result.getBody();
        if (Objects.isNull(w3DataSetVO.getData())) {
            return Collections.emptyList();
        }
        LinkedList<ContractField> resultList = new LinkedList<>();
        ObjectMapper mapper = new ObjectMapper();
        List<TokenDataSetVO> dataSetVOS = mapper.convertValue(w3DataSetVO.getData(), new TypeReference<List<TokenDataSetVO>>() {
        });

        dataSetVOS.forEach(dataSetVO -> {

            String unifiedSymbol = dataSetVO.getNsCode();
            String symbol = dataSetVO.getSymbol();
            String name = dataSetVO.getName();
            String unitDesc = dataSetVO.getQuoteUnitDesc();

            try {
                ContractField contract = ContractField.newBuilder().setUnifiedSymbol(unifiedSymbol).setSymbol(symbol).setExchange(exchange).setCurrency(CurrencyEnum.USDT).setFullName(name).setName(name).setContractId(unifiedSymbol).setGatewayId(channelName(exchange)).setThirdPartyId(symbol + "@" + channelName(exchange)).setLastTradeDateOrContractMonth(dataSetVO.getDelistDate()).setLongMarginRatio(0.1).setShortMarginRatio(0.1).setProductClass(ProductClassEnum.SWAP).setMultiplier(Double.parseDouble(dataSetVO.getMultiplier())).setPriceTick(0.1).build();
                resultList.add(contract);
            } catch (Exception e) {
                log.warn("无效合约数据：{}", JSON.toJSONString(dataSetVO));
            }
        });
        return resultList;
    }

    private String channelName(ExchangeEnum exchange) {
        return exchangeChannelType.get(exchange).name();
    }

    /**
     * 获取币圈市场信息  暂不需要实现 TODO
     */
    @Override
    public JSONObject getCtpMetaSettings(String brokerId) {
        URI uri = URI.create(String.format("%s/ctp/settings?brokerId=%s", w3BaseUrl, brokerId));
        return execute(uri, JSONObject.class).getBody();
    }

    @Override
    public List<ExchangeEnum> getUserAvailableExchanges() {
        return null;
    }


    private List<BarField> commonGetData(String type, String unifiedSymbol, LocalDate startDate, LocalDate endDate) {
        // 币圈市场数据
        URI uri = URI.create(String.format("%s/dataex/data/hostoryKlines?frequencyType=%s&&unifiedSymbol=%s&startDate=%s&endDate=%s", w3BaseUrl, type, unifiedSymbol, startDate, endDate));
        return convertDataSet(execute(uri, ExResult.class).getBody());
    }

    private <T> ResponseEntity<T> execute(URI uri, Class<T> clz) {
        HttpHeaders headers = new HttpHeaders();
        String token;
        if (StringUtils.isNotBlank(userToken)) {
            token = userToken;
        } else {
            token = dummyToken;
            log.warn("【注意】 当前数据服务调用受限，仅能查询部分基础信息。如需要查询历史行情数据，请向社群咨询。");
        }
        headers.add("Authorization", String.format("Bearer %s", token));
        HttpEntity<?> reqEntity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(uri, HttpMethod.GET, reqEntity, clz);
        } catch (HttpServerErrorException e) {
            JSONObject entity = JSON.parseObject(e.getResponseBodyAsString());
            throw new IllegalStateException(entity.getString("message"));
        } catch (Exception e) {
            throw new IllegalStateException("数据服务连接异常", e);
        }
    }

    private List<BarField> convertDataSet(ExResult<List<LinkedHashMap>> result) {
        if (result.getCode() != 200) {
            log.warn("数据服务查询失败!");
            return Collections.emptyList();
        }
        if (Objects.isNull(result.data)) {
            log.warn("数据服务查询不到相关数据");
            return Collections.emptyList();
        }

        LinkedList<BarField> resultList = new LinkedList<>();
        List<JSONObject> array = JSON.parseArray(JSON.toJSONString(result.getData()), JSONObject.class);

        for (JSONObject jsonObject : array) {
            try {
                String unifiedSymbol = jsonObject.getString("unifiedSymbol");
                ContractField contract = contractMgr.getContract(jsonObject.getString("gatewayId"), unifiedSymbol).contractField();
                resultList.addFirst(BarField.newBuilder().setUnifiedSymbol(unifiedSymbol).setTradingDay(jsonObject.getString("tradingDay")).setActionDay(jsonObject.getString("actionDay")).setActionTime(jsonObject.getString("actionTime")).setActionTimestamp(jsonObject.getLongValue("actionTimestamp")).setHighPrice(normalizeValue(jsonObject.getDoubleValue("highPrice"), contract.getPriceTick())).setClosePrice(normalizeValue(jsonObject.getDoubleValue("closePrice"), contract.getPriceTick())).setLowPrice(normalizeValue(jsonObject.getDoubleValue("lowPrice"), contract.getPriceTick())).setOpenPrice(normalizeValue(jsonObject.getDoubleValue("openPrice"), contract.getPriceTick())).setGatewayId(contract.getGatewayId()).setOpenInterestDelta(jsonObject.getDoubleValue("openInterestDelta")).setOpenInterest(jsonObject.getDoubleValue("openInterest")).setVolume(jsonObject.getLongValue("volume")).setTurnover(jsonObject.getDouble("turnover")).setPreClosePrice(jsonObject.getDoubleValue("preClosePrice")).setPreSettlePrice(jsonObject.getDoubleValue("preSettlePrice")).setPreOpenInterest(jsonObject.getDoubleValue("preOpenInterest")).build());
            } catch (Exception e) {
                log.warn("无效合约行情数据：{}", jsonObject.toJSONString());
                log.error("", e);
            }
        }

        return resultList;
    }

    private double normalizeValue(double val, double priceTick) {
        return (int) (val / priceTick) * priceTick;
    }

    private String getValue(String key, Map<String, Integer> fieldIndexMap, String[] item, String defaultVal) {
        return fieldIndexMap.containsKey(key) && Objects.nonNull(item[fieldIndexMap.get(key)]) ? item[fieldIndexMap.get(key)] : defaultVal;
    }

    @Data
    protected static class W3DataSetVO<T> {

        public W3DataSetVO() {

        }

        /**
         * 返回状态
         */
        private int code;
        /**
         * 提示信息
         */
        private String msg;
        /**
         * 分页查询时总条数
         */
        private Long total;
        /**
         * 返回数据
         */
        private T data;
    }

    @Data
    protected static class TokenDataSetVO {

        public TokenDataSetVO() {

        }

        /**
         * BTC@OKX@SWAP  jd1907@DCE@FUTURES
         */
        private String nsCode;
        /**
         * BTC   JD1907
         */
        private String symbol;
        /**
         * OKX  DCE
         */
        private String exchange;
        /**
         * BTC 鸡蛋1907
         */
        private String name;
        /**
         * BTC  JD
         */
        private String futCode;
        /**
         * 倍数
         */
        private String multiplier;
        /**
         * 吨 交易单位
         */
        private String tradeUnit;
        /**
         * 5.0
         */
        private String perUnit;
        /**
         * 人民币元/500千克
         */
        private String quoteUnit;
        /**
         * 1人民币元/500千克
         */
        private String quoteUnitDesc;
        /**
         * 实物交割
         */
        private String dModeDesc;
        /**
         * 20180727
         */
        private String listDate;
        /**
         * 20190726
         */
        private String delistDate;
        /**
         * 201907
         */
        private String dMonth;
        /**
         * 20190731
         */
        private String lastDdate;

    }

}
