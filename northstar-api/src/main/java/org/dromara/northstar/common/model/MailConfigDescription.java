package org.dromara.northstar.common.model;

import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.event.NorthstarEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

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
	 * 禁用邮件通知
	 */
	@Default
	private boolean disabled = true;
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
	@Default
	private List<String> subscriberList = Collections.emptyList();
	/**
	 * 订阅事件列表
	 */
	@Default
	private List<NorthstarEventType> interestTopicList = Collections.emptyList();
}
