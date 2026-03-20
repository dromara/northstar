package org.dromara.northstar.copilot.platform.openai.audio.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * @Author cly
 * @Description 音频audio枚举类
 * @Date 2024/10/10 23:49
 */
public class AudioEnum {
    @Getter
    @AllArgsConstructor
    public enum Voice implements Serializable {
        ALLOY("alloy"),
        ECHO("echo"),
        FABLE("fable"),
        ONYX("onyx"),
        NOVA("nova"),
        SHIMMER("shimmer"),
        ;
        private final String value;
    }

    @Getter
    @AllArgsConstructor
    public enum ResponseFormat implements Serializable {
        MP3("mp3"),
        OPUS("opus"),
        AAC("aac"),
        FLAC("flac"),
        WAV("wav"),
        PCM("pcm"),
        ;
        private final String value;
    }
}
