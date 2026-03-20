package org.dromara.northstar.copilot.exception.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description 腾讯混元错误实体
 *
 * {"Response":{"RequestId":"e4650694-f018-4490-b4d0-d5242cd68106","Error":{"Code":"InvalidParameterValue.Model","Message":"模型不存在"}}}
 *
 * @Date 2024/9/18 21:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HunyuanError {
    private Response Response;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Response{
        private Error Error;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public class Error{
            private String Code;
            private String Message;
        }
    }
}
