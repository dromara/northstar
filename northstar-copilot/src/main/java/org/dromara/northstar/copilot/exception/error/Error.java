package org.dromara.northstar.copilot.exception.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description 基础错误实体
 * @Date 2024/9/18 23:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Error {
    private String message;
    private String type;
    private String param;
    private String code;
}
