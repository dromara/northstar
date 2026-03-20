package org.dromara.northstar.copilot.service.factor;

import org.dromara.northstar.copilot.platform.deepseek.chat.DeepSeekChatService;
import org.dromara.northstar.copilot.platform.moonshot.chat.MoonshotChatService;
import org.dromara.northstar.copilot.platform.ollama.chat.OllamaAiChatService;
import org.dromara.northstar.copilot.platform.openai.audio.OpenAiAudioService;
import org.dromara.northstar.copilot.platform.openai.chat.OpenAiChatService;
import org.dromara.northstar.copilot.platform.openai.embedding.OpenAiEmbeddingService;
import org.dromara.northstar.copilot.platform.openai.realtime.OpenAiRealtimeService;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.IAudioService;
import org.dromara.northstar.copilot.service.IChatService;
import org.dromara.northstar.copilot.service.IEmbeddingService;
import org.dromara.northstar.copilot.service.IRealtimeService;
import org.dromara.northstar.copilot.service.PlatformType;
import org.dromara.northstar.copilot.vector.service.PineconeService;
import org.dromara.northstar.copilot.websearch.ChatWithWebSearchEnhance;

/**
 * @Author cly
 * @Description AI服务工厂，创建各种AI应用
 * @Date 2024/8/7 18:10
 */
public class AiService {
   // private final ConcurrentMap<PlatformType, IChatService> chatServiceCache = new ConcurrentHashMap<>();
    //private final ConcurrentMap<PlatformType, IEmbeddingService> embeddingServiceCache = new ConcurrentHashMap<>();

    private final Configuration configuration;

    public AiService(Configuration configuration) {
        this.configuration = configuration;
    }

    public IChatService getChatService(PlatformType platform) {
        //return chatServiceCache.computeIfAbsent(platform, this::createChatService);
        return createChatService(platform);
    }

    public IChatService webSearchEnhance(IChatService chatService) {
        //IChatService chatService = getChatService(platform);
        return new ChatWithWebSearchEnhance(chatService, configuration);
    }

    private IChatService createChatService(PlatformType platform) {
        switch (platform) {
            case OPENAI:
                return new OpenAiChatService(configuration);
            case DEEPSEEK:
                return new DeepSeekChatService(configuration);
            case MOONSHOT:
                return new MoonshotChatService(configuration);
            case OLLAMA:
                return new OllamaAiChatService(configuration);
            default:
                throw new IllegalArgumentException("Unknown platform: " + platform);
        }
    }



    public IEmbeddingService getEmbeddingService(PlatformType platform) {
        //return embeddingServiceCache.computeIfAbsent(platform, this::createEmbeddingService);
        return createEmbeddingService(platform);
    }

    private IEmbeddingService createEmbeddingService(PlatformType platform) {
        switch (platform) {
            case OPENAI:
                return new OpenAiEmbeddingService(configuration);
            default:
                throw new IllegalArgumentException("Unknown platform: " + platform);
        }
    }

    public IAudioService getAudioService(PlatformType platform) {
        return createAudioService(platform);
    }

    private IAudioService createAudioService(PlatformType platform) {
        switch (platform) {
            case OPENAI:
                return new OpenAiAudioService(configuration);
            default:
                throw new IllegalArgumentException("Unknown platform: " + platform);
        }
    }

    public IRealtimeService getRealtimeService(PlatformType platform) {
        return createRealtimeService(platform);
    }

    private IRealtimeService createRealtimeService(PlatformType platform) {
        switch (platform) {
            case OPENAI:
                return new OpenAiRealtimeService(configuration);
            default:
                throw new IllegalArgumentException("Unknown platform: " + platform);
        }
    }

    public PineconeService getPineconeService() {
        return new PineconeService(configuration);
    }

}
