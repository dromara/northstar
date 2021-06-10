package tech.xuanwu.northstar.plugin.mail;

import java.time.LocalDateTime;

import lombok.Data;
import xyz.redtorch.pb.CoreField.NoticeField;

@Data
public class Message {
	
	private LocalDateTime dateTime;
	
	private String title;
	
	private String content;

	public Message(String title, String content) {
		this.title = title;
		this.content = content;
		dateTime = LocalDateTime.now();
	}
	
	public Message(NoticeField notice) {
		dateTime = LocalDateTime.now();
		content = notice.getContent();
		title = String.format("Northstar消息通知 - [%s]", notice.getStatus().toString());
	}
	
}
