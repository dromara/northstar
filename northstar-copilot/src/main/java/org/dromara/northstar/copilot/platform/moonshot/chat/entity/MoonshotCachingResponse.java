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
public class MoonshotCachingResponse {

	/**
	 * 缓存 id，使用这个 id 执行对缓存的 Modify、Retrieve 操作，或是在 /v1/chat/completions 接口中携带这个 id 以应用缓存
	 */
	private String id;
	
	/**
	 * 当前缓存的状态，其遵循以下规则：
	 * 1. 当缓存被初次创建时，其初始状态为 pending；
	 * 2. 如果参数合法，缓存创建成功，其状态变更为 ready；
	 * 3. 如果参数不合法，或因其他原因缓存创建失败，其状态变更为 error；
	 * 4. 对于已过期的缓存，其状态变更为 inactive；
	 * 5. 更新一个存在 id 的缓存，其状态会重新回到 pending，并应用上述步骤 2、3、4；
	 */
	private String status;
	
	/**
	 * 当前缓存的存储类型，为固定值 context-cache
	 */
	private String object;
	
	@JsonProperty("created_at")
	private Integer createdAt;
	
	/**
	 * 当缓存创建失败，即 status 字段为“error”时，会额外携带一个 error 字段，用于表示缓存创建失败的具体失败原因。其具体格式为：{ "type": "error_type", "message": "error_message"}
	 */
	private Map<String, String> error;
	
	/**
	 * 当前已缓存的 Tokens 数量。
	 */
	private Integer tokens;
	
	private String model;

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
