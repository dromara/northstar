package org.dromara.northstar.copilot.utils;


import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.ModelType;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author cly
 */
@Slf4j
public class TikTokensUtil {

    /**
     * 模型名称对应Encoding
     */
    private static final Map<String, Encoding> modelMap = new HashMap<>();
    /**
     * registry实例
     */
    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();


    static {
        for (ModelType model : ModelType.values()){
            Optional<Encoding> encodingForModel = registry.getEncodingForModel(model.getName());
            encodingForModel.ifPresent(encoding -> modelMap.put(model.getName(), encoding));
        }
    }

    /**
     * 可以简单点用，直接传入list.toString()
     * @param encodingType
     * @param content
     * @return
     */
    public static int tokens(EncodingType encodingType, String content){
        Encoding encoding = registry.getEncoding(encodingType);
        return encoding.countTokens(content);
    }
    public static int tokens(String modelName, String content)  {
        if (StringUtils.isEmpty(content)) {
            return 0;
        }
        Encoding encoding = modelMap.get(modelName);
        return encoding.countTokens(content);
    }
    public static int tokens(String modelName, List<ChatMessage> messages) {
        Encoding encoding = modelMap.get(modelName);
        if (ObjectUtils.isEmpty(encoding)) {
            throw new IllegalArgumentException("不支持计算Token的模型: " + modelName);
        }

        int tokensPerMessage = 0;
        int tokensPerName = 0;
        if (modelName.startsWith("gpt-4")) {
            tokensPerMessage = 3;
            tokensPerName = 1;
        } else if (modelName.startsWith("gpt-3.5")) {
            tokensPerMessage = 4;
            tokensPerName = -1;
        }
        int sum = 0;
        for (ChatMessage message : messages) {

            sum += tokensPerMessage;
            sum += encoding.countTokens(message.getContent().getText());
            sum += encoding.countTokens(message.getRole());
            if(StringUtils.isNotEmpty(message.getName())){
                sum += encoding.countTokens(message.getName());
                sum += tokensPerName;
            }


        }
        sum += 3; // every reply is primed with <|start|>assistant<|message|>
        return sum;
    }

}
