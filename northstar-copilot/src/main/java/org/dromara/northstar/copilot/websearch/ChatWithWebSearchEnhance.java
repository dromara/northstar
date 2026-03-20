package org.dromara.northstar.copilot.websearch;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.copilot.exception.CommonException;
import org.dromara.northstar.copilot.listener.SseListener;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletion;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletionResponse;
import org.dromara.northstar.copilot.platform.openai.chat.entity.Content;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.IChatService;
import org.dromara.northstar.copilot.service.PlatformType;
import org.dromara.northstar.copilot.utils.ValidateUtil;
import org.dromara.northstar.copilot.websearch.searxng.SearXNGConfig;
import org.dromara.northstar.copilot.websearch.searxng.SearXNGResponse;

import com.alibaba.fastjson2.JSON;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/12/11 22:32
 */
public class ChatWithWebSearchEnhance implements IChatService {
    private final IChatService chatService;
    private final SearXNGConfig searXNGConfig;
    private final OkHttpClient okHttpClient;
    public ChatWithWebSearchEnhance(IChatService chatService, Configuration configuration) {
        this.chatService = chatService;
        this.searXNGConfig = configuration.getSearXNGConfig();
        this.okHttpClient = configuration.getOkHttpClient();
    }

    @Override
    public ChatCompletionResponse chatCompletion(String baseUrl, String apiKey, ChatCompletion chatCompletion) throws Exception {
        return chatService.chatCompletion(baseUrl, apiKey, addWebSearchResults(chatCompletion));
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion) throws Exception {
        return chatService.chatCompletion(addWebSearchResults(chatCompletion));
    }

    @Override
    public void chatCompletionStream(String baseUrl, String apiKey, ChatCompletion chatCompletion, SseListener eventSourceListener) throws Exception {
        chatService.chatCompletionStream(baseUrl, apiKey, addWebSearchResults(chatCompletion), eventSourceListener);
    }

    @Override
    public void chatCompletionStream(ChatCompletion chatCompletion, SseListener eventSourceListener) throws Exception {
        chatService.chatCompletionStream(addWebSearchResults(chatCompletion), eventSourceListener);
    }


    private ChatCompletion addWebSearchResults(ChatCompletion chatCompletion) {
        int chatLen = chatCompletion.getMessages().size();
        String prompt = chatCompletion.getMessages().get(chatLen - 1).getContent().getText();
        // 执行联网搜索并将结果附加到提示词中
        String searchResults = performWebSearch(prompt);
        chatCompletion.getMessages().get(chatLen - 1).setContent(Content.ofText("我将提供一段来自互联网的资料信息, 请根据这段资料以及用户提出的问题来给出回答。请确保在回答中使用Markdown格式，并在回答末尾列出参考资料。如果资料中的信息不足以回答用户的问题，可以根据自身知识库进行补充，或者说明无法提供确切的答案。\n" +
                "网络资料:\n"
                + "============\n"
                + searchResults
                + "============\n"
                + "用户问题:\n"
                + "============\n"
                + prompt
                + "============\n"));
        return chatCompletion;
    }

    private String performWebSearch(String query) {
        if(StringUtils.isBlank(searXNGConfig.getUrl())){
            throw new CommonException("SearXNG url is not configured");
        }


        Request request = new Request.Builder()
                .url(ValidateUtil.concatUrl(searXNGConfig.getUrl(), "?format=json&q=" + query + "&engines=" + searXNGConfig.getEngines()))
                .get()
                .build();


        try(Response execute = okHttpClient.newCall(request).execute()) {
            if (execute.isSuccessful() && execute.body() != null){
                SearXNGResponse searXNGResponse = JSON.parseObject(execute.body().string(), SearXNGResponse.class);

                if(searXNGResponse.getResults().size() > searXNGConfig.getNums()) {
                    return JSON.toJSONString(searXNGResponse.getResults().subList(0, searXNGConfig.getNums()));
                }
                return JSON.toJSONString(searXNGResponse.getResults());


            }else{
                throw new CommonException("SearXNG request failed");
            }


        } catch (Exception e) {
            throw new CommonException("SearXNG request failed");
        }


    }

	@Override
	public PlatformType platformType() {
		return chatService.platformType();
	}
}
