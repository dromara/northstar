package org.dromara.northstar.copilot.platform.openai.audio;

import com.alibaba.fastjson2.JSON;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.copilot.config.OpenAiConfig;
import org.dromara.northstar.copilot.constant.Constants;
import org.dromara.northstar.copilot.platform.openai.audio.entity.*;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.IAudioService;
import org.dromara.northstar.copilot.utils.ValidateUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author cly
 * @Description OpenAi音频服务
 * @Date 2024/10/10 23:36
 */
public class OpenAiAudioService implements IAudioService {
    private final OpenAiConfig openAiConfig;
    private final OkHttpClient okHttpClient;

    public OpenAiAudioService(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
    }


    @Override
    public InputStream textToSpeech(String baseUrl, String apiKey, TextToSpeech textToSpeech) {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();

        String requestString = JSON.toJSONString(textToSpeech);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getSpeechUrl()))
                .post(RequestBody.create(MediaType.parse("application/json"), requestString))
                .build();

        // 发送请求并获取响应
        try (Response response = okHttpClient.newCall(request).execute()) {
            // 检查响应是否成功
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 获取响应体
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                return responseBody.byteStream();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public InputStream textToSpeech(TextToSpeech textToSpeech) {
        return this.textToSpeech(null, null, textToSpeech);
    }

    @Override
    public TranscriptionResponse transcription(String baseUrl, String apiKey, Transcription transcription) {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();


        // 创建请求体
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", transcription.getFile().getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), transcription.getFile()))
                .addFormDataPart("model", transcription.getModel())
                .addFormDataPart("temperature", String.valueOf(transcription.getTemperature()));
        if(StringUtils.isNotBlank(transcription.getLanguage())){
            builder.addFormDataPart("language", transcription.getLanguage());
        }
        if(StringUtils.isNotBlank(transcription.getPrompt())){
            builder.addFormDataPart("prompt", transcription.getPrompt());
        }
        if(StringUtils.isNotBlank(transcription.getResponseFormat())){
            builder.addFormDataPart("response_format", transcription.getResponseFormat());
        }

        MultipartBody multipartBody = builder.build();


        // 创建请求
        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getTranscriptionUrl()))
                .post(multipartBody)
                .build();

        // 发送请求并获取响应
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String res = response.body().string();
                TranscriptionResponse transcriptionResponse = JSON.parseObject(res, TranscriptionResponse.class);
                return transcriptionResponse;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public TranscriptionResponse transcription(Transcription transcription) {
        return this.transcription(null, null, transcription);
    }

    @Override
    public TranslationResponse translation(String baseUrl, String apiKey, Translation translation) {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();

        // 创建请求体
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", translation.getFile().getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), translation.getFile()))
                .addFormDataPart("model", translation.getModel())
                .addFormDataPart("temperature", String.valueOf(translation.getTemperature()));
        if(StringUtils.isNotBlank(translation.getPrompt())){
            builder.addFormDataPart("prompt", translation.getPrompt());
        }
        if(StringUtils.isNotBlank(translation.getResponseFormat())){
            builder.addFormDataPart("response_format", translation.getResponseFormat());
        }

        MultipartBody multipartBody = builder.build();

        // 创建请求
        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getTranslationUrl()))
                .post(multipartBody)
                .build();

        // 发送请求并获取响应
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String res = response.body().string();
                TranslationResponse translationResponse = JSON.parseObject(res, TranslationResponse.class);
                return translationResponse;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public TranslationResponse translation(Translation translation) {
        return this.translation(null, null, translation);
    }
}
