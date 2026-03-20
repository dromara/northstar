package org.dromara.northstar.copilot.platform.openai.chat;

import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.common.model.ChatCompletionDTO.ContentDTO;
import org.dromara.northstar.copilot.config.OpenAiConfig;
import org.dromara.northstar.copilot.constant.Constants;
import org.dromara.northstar.copilot.listener.SseListener;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletion;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletionResponse;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;
import org.dromara.northstar.copilot.platform.openai.chat.entity.Choice;
import org.dromara.northstar.copilot.platform.openai.chat.entity.Content;
import org.dromara.northstar.copilot.platform.openai.chat.entity.StreamOptions;
import org.dromara.northstar.copilot.platform.openai.tool.Tool;
import org.dromara.northstar.copilot.platform.openai.tool.ToolCall;
import org.dromara.northstar.copilot.platform.openai.usage.Usage;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.IChatService;
import org.dromara.northstar.copilot.service.IHelperFactory;
import org.dromara.northstar.copilot.service.PlatformType;
import org.dromara.northstar.copilot.utils.ToolUtil;
import org.dromara.northstar.copilot.utils.ValidateUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;

/**
 * @Author cly
 * @Description OpenAi 聊天服务
 * @Date 2024/8/2 23:16
 */
@Slf4j
public class OpenAiChatService implements IHelperFactory, IChatService {

    private final OpenAiConfig openAiConfig;
    private final OkHttpClient okHttpClient;
    private final EventSource.Factory factory;

    public OpenAiChatService(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }


    @Override
    public ChatCompletionResponse chatCompletion(String baseUrl, String apiKey, ChatCompletion chatCompletion)  throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();
        chatCompletion.setStream(false);
        chatCompletion.setStreamOptions(null);

        if(chatCompletion.getFunctions()!=null && !chatCompletion.getFunctions().isEmpty()){
            List<Tool> tools = ToolUtil.getAllFunctionTools(chatCompletion.getFunctions());
            chatCompletion.setTools(tools);
            if(tools == null){
                chatCompletion.setParallelToolCalls(null);
            }
        }else{
            chatCompletion.setParallelToolCalls(null);
        }


        // 总token消耗
        Usage allUsage = new Usage();
        String finishReason = "first";

        while("first".equals(finishReason) || "tool_calls".equals(finishReason)){

            finishReason = null;

            // 构造请求
            ObjectMapper mapper = new ObjectMapper();
            String requestString = mapper.writeValueAsString(chatCompletion);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(requestString, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                    .build();
            log.debug("请求地址：{}", request.toString());
            Response execute = okHttpClient.newCall(request).execute();
            if (execute.isSuccessful() && execute.body() != null){
                ChatCompletionResponse chatCompletionResponse = mapper.readValue(execute.body().string(), ChatCompletionResponse.class);

                Choice choice = chatCompletionResponse.getChoices().get(0);
                finishReason = choice.getFinishReason();

                Usage usage = chatCompletionResponse.getUsage();
                allUsage.setCompletionTokens(allUsage.getCompletionTokens() + usage.getCompletionTokens());
                allUsage.setTotalTokens(allUsage.getTotalTokens() + usage.getTotalTokens());
                allUsage.setPromptTokens(allUsage.getPromptTokens() + usage.getPromptTokens());

                // 判断是否为函数调用返回
                if("tool_calls".equals(finishReason)){
                    ChatMessage message = choice.getMessage();
                    List<ToolCall> toolCalls = message.getToolCalls();

                    List<ChatMessage> messages = new ArrayList<>(chatCompletion.getMessages());
                    messages.add(message);

                    // 添加 tool 消息
                    for (ToolCall toolCall : toolCalls) {
                        String functionName = toolCall.getFunction().getName();
                        String arguments = toolCall.getFunction().getArguments();
                        String functionResponse = ToolUtil.invoke(functionName, arguments);

                        messages.add(ChatMessage.withTool(functionResponse, toolCall.getId()));
                    }
                    chatCompletion.setMessages(messages);

                }else{
                    // 其他情况直接返回
                    chatCompletionResponse.setUsage(allUsage);


                    return chatCompletionResponse;

                }

            }else{
                return null;
            }

        }


        return null;
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion)  throws Exception {
        return chatCompletion(null, null, chatCompletion);
    }

    @Override
    public void chatCompletionStream(String baseUrl, String apiKey, ChatCompletion chatCompletion, SseListener eventSourceListener) throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();
        chatCompletion.setStream(true);
        StreamOptions streamOptions = chatCompletion.getStreamOptions();
        if(streamOptions == null){
            chatCompletion.setStreamOptions(new StreamOptions(true));
        }

        if(chatCompletion.getFunctions()!=null && !chatCompletion.getFunctions().isEmpty()){
            List<Tool> tools = ToolUtil.getAllFunctionTools(chatCompletion.getFunctions());
            chatCompletion.setTools(tools);
            if(tools == null){
                chatCompletion.setParallelToolCalls(null);
            }
        }else{
            chatCompletion.setParallelToolCalls(null);
        }

        String finishReason = "first";

        while("first".equals(finishReason) || "tool_calls".equals(finishReason)){

            finishReason = null;
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(chatCompletion);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                    .build();
            
            log.debug("请求地址：{}", request.toString());

            factory.newEventSource(request, eventSourceListener);
            eventSourceListener.getCountDownLatch().await();

            finishReason = eventSourceListener.getFinishReason();
            List<ToolCall> toolCalls = eventSourceListener.getToolCalls();

            // 需要调用函数
            if("tool_calls".equals(finishReason) && !toolCalls.isEmpty()){
                // 创建tool响应消息
                ChatMessage responseMessage = ChatMessage.withAssistant(eventSourceListener.getToolCalls());

                List<ChatMessage> messages = new ArrayList<>(chatCompletion.getMessages());
                messages.add(responseMessage);

                // 封装tool结果消息
                for (ToolCall toolCall : toolCalls) {
                    String functionName = toolCall.getFunction().getName();
                    String arguments = toolCall.getFunction().getArguments();
                    String functionResponse = ToolUtil.invoke(functionName, arguments);

                    messages.add(ChatMessage.withTool(functionResponse, toolCall.getId()));
                }
                eventSourceListener.setToolCalls(new ArrayList<>());
                eventSourceListener.setToolCall(null);
                chatCompletion.setMessages(messages);
            }

        }

    }

    @Override
    public void chatCompletionStream(ChatCompletion chatCompletion, SseListener eventSourceListener) throws Exception {
        chatCompletionStream(null, null, chatCompletion, eventSourceListener);
    }


	@Override
	public PlatformType platformType() {
		return PlatformType.OPENAI;
	}

	@Override
	public ChatCompletion actAsBeginnerHelper(List<ContentDTO> contents) {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.builder().role("system").content(Content.ofText("你现在是Northstar盈富量化平台的智能助手，主要通过盈富平台官网查询到的信息给用户解答疑问，用户以下全部的问题，大前提都是在Northstar盈富量化平台的语境下。比如，用户问“合约是什么意思”，用户真正的问题应该是“在Northstar盈富量化平台里，合约这个概念是代表什么”。如果官网查找不到相应的描述，可以按照你已有的理解，给出一个参考答案并说明“详情请加入盈富量化社区咨询”")).build());
		msgs.addAll(contents.stream().map(c -> ChatMessage.builder().role(c.getRole()).content(Content.ofText(c.getContent())).build()).toList());
		return ChatCompletion.builder()
				.model("deepseek-ai/DeepSeek-V3")
				.frequencyPenalty(1f)
				.topP(0.1f)
				.messages(msgs)
				.build();
	}

	@Override
	public ChatCompletion actAsContractLookupHelper(List<ContentDTO> contents) {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.builder().role("system").content(Content.ofText("你现在是Northstar盈富量化平台的合约查询助手，主要通过调用function_call的queryContract来查询合约信息。通过分析用户的问题，来提取用户究竟想要查询什么合约。当返回的合约数量多于1个时，提醒用户给出更准确的合约名称或合约编码。")).build());
		msgs.addAll(contents.stream().map(c -> ChatMessage.builder().role(c.getRole()).content(Content.ofText(c.getContent())).build()).toList());
		return ChatCompletion.builder()
				.model("deepseek-ai/DeepSeek-V2.5")
				.frequencyPenalty(1f)
				.topP(0.1f)
				.messages(msgs)
				.functions("queryContract")
				.build();
	}


	@Override
	public ChatCompletion actAsStrategyHelper(List<ContentDTO> contents) {
		throw new UnsupportedOperationException();
	}
}
