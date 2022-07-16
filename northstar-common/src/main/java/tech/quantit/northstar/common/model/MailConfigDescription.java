package tech.quantit.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.event.NorthstarEventType;

/**
 * EMAIL配置信息
 * @author KevinHuangwl
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MailConfigDescription {
	/**
	 * SMTP服务器地址
	 */
	private String emailSMTPHost;
	/**
	 * 邮箱名
	 */
	private String emailUsername;
	/**
	 * 邮箱授权码
	 */
	private String emailPassword;
	/**
	 * 订阅人邮箱列表
	 */
	private List<String> subscriberList;
	/**
	 * 订阅事件列表
	 */
	private List<NorthstarEventType> interestTopicList;
}
