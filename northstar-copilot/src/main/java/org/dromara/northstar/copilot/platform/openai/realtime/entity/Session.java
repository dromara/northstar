package org.dromara.northstar.copilot.platform.openai.realtime.entity;

import java.util.List;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/10/12 17:24
 */
public class Session {
    /**
     * 模型可以响应的一组模式。要禁用音频，请将其设置为 [“text”]。
     */
    private List<String> modalities;

    /**
     * 默认系统指令添加到模型调用之前。
     */
    private String instructions;

    /**
     * 模型用于响应的声音 - alloy 、 echo或shimmer之一。一旦模型至少响应一次音频，就无法更改。
     */
    private String voice;

    /**
     * 输入音频的格式。选项为“pcm16”、“g711_ulaw”或“g711_alaw”。
     */
    private String input_audio_format;

    /**
     * 输出音频的格式。选项为“pcm16”、“g711_ulaw”或“g711_alaw”。
     */
    private String output_audio_format;


    /**
     * 输入音频转录的配置。可以设置为null来关闭。
     */
    private Object input_audio_transcription;


    /**
     * 转弯检测的配置。可以设置为null来关闭。
     */
    private Object turn_detection;


    private Object tools;

    private String tool_choice;
    private Double temperature;
    private Integer max_output_tokens;


}
