package org.dromara.northstar.copilot.convert.chat;

import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatCompletion;

/**
 * @Author cly
 * @Description 处理请求参数 统一的OpenAi格式--->其它模型格式
 * @Date 2024/8/12 1:04
 */
public interface ParameterConvert<T> {
    T convertChatCompletionObject(ChatCompletion chatCompletion);
}
