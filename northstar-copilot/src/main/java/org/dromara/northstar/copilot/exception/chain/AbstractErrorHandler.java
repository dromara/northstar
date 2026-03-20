package org.dromara.northstar.copilot.exception.chain;

import org.dromara.northstar.copilot.exception.error.Error;

/**
 * @Author cly
 * @Description 错误处理抽象
 * @Date 2024/9/18 20:57
 */
public abstract class AbstractErrorHandler implements IErrorHandler{
    protected IErrorHandler nextHandler;

    @Override
    public void setNext(IErrorHandler handler) {
        this.nextHandler = handler;
    }

    protected Error handleNext(String errorInfo) {
        if (nextHandler != null) {
            return nextHandler.parseError(errorInfo);
        }
        return null;
    }

}
