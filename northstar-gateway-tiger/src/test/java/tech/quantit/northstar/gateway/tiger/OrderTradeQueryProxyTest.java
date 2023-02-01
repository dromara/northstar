package tech.quantit.northstar.gateway.tiger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tigerbrokers.stock.openapi.client.https.request.TigerHttpRequest;
import com.tigerbrokers.stock.openapi.client.https.response.TigerHttpResponse;

import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import test.common.TestFieldFactory;

class OrderTradeQueryProxyTest {
	
	private OrderTradeQueryProxy proxy;
	
	private TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	private TigerHttpClient client = mock(TigerHttpClient.class);
	
	@BeforeEach
	void prepare() {
		IContractManager contractMgr = mock(IContractManager.class);
		Contract contract = mock(Contract.class);
		when(contract.contractField()).thenReturn(factory.makeContract("rb2205"));
		when(contractMgr.getContract(anyString(), anyString())).thenReturn(contract);
		proxy = new OrderTradeQueryProxy(client, contractMgr, "testGateway", "testAccount");
	}

	@Test
	void testGetDeltaOrder() {
		String demoStr = "{\"nextPageToken\":\"b3JkZXJzfG51bGx8bnVsbHwyOTQ5NTY2MTY5NjEyMjg4MA==\",\"items\":[{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29530665135767552,\"orderId\":10,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.84,\"totalQuantity\":200,\"filledQuantity\":200,\"avgFillPrice\":11.84,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":22.37,\"realizedPnl\":0.0,\"remark\":\"\",\"liquidation\":false,\"openTime\":1674200295000,\"updateTime\":1674200310000,\"latestTime\":1674200310000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29530383993667584,\"orderId\":9,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.78,\"totalQuantity\":200,\"filledQuantity\":200,\"avgFillPrice\":11.78,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":22.36,\"realizedPnl\":0.0,\"remark\":\"\",\"liquidation\":false,\"openTime\":1674198150000,\"updateTime\":1674198158000,\"latestTime\":1674198158000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29530343978696704,\"orderId\":8,\"account\":\"20221212084743666\",\"action\":\"SELL\",\"orderType\":\"LMT\",\"limitPrice\":11.76,\"totalQuantity\":200,\"filledQuantity\":200,\"avgFillPrice\":11.76,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":22.36,\"realizedPnl\":-56.73,\"remark\":\"\",\"liquidation\":false,\"openTime\":1674197844000,\"updateTime\":1674197845000,\"latestTime\":1674197845000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29529968524787712,\"orderId\":7,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.82,\"totalQuantity\":200,\"filledQuantity\":200,\"avgFillPrice\":11.82,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":22.37,\"realizedPnl\":0.0,\"remark\":\"\",\"liquidation\":false,\"openTime\":1674194980000,\"updateTime\":1674194981000,\"latestTime\":1674194982000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29517286654477312,\"orderId\":6,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.66,\"totalQuantity\":500,\"filledQuantity\":0,\"avgFillPrice\":0.0,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":0.0,\"realizedPnl\":0.0,\"remark\":\"The order size must be a multiple of 200. If you want to sell odd-lots, you must use the \\\"Odd-Lot-Sell\\\" Function which is available in the \\\"More\\\" section of the \\\"Trade\\\" interface\",\"liquidation\":false,\"openTime\":1674098225000,\"updateTime\":1674098225000,\"latestTime\":1674098225000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Invalid\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29517264261087232,\"orderId\":5,\"account\":\"20221212084743666\",\"action\":\"SELL\",\"orderType\":\"LMT\",\"limitPrice\":11.64,\"totalQuantity\":1000,\"filledQuantity\":1000,\"avgFillPrice\":11.66,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":37.79,\"realizedPnl\":-30.2828571,\"remark\":\"\",\"liquidation\":false,\"openTime\":1674098054000,\"updateTime\":1674098054000,\"latestTime\":1674098055000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29508040482489344,\"orderId\":4,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.0,\"totalQuantity\":1000,\"filledQuantity\":0,\"avgFillPrice\":0.0,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":0.0,\"realizedPnl\":0.0,\"remark\":\"Order is expired\",\"liquidation\":false,\"openTime\":1674027682000,\"updateTime\":1674030600000,\"latestTime\":1674030600000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Inactive\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29508007821837312,\"orderId\":3,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.0,\"totalQuantity\":1000,\"filledQuantity\":0,\"avgFillPrice\":0.0,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":0.0,\"realizedPnl\":0.0,\"liquidation\":false,\"openTime\":1674027433000,\"updateTime\":1674027635000,\"latestTime\":1674027636000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Cancelled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29507834575455232,\"orderId\":2,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.0,\"totalQuantity\":400,\"filledQuantity\":0,\"avgFillPrice\":0.0,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":0.0,\"realizedPnl\":0.0,\"liquidation\":false,\"openTime\":1674026111000,\"updateTime\":1674026600000,\"latestTime\":1674026600000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Cancelled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29507813534729216,\"orderId\":1,\"account\":\"20221212084743666\",\"action\":\"SELL\",\"orderType\":\"LMT\",\"limitPrice\":11.86,\"totalQuantity\":400,\"filledQuantity\":400,\"avgFillPrice\":11.88,\"timeInForce\":\"DAY\",\"outsideRth\":true,\"commission\":26.26,\"realizedPnl\":64.7428571,\"remark\":\"\",\"liquidation\":false,\"openTime\":1674025951000,\"updateTime\":1674025951000,\"latestTime\":1674025951000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false},{\"symbol\":\"01810\",\"market\":\"HK\",\"secType\":\"STK\",\"currency\":\"HKD\",\"identifier\":\"01810\",\"id\":29495661696122880,\"account\":\"20221212084743666\",\"action\":\"BUY\",\"orderType\":\"LMT\",\"limitPrice\":11.64,\"totalQuantity\":1400,\"filledQuantity\":1400,\"avgFillPrice\":11.62,\"timeInForce\":\"DAY\",\"outsideRth\":false,\"commission\":45.49,\"realizedPnl\":0.0,\"remark\":\"\",\"liquidation\":false,\"openTime\":1673933240000,\"updateTime\":1673933240000,\"latestTime\":1673933240000,\"name\":\"XIAOMI-W\",\"attrDesc\":\"\",\"userMark\":\"\",\"algoStrategy\":\"LMT\",\"status\":\"Filled\",\"source\":\"android\",\"discount\":0.0,\"canModify\":false,\"canCancel\":false}]}";
		JSONObject json = JSON.parseObject(demoStr);
		JSONArray arr = json.getJSONArray("items");
		for(int i=0; i<arr.size(); i++) {
			JSONObject embedObj = arr.getJSONObject(i);
			embedObj.put("openTime", System.currentTimeMillis());
		}
		TigerHttpResponse response = mock(TigerHttpResponse.class);
		when(client.execute(any(TigerHttpRequest.class))).thenReturn(response);
		when(response.isSuccess()).thenReturn(true);
		when(response.getData()).thenReturn(json.toJSONString());
		
		assertThat(proxy.getDeltaOrder()).hasSize(11);
		assertThat(proxy.getDeltaOrder()).isEmpty();
	}

	@Test
	void testGetDeltaTrade() {
		String demoStr = "{\"items\":[{\"id\":29530667114823680,\"accountId\":20221212084743666,\"orderId\":29530665135767552,\"secType\":\"STK\",\"symbol\":\"01810\",\"right\":\"PUT\",\"currency\":\"HKD\",\"market\":\"HK\",\"action\":\"BUY\",\"filledQuantity\":200,\"filledPrice\":11.84,\"filledAmount\":2368.0,\"transactedAt\":\"2023-01-20 15:38:30\",\"transactionTime\":1674200310180}]}";
		JSONObject json = JSON.parseObject(demoStr);
		JSONArray arr = json.getJSONArray("items");
		for(int i=0; i<arr.size(); i++) {
			JSONObject embedObj = arr.getJSONObject(i);
			embedObj.put("transactionTime", System.currentTimeMillis());
		}
		
		TigerHttpResponse response = mock(TigerHttpResponse.class);
		when(client.execute(any(TigerHttpRequest.class))).thenReturn(response);
		when(response.isSuccess()).thenReturn(true);
		when(response.getData()).thenReturn(json.toJSONString());
		
		assertThat(proxy.getDeltaTrade(2L)).hasSize(1);
		assertThat(proxy.getDeltaTrade(2L)).isEmpty();
	}

}
