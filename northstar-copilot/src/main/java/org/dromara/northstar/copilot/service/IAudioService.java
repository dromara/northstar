package org.dromara.northstar.copilot.service;

import java.io.InputStream;

import org.dromara.northstar.copilot.platform.openai.audio.entity.*;

/**
 * @Author cly
 * @Description 音频audio接口服务
 * @Date 2024/10/10 23:39
 */
public interface IAudioService {

    InputStream textToSpeech(String baseUrl, String apiKey, TextToSpeech textToSpeech);
    InputStream textToSpeech(TextToSpeech textToSpeech);

    TranscriptionResponse transcription(String baseUrl, String apiKey, Transcription transcription);
    TranscriptionResponse transcription(Transcription transcription);

    TranslationResponse translation(String baseUrl, String apiKey, Translation translation);
    TranslationResponse translation(Translation translation);
}
