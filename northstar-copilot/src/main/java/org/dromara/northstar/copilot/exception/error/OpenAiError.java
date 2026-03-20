package org.dromara.northstar.copilot.exception.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description OpenAi错误返回实体类
 *
 * {
 *     "error": {
 *         "message": "Incorrect API key provided: sk-proj-*************************************************************************************************************************8YA1. You can find your API key at https://platform.openai.com/account/api-keys.",
 *         "type": "invalid_request_error",
 *         "param": null,
 *         "code": "invalid_api_key"
 *     }
 * }
 *
 * @Date 2024/9/18 18:44
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiError {
    private Error error;
}
