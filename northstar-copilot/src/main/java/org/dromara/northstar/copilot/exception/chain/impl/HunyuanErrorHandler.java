package org.dromara.northstar.copilot.exception.chain.impl;

import org.apache.commons.lang3.ObjectUtils;
import org.dromara.northstar.copilot.exception.CommonException;
import org.dromara.northstar.copilot.exception.chain.AbstractErrorHandler;
import org.dromara.northstar.copilot.exception.error.Error;
import org.dromara.northstar.copilot.exception.error.HunyuanError;

import com.alibaba.fastjson2.JSON;

/**
 * @Author cly
 * @Description 混元错误处理
 * @Date 2024/9/18 23:59
 */
public class HunyuanErrorHandler extends AbstractErrorHandler {
    @Override
    public Error parseError(String errorInfo) {
        // 解析json字符串
        try{
            HunyuanError hunyuanError = JSON.parseObject(errorInfo, HunyuanError.class);

            HunyuanError.Response response = hunyuanError.getResponse();

            if(ObjectUtils.isEmpty(response)){
                // 交给下一个节点处理
                return nextHandler.parseError(errorInfo);
            }

            HunyuanError.Response.Error error = response.getError();
            if(ObjectUtils.isEmpty(error)){
                // 交给下一个节点处理
                return nextHandler.parseError(errorInfo);
            }

            return new Error(error.getMessage(),error.getCode(),null,error.getCode());
        }catch (Exception e){
            throw new CommonException(errorInfo);
        }
    }
}
