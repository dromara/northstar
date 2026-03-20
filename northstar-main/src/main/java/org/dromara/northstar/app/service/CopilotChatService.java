package org.dromara.northstar.app.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.ChatCompletionDTO;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.InternalLoopCallUtils;
import org.dromara.northstar.copilot.listener.SseListener;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletion;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;
import org.dromara.northstar.copilot.service.IChatService;
import org.dromara.northstar.copilot.service.IHelperFactory;
import org.dromara.northstar.copilot.service.PlatformType;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;

@Slf4j
@Service
public class CopilotChatService {
	
	@Autowired(required = false)
	private List<IChatService> chatServices;
	
	ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());

	public SseEmitter chatCompletion(ChatCompletionDTO chats) {
		PlatformType platform = PlatformType.getPlatform(chats.getPlatform());
		if(chatServices == null) {
			throw new OperationException("error.without_setup_any_platform");
		}
		SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
		sseEmitter.onError(e -> sseEmitter.complete());
		
		IChatService chatService = chatServices.stream().filter(cs -> cs.platformType() == platform).findAny().get();
		exec.execute(() -> {
			try {
				IHelperFactory helperFactory = (IHelperFactory) chatService;
				ChatCompletion chatCompletion = null;
				if("beginnerHelper".equals(chats.getHelper())) {
					chatCompletion = helperFactory.actAsBeginnerHelper(chats.getContents());
				} else if ("contractLookupHelper".equals(chats.getHelper())) {
					chatCompletion = helperFactory.actAsContractLookupHelper(chats.getContents());
					chatCompletion.getMessages().add(ChatMessage.withSystem("查询接口的token=" + InternalLoopCallUtils.produce()));
				} else if ("strategyHelper".equals(chats.getHelper())) {
					chatCompletion = helperFactory.actAsStrategyHelper(chats.getContents());
				} else {
					throw new IllegalArgumentException(String.format("暂时无法处理[%s]的问答", chats.getHelper()));
				}
				
				chatService.chatCompletionStream(chatCompletion, new SseListener() {
					
					@Override
					public void onEvent(EventSource eventSource, String id, String type, String data) {
						super.onEvent(eventSource, id, type, data);
						if ("[DONE]".equalsIgnoreCase(data) && !super.getFinishReason().equals("tool_calls")) {
							try {
								sseEmitter.send(SseEmitter.event().name("done").build());
							} catch (IOException e) {
								log.error("", e);
							}
						}
						if(!StringUtils.isEmpty(super.getFinishReason()) && !"stop".equals(super.getFinishReason())
								&& !"tool_calls".equals(super.getFinishReason())) {
							ObjectMapper mapper = new ObjectMapper();
							try {
								log.warn("非正常结束对话，响应详情：{}", mapper.writeValueAsString(this));
							} catch (JsonProcessingException e) {
								log.warn("", e);
							}
						}
					}
					
					@Override
					public void onFailure(EventSource eventSource, Throwable t, Response response) {
						super.onFailure(eventSource, t, response);
						try {
							String errMsg = response.body().string();
							log.warn("异常提示：{}", errMsg);
							log.warn("", t);
							if(errMsg.contains("rate_limit_reached")) {
								sseEmitter.send(SseEmitter.event().name("hints").data("接口限流，请稍候").reconnectTime(3000L));
								Thread.sleep(5000);
							}
						} catch (Exception e) {
							log.error("", e);
						}
					}

					@Override
					protected void send() {
						try {					
							sseEmitter.send(SseEmitter.event().name("answer").data(getCurrStr()).reconnectTime(3000L));
							Thread.sleep(40);
						} catch(Exception e) {
							log.error("", e);
						}
					}
					
				});
			} catch (Exception ex) {
				sseEmitter.complete();
				throw new IllegalStateException(ex);
			}
		});
		
		return sseEmitter;
	}
	
	public List<String> availablePlatforms(){
		if(chatServices == null) {
			return Collections.emptyList();
		}
		return chatServices.stream().map(cs -> cs.platformType().getPlatform()).toList();
	}
	
	/* ===============   工具类辅助接口   ==================== */
	
	@Autowired
	private MarketCenter marketCenter;

	public List<Contract> queryContract(String query) {
		if(StringUtils.isEmpty(query)) {
			return Collections.emptyList();
		}
		return marketCenter.channelContracts().values()
				.stream()
				.flatMap(List::stream)
				.filter(c -> c.name().contains(query) || c.symbol().contains(query))
				.toList();
	}
	
}
