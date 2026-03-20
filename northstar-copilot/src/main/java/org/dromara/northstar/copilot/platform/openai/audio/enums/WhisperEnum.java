package org.dromara.northstar.copilot.platform.openai.audio.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * @Author cly
 * @Description Whisper枚举类
 * @Date 2024/10/10 23:56
 */
public class WhisperEnum {
    @Getter
    @AllArgsConstructor
    public enum ResponseFormat implements Serializable {
        JSON("json"),
        TEXT("text"),
        SRT("srt"),
        VERBOSE_JSON("verbose_json"),
        VTT("vtt"),
        ;
        private final String value;
    }
}
