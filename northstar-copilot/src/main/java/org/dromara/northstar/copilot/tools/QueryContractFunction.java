package org.dromara.northstar.copilot.tools;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.LocalEnvUtils;
import org.dromara.northstar.copilot.annotation.FunctionCall;
import org.dromara.northstar.copilot.annotation.FunctionParameter;
import org.dromara.northstar.copilot.annotation.FunctionRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@FunctionCall(name = "queryContract", description = "根据合约名称或者合约编码查询合约信息")
public class QueryContractFunction implements Function<QueryContractFunction.Request, String> {

	@Override
	public String apply(Request req) {
		Set<String> profiles = Set.of(LocalEnvUtils.getEnvironment().getActiveProfiles());
		String baseUrl = profiles.contains("dev") ? "http://localhost" : "https://localhost";
		String url = String.format("%s/northstar/copilot/function/queryContract?query=%s", baseUrl, req.query);
		OkHttpClient client = new OkHttpClient();

		okhttp3.Request http = new okhttp3.Request.Builder().addHeader(Constants.CSRF_HEADER, req.token).url(url).get().build();

		try (Response response = client.newCall(http).execute()) {
			String bodyStr = response.body().string();
			if (response.isSuccessful()) {
				ObjectMapper mapper = new ObjectMapper();
				List<Contract> contracts = mapper.readValue(bodyStr, new TypeReference<List<Contract>>() {});
				if(contracts.size() > 5) {
					return String.format("一共找到了%d个结果，请让用户给出更具体的信息以便缩小查询范围", contracts.size()) ;
				}
				if(contracts.size() > 1) {
					List<String> nameAndCodePairs = contracts.stream().map(c -> String.format("编码:%s, 名称:%s", c.symbol(), c.name())).toList();
					return String.format("一共找到了%d个结果，请把这些结果：%s，展示让用户选择其中一个来展示", contracts.size(), mapper.writeValueAsString(nameAndCodePairs));
				}
				return mapper.writeValueAsString(contracts);
			} else {
				return "获取合约失败，具体原因：" + bodyStr;
			}
		} catch (Exception e) {
			return "获取合约失败，具体原因：" + e.getLocalizedMessage();
		}
	}

	@Data
	@FunctionRequest
	public static class Request {
		@FunctionParameter(description = "要查询的合约名称或者合约编码")
		String query;
		@FunctionParameter(description = "请求所需的token")
		String token;
	}

}
