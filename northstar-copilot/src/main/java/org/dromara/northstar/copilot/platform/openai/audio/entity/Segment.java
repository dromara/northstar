package org.dromara.northstar.copilot.platform.openai.audio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author cly
 * @Description Segment块
 * @Date 2024/10/11 11:34
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Segment {
        private Integer id;
        private Integer seek;
        private Double start;
        private Double end;
        private String text;
        private List<Integer> tokens;
        private Float temperature;
        @JsonProperty("avg_logprob")
        private Double avgLogprob;
        @JsonProperty("compression_ratio")
        private Double compressionRatio;
        @JsonProperty("no_speech_prob")
        private Double noSpeechProb;
}
