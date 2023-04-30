package org.dromara.northstar.gateway.tiger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson2.JSONWriter.Feature;
import com.tigerbrokers.stock.openapi.client.TigerApiException;
import com.tigerbrokers.stock.openapi.client.config.ClientConfig;
import com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants;
import com.tigerbrokers.stock.openapi.client.https.client.TigerClient;
import com.tigerbrokers.stock.openapi.client.https.domain.ApiModel;
import com.tigerbrokers.stock.openapi.client.https.domain.BatchApiModel;
import com.tigerbrokers.stock.openapi.client.https.domain.contract.item.ContractItem;
import com.tigerbrokers.stock.openapi.client.https.domain.trade.model.TradeOrderModel;
import com.tigerbrokers.stock.openapi.client.https.domain.user.item.LicenseItem;
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest;
import com.tigerbrokers.stock.openapi.client.https.request.TigerRequest;
import com.tigerbrokers.stock.openapi.client.https.response.TigerHttpResponse;
import com.tigerbrokers.stock.openapi.client.https.response.TigerResponse;
import com.tigerbrokers.stock.openapi.client.https.response.contract.ContractResponse;
import com.tigerbrokers.stock.openapi.client.https.validator.ValidatorManager;
import com.tigerbrokers.stock.openapi.client.struct.enums.AccountType;
import com.tigerbrokers.stock.openapi.client.struct.enums.BizType;
import com.tigerbrokers.stock.openapi.client.struct.enums.Env;
import com.tigerbrokers.stock.openapi.client.struct.enums.License;
import com.tigerbrokers.stock.openapi.client.struct.enums.MethodName;
import com.tigerbrokers.stock.openapi.client.struct.enums.MethodType;
import com.tigerbrokers.stock.openapi.client.struct.enums.TigerApiCode;
import com.tigerbrokers.stock.openapi.client.util.AccountUtil;
import com.tigerbrokers.stock.openapi.client.util.ApiLogger;
import com.tigerbrokers.stock.openapi.client.util.FastJsonPropertyFilter;
import com.tigerbrokers.stock.openapi.client.util.HttpUtils;
import com.tigerbrokers.stock.openapi.client.util.NetworkUtil;
import com.tigerbrokers.stock.openapi.client.util.SdkVersionUtils;
import com.tigerbrokers.stock.openapi.client.util.StringUtils;
import com.tigerbrokers.stock.openapi.client.util.TigerSignature;
import com.tigerbrokers.stock.openapi.client.util.builder.AccountParamBuilder;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.ACCESS_TOKEN;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.ACCOUNT_TYPE;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.BIZ_CONTENT;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.CHARSET;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.DEVICE_ID;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.METHOD;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.SDK_VERSION;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.SIGN;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.SIGN_TYPE;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.TIGER_ID;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.TIMESTAMP;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.TRADE_TOKEN;
import static com.tigerbrokers.stock.openapi.client.constant.TigerApiConstants.VERSION;
import static com.tigerbrokers.stock.openapi.client.https.request.TigerCommonRequest.V2_0;

public class TigerHttpClient implements TigerClient {

  private String serverUrl;
  private String quoteServerUrl;
  private String paperServerUrl;
  private String tigerId;
  private String privateKey;
  private String tigerPublicKey;
  private String accessToken;
  private String tradeToken;
  private String accountType;
  private String deviceId;

  private static final String ONLINE_PUBLIC_KEY =
      "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDNF3G8SoEcCZh2rshUbayDgLLrj6rKgzNMxDL2HSnKcB0+GPOsndqSv+a4IBu9+I3fyBp5hkyMMG2+AXugd9pMpy6VxJxlNjhX1MYbNTZJUT4nudki4uh+LMOkIBHOceGNXjgB+cXqmlUnjlqha/HgboeHSnSgpM3dKSJQlIOsDwIDAQAB";

  private static final String SANDBOX_PUBLIC_KEY =
      "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCbm21i11hgAENGd3/f280PSe4g9YGkS3TEXBYMidihTvHHf+tJ0PYD0o3PruI0hl3qhEjHTAxb75T5YD3SGK4IBhHn/Rk6mhqlGgI+bBrBVYaXixmHfRo75RpUUuWACyeqQkZckgR0McxuW9xRMIa2cXZOoL1E4SL4lXKGhKoWbwIDAQAB";

  private String signType = TigerApiConstants.SIGN_TYPE_RSA;
  private String charset = TigerApiConstants.CHARSET_UTF8;

  private static final long REFRESH_URL_INTERVAL_SECONDS = 300;
  private ScheduledThreadPoolExecutor domainExecutorService;

  static {
    Security.setProperty("jdk.certpath.disabledAlgorithms", "");
  }

  private TigerHttpClient() {
  }

  private static class SingletonInner {
    private static TigerHttpClient singleton = new TigerHttpClient();
  }

  /**
   * get TigerHttpClient instance
   * @return TigerHttpClient
   */
  public static TigerHttpClient getInstance() {
    return TigerHttpClient.SingletonInner.singleton;
  }

  public TigerHttpClient clientConfig(ClientConfig clientConfig) {
    init(clientConfig.serverUrl, clientConfig.tigerId, clientConfig.privateKey);
    initDomainRefreshTask();
    if (clientConfig.isAutoGrabPermission) {
      TigerHttpRequest request = new TigerHttpRequest(MethodName.GRAB_QUOTE_PERMISSION);
      request.setBizContent(AccountParamBuilder.instance().buildJsonWithoutDefaultAccount());
      TigerHttpResponse response = execute(request);
      ApiLogger.info("tigerId:{}, grab_quote_permission:{}, data:{}",
          tigerId, response.getMessage(), response.getData());
    }
    return this;
  }

  /** please use TigerHttpClient.getInstance().clientConfig(ClientConfig.DEFAULT_CONFIG) */
  @Deprecated
  public TigerHttpClient(String serverUrl, String tigerId, String privateKey) {
    init(serverUrl, tigerId, privateKey);
  }

  private void init(String serverUrl, String tigerId, String privateKey) {
    if (tigerId == null) {
      throw new RuntimeException("tigerId is empty.");
    }
    if (privateKey == null) {
      throw new RuntimeException("privateKey is empty.");
    }
    this.tigerId = tigerId;
    this.privateKey = privateKey;
    if (ClientConfig.DEFAULT_CONFIG.getEnv() == Env.PROD) {
      this.tigerPublicKey = ONLINE_PUBLIC_KEY;
    } else {
      this.tigerPublicKey = SANDBOX_PUBLIC_KEY;
    }
    this.deviceId = NetworkUtil.getDeviceId();

    initLicense();
    if (Env.PROD == ClientConfig.DEFAULT_CONFIG.getEnv() || StringUtils.isEmpty(serverUrl)) {
      refreshUrl();
    } else {
      this.serverUrl = serverUrl;
    }
    if (this.serverUrl == null) {
      throw new RuntimeException("serverUrl is empty.");
    }
  }

  @Deprecated
  public TigerHttpClient(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  @Deprecated
  public TigerHttpClient(String serverUrl, String accessToken) {
    this.serverUrl = serverUrl;
    this.accessToken = accessToken;
  }

  private void initLicense() {
    if (null == ClientConfig.DEFAULT_CONFIG.license) {
      try {
        Map<BizType, String> urlMap = NetworkUtil.getHttpServerAddress(null, this.serverUrl);
        this.serverUrl = urlMap.get(BizType.COMMON);
        TigerHttpRequest request = new TigerHttpRequest(MethodName.USER_LICENSE);
        request.setBizContent(AccountParamBuilder.instance().buildJsonWithoutDefaultAccount());
        TigerHttpResponse response = execute(request);
        if (response.isSuccess()) {
          LicenseItem data = JSON.parseObject(response.getData(), LicenseItem.class);
          ApiLogger.debug("license:{}", data);
          ClientConfig.DEFAULT_CONFIG.license = License.valueOf(data.getLicense());
        }
      } catch (Exception e) {
        ApiLogger.debug("get license fail. tigerId:{}", tigerId);
      }
    }
  }

  private void initDomainRefreshTask() {
    synchronized (TigerHttpClient.SingletonInner.singleton) {
      if (domainExecutorService == null || domainExecutorService.isTerminated()) {
        domainExecutorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
          @Override
          public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
          }
        });
        domainExecutorService.scheduleWithFixedDelay(
            new Runnable() {
              @Override
              public void run() {
                refreshUrl();
              }
            }, REFRESH_URL_INTERVAL_SECONDS,
            REFRESH_URL_INTERVAL_SECONDS, TimeUnit.SECONDS);
      }
    }
  }

  private void refreshUrl() {
    try {
      Map<BizType, String> urlMap = NetworkUtil.getHttpServerAddress(ClientConfig.DEFAULT_CONFIG.license, this.serverUrl);
      String newServerUrl = urlMap.get(BizType.TRADE);
      if (newServerUrl == null) {
        newServerUrl = urlMap.get(BizType.COMMON);
      }
      String newQuoteServerUrl = urlMap.get(BizType.QUOTE) == null ? newServerUrl : urlMap.get(BizType.QUOTE);
      String newPaperServerUrl = urlMap.get(BizType.PAPER) == null ? newServerUrl : urlMap.get(BizType.PAPER);

      this.serverUrl = newServerUrl;
      this.quoteServerUrl = newQuoteServerUrl;
      this.paperServerUrl = newPaperServerUrl;
    } catch (Throwable t) {
      ApiLogger.error("refresh serverUrl error", t);
    }
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void setTradeToken(String tradeToken) {
    this.tradeToken = tradeToken;
  }

  public String getTradeToken() {
    return tradeToken;
  }

  public void setAccountType(AccountType accountType) {
    if (accountType != null) {
      this.accountType = accountType.name();
    }
  }

  public String getAccountType() {
    return accountType;
  }

  @Override
  public <T extends TigerResponse> T execute(TigerRequest<T> request) {
    T response;
    String param = null;
    String data = null;
    try {
      validate(request);
      // after successful verification（string enumeration values may be reset）, generate JSON data
      param = JSON.toJSONString(buildParams(request), SerializerFeature.WriteEnumUsingToString);
      ApiLogger.debug("request param:{}", param);

      data = HttpUtils.post(getServerUrl(request), param);

      if (StringUtils.isEmpty(data)) {
        throw new TigerApiException(TigerApiCode.EMPTY_DATA_ERROR);
      }
      response = JSON.parseObject(data, request.getResponseClass());
      if (MethodName.CONTRACT == request.getApiMethodName()) {
        convertContractItem(response, request.getApiVersion());
      }
      if (StringUtils.isEmpty(this.tigerPublicKey) || response.getSign() == null) {
        return response;
      }
      boolean signSuccess =
          TigerSignature.rsaCheckContent(request.getTimestamp(), response.getSign(), this.tigerPublicKey, this.charset);

      if (!signSuccess) {
        throw new TigerApiException(TigerApiCode.SIGN_CHECK_FAILED);
      }
      return response;
    } catch (RuntimeException e) {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), param, data, e);
      return errorResponse(tigerId, request, e);
    } catch (TigerApiException e) {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), param, data, e);
      return errorResponse(tigerId, request, e);
    } catch (Exception e) {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), param, data, e);
      return errorResponse(tigerId, request, e);
    }
  }

  private void convertContractItem(TigerResponse response, String apiVersion) {
    if (response instanceof ContractResponse) {
      ContractResponse contractResponse = (ContractResponse) response;
      if (StringUtils.isEmpty(contractResponse.getData())) {
        return;
      }
      if (V2_0.equals(apiVersion)) {
        List<ContractItem> items = ContractItem.convertFromJsonV2(contractResponse.getData());
        contractResponse.setItems(items);
        if (items != null && items.size() > 0) {
          contractResponse.setItem(items.get(0));
        }
      } else {
        contractResponse.setItem(ContractItem.convertFromJson(contractResponse.getData()));
      }
    }
  }

  private <T extends TigerResponse> T errorResponse(String tigerId, TigerRequest<T> request, TigerApiException e) {
    try {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), e);

      T response = request.getResponseClass().newInstance();
      response.setCode(e.getErrCode());
      response.setMessage(e.getErrMsg());
      return response;
    } catch (Exception e1) {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), e1);
      return null;
    }
  }

  private <T extends TigerResponse> T errorResponse(String tigerId, TigerRequest<T> request, Exception e) {
    try {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), e);

      T response = request.getResponseClass().newInstance();
      response.setCode(TigerApiCode.CLIENT_API_ERROR.getCode());
      response.setMessage(TigerApiCode.CLIENT_API_ERROR.getMessage() + "(" + e.getMessage() + ")");
      return response;
    } catch (Exception e1) {
      ApiLogger.error(tigerId, request.getApiMethodName(), request.getApiVersion(), e1);
      return null;
    }
  }

  private Map<String, Object> buildParams(TigerRequest request) {
    Map<String,Object> params = new HashMap<>();
    params.put(METHOD, request.getApiMethodName().getValue());
    params.put(VERSION, request.getApiVersion());
    params.put(SDK_VERSION, SdkVersionUtils.getSdkVersion());
    if (request instanceof TigerHttpRequest) {
      params.put(BIZ_CONTENT, ((TigerHttpRequest) request).getBizContent());
    } else {
      ApiModel apiModel = request.getApiModel();
      if (apiModel instanceof BatchApiModel) {
        params.put(BIZ_CONTENT, JSON.toJSONString(((BatchApiModel) apiModel).getItems(), SerializerFeature.WriteEnumUsingToString));
      } else if (apiModel instanceof TradeOrderModel) {
        params.put(BIZ_CONTENT, JSON.toJSONString(apiModel, SerializerFeature.WriteEnumUsingToString));
      } else {
        params.put(BIZ_CONTENT, JSON.toJSONString(apiModel, SerializerFeature.WriteEnumUsingToString));
      }
    }
    params.put(TIMESTAMP, request.getTimestamp());
    params.put(CHARSET, this.charset);
    params.put(TIGER_ID, this.tigerId);
    params.put(SIGN_TYPE, this.signType);
    if (this.accessToken != null) {
      params.put(ACCESS_TOKEN, this.accessToken);
    }
    if (this.tradeToken != null) {
      params.put(TRADE_TOKEN, this.tradeToken);
    }
    if (this.accountType != null) {
      params.put(ACCOUNT_TYPE, this.accountType);
    }
    if (this.deviceId != null) {
      params.put(DEVICE_ID, this.deviceId);
    }
    if (this.tigerId != null) {
      String content = TigerSignature.getSignContent(params);
      params.put(SIGN, TigerSignature.rsaSign(content, privateKey, charset));
    }

    return params;
  }

  /**
   * validate parameters
   * @param request
   * @throws TigerApiException
   */
  private void validate(TigerRequest request) throws TigerApiException {
    if (request instanceof TigerHttpRequest) {
      return;
    }
    // TigerCommonRequest
    ValidatorManager.getInstance().validate(request.getApiModel());
  }

  private String getServerUrl(TigerRequest request) {
    String url = null;
    MethodType methodType = request.getApiMethodName().getType();
    if (MethodType.QUOTE == methodType) {
      url = this.quoteServerUrl;
    } else if (MethodType.TRADE == methodType && paperServerUrl != null) {
      String account = AccountUtil.parseAccount(request);
      if (AccountUtil.isVirtualAccount(account)) {
        url = this.paperServerUrl;
      }
    }
    return url == null ? this.serverUrl : url;
  }

}
