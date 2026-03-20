package org.dromara.northstar.copilot.platform.openai.audio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.io.Serializable;

import org.dromara.northstar.copilot.platform.openai.audio.enums.AudioEnum;

/**
 * @Author cly
 * @Description TextToSpeech请求实体类
 * @Date 2024/10/10 23:45
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextToSpeech {
    /**
     * tts-1 or tts-1-hd
     */
    @Builder.Default
    @NonNull
    private String model = "tts-1";

    /**
     * 要为其生成音频的文本。最大长度为 4096 个字符。
     */
    @NonNull
    private String input;


    /**
     * 生成音频时要使用的语音。支持的声音包括 alloy、echo、fable、onyx、nova 和 shimmer
     */
    @Builder.Default
    @NonNull
    private String voice = AudioEnum.Voice.ALLOY.getValue();

    /**
     * 音频输入的格式。支持的格式包括 mp3, opus, aac, flac, wav, and pcm。
     */
    @Builder.Default
    @JsonProperty("response_format")
    private String responseFormat = AudioEnum.ResponseFormat.MP3.getValue();

    /**
     * 生成的音频的速度。选择一个介于 0.25 到 4.0 之间的值。默认值为 1.0。
     */
    @Builder.Default
    private Double speed = 1.0d;
}