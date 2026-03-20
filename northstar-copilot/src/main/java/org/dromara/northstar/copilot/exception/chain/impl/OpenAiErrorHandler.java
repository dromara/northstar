package org.dromara.northstar.copilot.exception.chain.impl;

import com.alibaba.fastjson2.JSON;

import org.apache.commons.lang3.ObjectUtils;
import org.dromara.northstar.copilot.exception.CommonException;
import org.dromara.northstar.copilot.exception.chain.AbstractErrorHandler;
import org.dromara.northstar.copilot.exception.error.Error;
import org.dromara.northstar.copilot.exception.error.OpenAiError;

/**
 * @Author cly
 * @Description OpenAi错误处理
 *
 * [openai, zhipu, deepseek, lingyi, moonshot] 错误返回类似，这里共用一个处理类
 *
 * @Date 2024/9/18 21:01
 */
public class OpenAiErrorHandler extends AbstractErrorHandler {

    @Override
    public Error parseError(String errorInfo) {
        // 解析json字符串
        try{
            OpenAiError openAiError = JSON.parseObject(errorInfo, OpenAiError.class);

            Error error = openAiError.getError();
            if(ObjectUtils.isEmpty(error)){
                // 交给下一个节点处理
                return nextHandler.parseError(errorInfo);
            }
            return error;
        }catch (Exception e){
            throw new CommonException(errorInfo);
        }

    }
}
