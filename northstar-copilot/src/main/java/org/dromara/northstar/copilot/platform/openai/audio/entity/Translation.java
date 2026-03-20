package org.dromara.northstar.copilot.platform.openai.audio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

import java.io.File;

import org.dromara.northstar.copilot.platform.openai.audio.enums.WhisperEnum;

/**
 * 转录请求参数。
 * 可将音频文件转录为你所输入语言对应文本。语言可以自己指定
 *
 * @author cly
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Translation {

    /**
     * 要转录的音频文件对象（不是文件名），采用以下格式之一：flac、mp3、mp4、mpeg、mpga、m4a、ogg、wav 或 webm。
     */
    @NonNull
    private File file;

    /**
     * 要使用的模型的 ID。目前只有 whisper-1 可用。
     */
    @NonNull
    @Builder.Default
    private String model = "whisper-1";

    /**
     * 一个可选文本，用于指导模型的样式或继续上一个音频片段。提示应与音频语言匹配。
     */
    private String prompt;

    /**
     * 输出的格式，采用以下选项之一：json、text、srt、verbose_json 或 vtt。
     */
    @JsonProperty("response_format")
    @Builder.Default
    private String responseFormat = WhisperEnum.ResponseFormat.JSON.getValue();

    /**
     * 采样温度，介于 0 和 1 之间。较高的值（如 0.8）将使输出更加随机，而较低的值（如 0.2）将使其更具集中性和确定性。如果设置为 0，模型将使用对数概率自动提高温度，直到达到某些阈值。
     */
    @Builder.Default
    private Double temperature = 0d;


    public static class TranslationBuilder {
        private File file;

        public Translation.TranslationBuilder content(File file){
            // 校验File是否为以下格式之一：flac、mp3、mp4、mpeg、mpga、m4a、ogg、wav 或 webm。
            if (file == null) {
                throw new IllegalArgumentException("file is required");
            }

            String[] allowedFormats = {"flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm"};
            String fileName = file.getName().toLowerCase();
            boolean isValidFormat = false;

            for (String format : allowedFormats) {
                if (fileName.endsWith("." + format)) {
                    isValidFormat = true;
                    break;
                }
            }

            if (!isValidFormat) {
                throw new IllegalArgumentException("Invalid file format. Allowed formats are: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm.");
            }

            this.file = file;
            return this;
        }
    }

    public void setFile(@NonNull File file) {
        // 校验File是否为以下格式之一：flac、mp3、mp4、mpeg、mpga、m4a、ogg、wav 或 webm。
        if (file == null) {
            throw new IllegalArgumentException("file is required");
        }

        String[] allowedFormats = {"flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm"};
        String fileName = file.getName().toLowerCase();
        boolean isValidFormat = false;

        for (String format : allowedFormats) {
            if (fileName.endsWith("." + format)) {
                isValidFormat = true;
                break;
            }
        }

        if (!isValidFormat) {
            throw new IllegalArgumentException("Invalid file format. Allowed formats are: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm.");
        }

        this.file = file;
    }
}
