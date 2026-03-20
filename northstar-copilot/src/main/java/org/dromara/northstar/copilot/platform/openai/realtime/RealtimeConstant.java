package org.dromara.northstar.copilot.platform.openai.realtime;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/10/13 13:56
 */
public class RealtimeConstant {
    /**
     * session.update
     * input_audio_buffer.append
     * input_audio_buffer.commit
     * input_audio_buffer.clear
     * conversation.item.create
     * conversation.item.truncate
     * conversation.item.delete
     * response.create
     * response.cancel
     */
    public static class ClientEvent {
        public static final String SESSION_UPDATE = "session.update";
        public static final String INPUT_AUDIO_BUFFER_APPEND = "input_audio_buffer.append";
        public static final String INPUT_AUDIO_BUFFER_COMMIT = "input_audio_buffer.commit";
        public static final String INPUT_AUDIO_BUFFER_CLEAR = "input_audio_buffer.clear";
        public static final String CONVERSATION_ITEM_CREATE = "conversation.item.create";
        public static final String CONVERSATION_ITEM_TRUNCATE = "conversation.item.truncate";
        public static final String CONVERSATION_ITEM_DELETE = "conversation.item.delete";
        /**
         * 发送此事件可触发响应生成。
         */
        public static final String RESPONSE_CREATE = "response.create";
        public static final String RESPONSE_CANCEL = "response.cancel";
    }

    /**
     * error
     * session.created
     * session.updated
     * conversation.created
     * input_audio_buffer.committed
     * input_audio_buffer.cleared
     * input_audio_buffer.speech_started
     * input_audio_buffer.speech_stopped
     * conversation.item.created
     * conversation.item.input_audio_transcription.completed
     * conversation.item.input_audio_transcription.failed
     * conversation.item.truncated
     * conversation.item.deleted
     * response.created
     * response.done
     * response.output_item.added
     * response.output_item.done
     * response.content_part.added
     * response.content_part.done
     * response.text.delta
     * response.text.done
     * response.audio_transcript.delta
     * response.audio_transcript.done
     * response.audio.delta
     * response.audio.done
     * response.function_call_arguments.delta
     * response.function_call_arguments.done
     * rate_limits.updated
     */
    public static class ServerEvent {
        public static final String ERROR = "error";
        public static final String SESSION_CREATED = "session.created";
        public static final String SESSION_UPDATED = "session.updated";
        /**
         * 创建对话时返回。会话创建后立即发出。
         */
        public static final String CONVERSATION_CREATED = "conversation.created";
        public static final String INPUT_AUDIO_BUFFER_COMMITTED = "input_audio_buffer.committed";
        public static final String INPUT_AUDIO_BUFFER_CLEARED = "input_audio_buffer.cleared";
        public static final String INPUT_AUDIO_BUFFER_SPEECH_STARTED = "input_audio_buffer.speech_started";
        public static final String INPUT_AUDIO_BUFFER_SPEECH_STOPPED = "input_audio_buffer.speech_stopped";
        /**
         * 创建对话项目时返回。
         */
        public static final String CONVERSATION_ITEM_CREATED = "conversation.item.created";
        public static final String CONVERSATION_ITEM_INPUT_AUDIO_TRANSCRIPTION_COMPLETED = "conversation.item.input_audio_transcription.completed";
        public static final String CONVERSATION_ITEM_INPUT_AUDIO_TRANSCRIPTION_FAILED = "conversation.item.input_audio_transcription.failed";
        public static final String CONVERSATION_ITEM_TRUNCATED = "conversation.item.truncated";
        public static final String CONVERSATION_ITEM_DELETED = "conversation.item.deleted";
        public static final String RESPONSE_CREATED = "response.created";
        /**
         * 当响应完成流式传输时返回。无论最终状态如何，始终发出。
         */
        public static final String RESPONSE_DONE = "response.done";
        public static final String RESPONSE_OUTPUT_ITEM_ADDED = "response.output_item.added";
        public static final String RESPONSE_OUTPUT_ITEM_DONE = "response.output_item.done";
        /**
         * 在生成回复过程中将新内容添加到助理信息项目时返回。
         */
        public static final String RESPONSE_CONTENT_PART_ADDED = "response.content_part.added";
        /**
         * 当助手信息项目中的内容部分完成流式传输时返回。当响应中断、不完整或取消时也会返回。
         */
        public static final String RESPONSE_CONTENT_PART_DONE = "response.content_part.done";
        /**
         * 当“文本”内容部分的文本值更新时返回。
         */
        public static final String RESPONSE_TEXT_DELTA = "response.text.delta";
        /**
         * 当 “文本 ”内容部分的文本值完成流式传输时返回。当响应被中断、不完整或取消时也会返回。
         */
        public static final String RESPONSE_TEXT_DONE = "response.text.done";
        public static final String RESPONSE_AUDIO_TRANSCRIPT_DELTA = "response.audio_transcript.delta";
        public static final String RESPONSE_AUDIO_TRANSCRIPT_DONE = "response.audio_transcript.done";
        /**
         * 当模型生成的音频更新时返回。
         */
        public static final String RESPONSE_AUDIO_DELTA = "response.audio.delta";
        /**
         * 当模型生成的音频完成时返回。当响应被中断、不完整或取消时也会发出。
         */
        public static final String RESPONSE_AUDIO_DONE = "response.audio.done";
        public static final String RESPONSE_FUNCTION_CALL_ARGUMENTS_DELTA = "response.function_call_arguments.delta";
        public static final String RESPONSE_FUNCTION_CALL_ARGUMENTS_DONE = "response.function_call_arguments.done";
        public static final String RATE_LIMITS_UPDATED = "rate_limits.updated";
    }
}
