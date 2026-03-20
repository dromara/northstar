package org.dromara.northstar.copilot.platform.moonshot.chat.entity;

import java.util.List;
import java.util.Map;

import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;
import org.dromara.northstar.copilot.platform.openai.tool.Tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoonshotCaching {

	@Builder.Default
	private String model = "moonshot-v1";

	/**
	 * 缓存的消息内容，其格式与 /v1/chat/completions 接口中的 messages 一致
	 */
	@NonNull
	private List<ChatMessage> messages;
	
	/**
     * 缓存的工具内容，其格式与 /v1/chat/completions 接口中的 tools 一致
     */
    private List<Tool> tools;

    /**
     * 缓存名称，这是一个辅助性质的字段，可以使用与你业务相关的信息来设置本次缓存的名称
     */
	private String name;
	
	/**
	 * 缓存描述信息，这是一个辅助性质的字段，可以使用与你业务相关的信息来设置本次缓存的描述信息，在检索缓存时，你可以通过 description 字段来判断这个缓存是否是你需要的缓存
	 */
	private String description;

	/**
	 * 缓存的元信息，你可以将与你业务相关的各种信息以 key-value 的形式存储在 metadata 中，你最多可以设置 16 组元信息，每组元信息的 key 长度最高不超过 64 个 utf-8 字符，每组元信息的 value 长度最高不超过 512 个 utf-8 字符
	 */
	private Map<String, String> metadata;
	
	/**
	 * 缓存的有效期，单位为秒
	 */
	private Integer ttl;
	
	/**
	 * 缓存的过期时间，格式为 unix 时间戳（单位为秒），是指缓存过期的某个具体时间点
	 */
	@JsonProperty("expired_at")
	private Integer expiredAt;
}
