package org.dromara.northstar.copilot.platform.moonshot.chat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.dromara.northstar.common.model.ChatCompletionDTO.ContentDTO;
import org.dromara.northstar.copilot.config.MoonshotConfig;
import org.dromara.northstar.copilot.constant.Constants;
import org.dromara.northstar.copilot.convert.chat.ParameterConvert;
import org.dromara.northstar.copilot.convert.chat.ResultConvert;
import org.dromara.northstar.copilot.exception.CommonException;
import org.dromara.northstar.copilot.listener.SseListener;
import org.dromara.northstar.copilot.platform.moonshot.chat.entity.MoonshotChatCompletion;
import org.dromara.northstar.copilot.platform.moonshot.chat.entity.MoonshotChatCompletionResponse;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletion;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletionResponse;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;
import org.dromara.northstar.copilot.platform.openai.chat.entity.Choice;
import org.dromara.northstar.copilot.platform.openai.chat.entity.Content;
import org.dromara.northstar.copilot.platform.openai.tool.Tool;
import org.dromara.northstar.copilot.platform.openai.tool.Tool.Function;
import org.dromara.northstar.copilot.platform.openai.tool.ToolCall;
import org.dromara.northstar.copilot.platform.openai.usage.Usage;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.IChatService;
import org.dromara.northstar.copilot.service.IHelperFactory;
import org.dromara.northstar.copilot.service.PlatformType;
import org.dromara.northstar.copilot.utils.ToolUtil;
import org.dromara.northstar.copilot.utils.ValidateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ResourceUtils;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

/**
 * @Author cly
 * @Description 月之暗面请求服务
 * @Date 2024/8/29 23:12
 */
@Slf4j
public class MoonshotChatService implements IHelperFactory, IChatService, ParameterConvert<MoonshotChatCompletion>, ResultConvert<MoonshotChatCompletionResponse> {
    private final MoonshotConfig moonshotConfig;
    private final OkHttpClient okHttpClient;
    private final EventSource.Factory factory;

    public MoonshotChatService(Configuration configuration) {
        this.moonshotConfig = configuration.getMoonshotConfig();
        this.okHttpClient = configuration.getOkHttpClient();
        this.factory = configuration.createRequestFactory();
    }

    @Override
    public MoonshotChatCompletion convertChatCompletionObject(ChatCompletion chatCompletion) {
        MoonshotChatCompletion moonshotChatCompletion = new MoonshotChatCompletion();
        moonshotChatCompletion.setModel(chatCompletion.getModel());
        moonshotChatCompletion.setMessages(chatCompletion.getMessages());
        moonshotChatCompletion.setFrequencyPenalty(chatCompletion.getFrequencyPenalty());
        moonshotChatCompletion.setMaxTokens(chatCompletion.getMaxTokens());
        moonshotChatCompletion.setPresencePenalty(chatCompletion.getPresencePenalty());
        moonshotChatCompletion.setResponseFormat(chatCompletion.getResponseFormat());
        moonshotChatCompletion.setStop(chatCompletion.getStop());
        moonshotChatCompletion.setStream(chatCompletion.getStream());
        moonshotChatCompletion.setTemperature(chatCompletion.getTemperature() / 2);
        moonshotChatCompletion.setTopP(chatCompletion.getTopP());
        moonshotChatCompletion.setTools(chatCompletion.getTools());
        moonshotChatCompletion.setFunctions(chatCompletion.getFunctions());
        moonshotChatCompletion.setToolChoice(chatCompletion.getToolChoice());
        return moonshotChatCompletion;
    }

    @Override
    public EventSourceListener convertEventSource(SseListener eventSourceListener) {
        return new EventSourceListener() {
            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                eventSourceListener.onOpen(eventSource, response);
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                eventSourceListener.onFailure(eventSource, t, response);
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                if ("[DONE]".equalsIgnoreCase(data)) {
                    eventSourceListener.onEvent(eventSource, id, type, data);
                    return;
                }


                ObjectMapper mapper = new ObjectMapper();
                MoonshotChatCompletionResponse chatCompletionResponse = null;
                String s = null;
                try {
                    chatCompletionResponse = mapper.readValue(data, MoonshotChatCompletionResponse.class);

                    ChatCompletionResponse response = convertChatCompletionResponse(chatCompletionResponse);

                    // 适配moonshot的流式 usage格式
                    JSONObject object = (JSONObject)JSONPath.eval(data, "$.choices[0].usage");
                    if(object!=null){
                        Usage usage = object.toJavaObject(Usage.class);
                        response.setUsage(usage);
                    }

                    s = mapper.writeValueAsString(response);
                } catch (JsonProcessingException e) {
                    throw new CommonException("Moonshot Chat 对象JSON序列化出错");
                }

                eventSourceListener.onEvent(eventSource, id, type, s);
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                eventSourceListener.onClosed(eventSource);
            }
        };
    }

    @Override
    public ChatCompletionResponse convertChatCompletionResponse(MoonshotChatCompletionResponse moonshotChatCompletionResponse) {
        ChatCompletionResponse chatCompletionResponse = new ChatCompletionResponse();
        chatCompletionResponse.setId(moonshotChatCompletionResponse.getId());
        chatCompletionResponse.setObject(moonshotChatCompletionResponse.getObject());
        chatCompletionResponse.setCreated(moonshotChatCompletionResponse.getCreated());
        chatCompletionResponse.setModel(moonshotChatCompletionResponse.getModel());
        chatCompletionResponse.setChoices(moonshotChatCompletionResponse.getChoices());
        chatCompletionResponse.setUsage(moonshotChatCompletionResponse.getUsage());
        return chatCompletionResponse;
    }

    @Override
    public ChatCompletionResponse chatCompletion(String baseUrl, String apiKey, ChatCompletion chatCompletion) throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = moonshotConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = moonshotConfig.getApiKey();
        chatCompletion.setStream(false);
        // 转换 请求参数
        MoonshotChatCompletion moonshotChatCompletion = this.convertChatCompletionObject(chatCompletion);

        // 如含有function，则添加tool
        if(moonshotChatCompletion.getFunctions()!=null && !moonshotChatCompletion.getFunctions().isEmpty()){
            List<Tool> tools = ToolUtil.getAllFunctionTools(moonshotChatCompletion.getFunctions());
            moonshotChatCompletion.setTools(tools);
        }

        // 总token消耗
        Usage allUsage = new Usage();

        String finishReason = "first";

        while("first".equals(finishReason) || "tool_calls".equals(finishReason)){

            finishReason = null;

            // 构造请求
            ObjectMapper mapper = new ObjectMapper();
            String requestString = mapper.writeValueAsString(moonshotChatCompletion);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, moonshotConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(requestString, MediaType.parse(Constants.JSON_CONTENT_TYPE)))
                    .build();
            log.debug("请求地址：{}", request.toString());
            Response execute = okHttpClient.newCall(request).execute();
            if (execute.isSuccessful() && execute.body() != null){
                MoonshotChatCompletionResponse moonshotChatCompletionResponse = mapper.readValue(execute.body().string(), MoonshotChatCompletionResponse.class);

                Choice choice = moonshotChatCompletionResponse.getChoices().get(0);
                finishReason = choice.getFinishReason();

                Usage usage = moonshotChatCompletionResponse.getUsage();
                allUsage.setCompletionTokens(allUsage.getCompletionTokens() + usage.getCompletionTokens());
                allUsage.setTotalTokens(allUsage.getTotalTokens() + usage.getTotalTokens());
                allUsage.setPromptTokens(allUsage.getPromptTokens() + usage.getPromptTokens());

                // 判断是否为函数调用返回
                if("tool_calls".equals(finishReason)){
                    ChatMessage message = choice.getMessage();
                    List<ToolCall> toolCalls = message.getToolCalls();

                    List<ChatMessage> messages = new ArrayList<>(moonshotChatCompletion.getMessages());
                    messages.add(message);

                    // 添加 tool 消息
                    for (ToolCall toolCall : toolCalls) {
                        String functionName = toolCall.getFunction().getName();
                        String arguments = toolCall.getFunction().getArguments();
                        String functionResponse = ToolUtil.invoke(functionName, arguments);

                        messages.add(ChatMessage.withTool(functionResponse, toolCall.getId()));
                    }
                    moonshotChatCompletion.setMessages(messages);

                }else{// 其他情况直接返回

                    // 设置包含tool的总token数
                    moonshotChatCompletionResponse.setUsage(allUsage);
                    //moonshotChatCompletionResponse.setObject("chat.completion");

                    // 恢复原始请求数据
                    chatCompletion.setMessages(moonshotChatCompletion.getMessages());
                    chatCompletion.setTools(moonshotChatCompletion.getTools());

                    return this.convertChatCompletionResponse(moonshotChatCompletionResponse);

                }
            }
            
            if(!execute.isSuccessful()) {
            	log.warn("请求失败：{}", execute.body());
            }
        }
        return null;
    }

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion) throws Exception {
        return this.chatCompletion(null, null, chatCompletion);
    }

    @Override
    public void chatCompletionStream(String baseUrl, String apiKey, ChatCompletion chatCompletion, SseListener eventSourceListener) throws Exception {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = moonshotConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = moonshotConfig.getApiKey();
        chatCompletion.setStream(true);

        // 转换 请求参数
        MoonshotChatCompletion moonshotChatCompletion = this.convertChatCompletionObject(chatCompletion);

        // 如含有function，则添加tool
        if(moonshotChatCompletion.getFunctions()!=null && !moonshotChatCompletion.getFunctions().isEmpty()){
            List<Tool> tools = ToolUtil.getAllFunctionTools(moonshotChatCompletion.getFunctions());
            moonshotChatCompletion.setTools(tools);
        }

        String finishReason = "first";

        while("first".equals(finishReason) || "tool_calls".equals(finishReason)){

            finishReason = null;
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(moonshotChatCompletion);

            Request request = new Request.Builder()
                    .header("Authorization", "Bearer " + apiKey)
                    .url(ValidateUtil.concatUrl(baseUrl, moonshotConfig.getChatCompletionUrl()))
                    .post(RequestBody.create(jsonString, MediaType.parse(Constants.APPLICATION_JSON)))
                    .build();

            log.debug("请求地址：{}", request.toString());
            factory.newEventSource(request, convertEventSource(eventSourceListener));
            eventSourceListener.getCountDownLatch().await();

            finishReason = eventSourceListener.getFinishReason();
            List<ToolCall> toolCalls = eventSourceListener.getToolCalls();

            // 需要调用函数
            if("tool_calls".equals(finishReason) && !toolCalls.isEmpty()){
                // 创建tool响应消息
                ChatMessage responseMessage = ChatMessage.withAssistant(eventSourceListener.getToolCalls());

                List<ChatMessage> messages = new ArrayList<>(moonshotChatCompletion.getMessages());
                messages.add(responseMessage);

                // 封装tool结果消息
                for (ToolCall toolCall : toolCalls) {
                    String functionName = toolCall.getFunction().getName();
                    String arguments = toolCall.getFunction().getArguments();
                    if("$web_search".equals(functionName)) {                 
                    	// 这个是Kimi平台的做法
                    	messages.add(ChatMessage.withTool(arguments, toolCall.getId()));
                    } else {
                    	String functionResponse = ToolUtil.invoke(functionName, arguments);
                    	messages.add(ChatMessage.withTool(functionResponse, toolCall.getId()));
                    }
                }
                eventSourceListener.setToolCalls(new ArrayList<>());
                eventSourceListener.setToolCall(null);
                moonshotChatCompletion.setMessages(messages);
            }

        }

        // 补全原始请求
        chatCompletion.setMessages(moonshotChatCompletion.getMessages());
        chatCompletion.setTools(moonshotChatCompletion.getTools());
    }

    @Override
    public void chatCompletionStream(ChatCompletion chatCompletion, SseListener eventSourceListener) throws Exception {
        this.chatCompletionStream(null, null, chatCompletion, eventSourceListener);
    }

	@Override
	public PlatformType platformType() {
		return PlatformType.MOONSHOT;
	}

	@Override
	public ChatCompletion actAsBeginnerHelper(List<ContentDTO> contents) {
		Tool webSearch = Tool.builder()
				.type("builtin_function")
				.function(Function.builder().name("$web_search").build())
				.build();
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.withSystem("你现在是Northstar盈富量化平台的智能助手，主要通过盈富平台官网查询到的信息给用户解答疑问，用户以下全部的问题，大前提都是在Northstar盈富量化平台的语境下。比如，用户问“合约是什么意思”，用户真正的问题应该是“在Northstar盈富量化平台里，合约这个概念是代表什么”。如果官网查找不到相应的描述，可以按照你已有的理解，给出一个参考答案并说明“详情请加入盈富量化社区咨询”"));
		msgs.addAll(contents.stream().map(c -> ChatMessage.builder().role(c.getRole()).content(Content.ofText(c.getContent())).build()).toList());
		return ChatCompletion.builder()
				.model("moonshot-v1-8k")
				.frequencyPenalty(1f)
				.topP(0.1f)
				.messages(msgs)
				.tools(List.of(webSearch))
				.build();
	}

	@Override
	public ChatCompletion actAsContractLookupHelper(List<ContentDTO> contents) {
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.withSystem("你现在是Northstar盈富量化平台的合约查询助手，主要通过调用function_call的queryContract来查询合约信息。通过分析用户的问题，来提取用户究竟想要查询什么合约。当返回的合约数量多于1个时，提醒用户给出更准确的合约名称或合约编码。"));
		msgs.addAll(contents.stream().map(c -> ChatMessage.builder().role(c.getRole()).content(Content.ofText(c.getContent())).build()).toList());
		return ChatCompletion.builder()
				.model("moonshot-v1-8k")
				.frequencyPenalty(1f)
				.topP(0.1f)
				.messages(msgs)
				.functions("queryContract")
				.build();
	}

	@Override
	public ChatCompletion actAsStrategyHelper(List<ContentDTO> contents) {
		File strategyDoc;
		String content;
		try {
			strategyDoc = ResourceUtils.getFile("classpath:copilot-ref/strategy/guide.md");
			content = FileUtils.readFileToString(strategyDoc, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		List<ChatMessage> msgs = new ArrayList<>();
		msgs.add(ChatMessage.withSystem("你现在是Northstar盈富量化平台的策略助手，根据Northstar的策略代码指南，帮助用户完成策略代码的编写工作。"));
		msgs.addAll(contents.stream().map(c -> ChatMessage.builder().role(c.getRole()).content(Content.ofText(c.getContent())).build()).toList());
		msgs.add(ChatMessage.withSystem("结合以下策略指南，为用户编写策略：" + content));
		return ChatCompletion.builder()
				.model("moonshot-v1-32k")
				.frequencyPenalty(1f)
				.maxTokens(32*1024)
				.messages(msgs)
				.build();
	}
	
}
