package org.dromara.northstar.copilot.platform.moonshot.chat;

import java.io.IOException;
import java.util.List;

import org.dromara.northstar.copilot.platform.moonshot.chat.entity.MoonshotCaching;
import org.dromara.northstar.copilot.platform.moonshot.chat.entity.MoonshotCachingListResponse;
import org.dromara.northstar.copilot.platform.moonshot.chat.entity.MoonshotCachingResponse;
import org.dromara.northstar.copilot.utils.SharedInfoUtils;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MoonshotCachingHelper {
	
	private final OkHttpClient client = new OkHttpClient();
	private final ObjectMapper mapper = new ObjectMapper();

	public MoonshotCachingResponse createCache(MoonshotCaching caching) throws IOException {
		String apiKey = SharedInfoUtils.getMoonshotConfig().getApiKey();
		String url = SharedInfoUtils.getMoonshotConfig().getApiHost() + SharedInfoUtils.getMoonshotConfig().getCachingUrl();
		String postBody = mapper.writeValueAsString(caching);

		Request http = new Request.Builder()
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.url(url)
				.post(RequestBody.create(postBody, MediaType.parse("application/json")))
				.build();

		try (Response response = client.newCall(http).execute()) {
			if (response.isSuccessful()) {
				String bodyStr = response.body().string();
				return mapper.readValue(bodyStr, new TypeReference<MoonshotCachingResponse>() {});
			} else {
				throw new IllegalStateException("缓存创建失败，原因：" + response.message());
			}
		}
	}
	
	public List<MoonshotCachingResponse> getCachings(){
		String cachingUrl = SharedInfoUtils.getMoonshotConfig().getApiHost() + SharedInfoUtils.getMoonshotConfig().getCachingUrl();
		String apiKey = SharedInfoUtils.getMoonshotConfig().getApiKey();
		Request http = new Request.Builder()
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.url(cachingUrl)
				.get()
				.build();
		
		try (Response response = client.newCall(http).execute()) {
			if (response.isSuccessful()) {
				String bodyStr = response.body().string();
				MoonshotCachingListResponse list = mapper.readValue(bodyStr, new TypeReference<MoonshotCachingListResponse>() {});
				return list.getData();
			} else {
				throw new IllegalStateException("缓存获取失败，原因：" + response.message());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public MoonshotCachingResponse getCaching(String cacheId) throws IOException {
		String apiKey = SharedInfoUtils.getMoonshotConfig().getApiKey();
		String url = SharedInfoUtils.getMoonshotConfig().getApiHost() + SharedInfoUtils.getMoonshotConfig().getCachingUrl() + "/" + cacheId;

		Request http = new Request.Builder()
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.url(url)
				.get()
				.build();

		try (Response response = client.newCall(http).execute()) {
			if (response.isSuccessful()) {
				String bodyStr = response.body().string();
				return mapper.readValue(bodyStr, new TypeReference<MoonshotCachingResponse>() {});
			} else {
				throw new IllegalStateException("缓存查询失败，原因：" + response.message());
			}
		}
	}
	
	public void removeCache(String cacheId) {
		String cachingUrl = SharedInfoUtils.getMoonshotConfig().getApiHost() + SharedInfoUtils.getMoonshotConfig().getCachingUrl() + "/" + cacheId;
		String apiKey = SharedInfoUtils.getMoonshotConfig().getApiKey();
		Request http = new Request.Builder()
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.url(cachingUrl)
				.delete()
				.build();
		
		try (Response response = client.newCall(http).execute()) {
			if (response.isSuccessful()) {
				return;
			} else {
				throw new IllegalStateException("缓存删除失败，原因：" + response.message());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void housekeeping() {
		getCachings().stream()
			.filter(c -> c.getStatus().equals("inactive") || c.getStatus().equals("error"))
			.forEach(c -> removeCache(c.getId()));
	}
}
