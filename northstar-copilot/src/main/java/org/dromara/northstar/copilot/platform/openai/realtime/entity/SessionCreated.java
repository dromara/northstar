package org.dromara.northstar.copilot.platform.openai.realtime.entity;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/10/12 17:58
 */
public class SessionCreated {
    private String event_id;
    private String type = "session.created";
    private Session session;
}
