package org.dromara.northstar.main.mail;

import java.util.Properties;

import org.dromara.northstar.common.model.MailConfigDescription;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class MailSenderFactory {

	public JavaMailSender newInstance(MailConfigDescription emailConfig) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(emailConfig.getEmailSMTPHost());
		sender.setUsername(emailConfig.getEmailUsername());
		sender.setPassword(emailConfig.getEmailPassword());
		sender.setProtocol("smtp");
		sender.setPort(465);
		sender.setDefaultEncoding("UTF-8");
		// 获取系统属性
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.auth", "true");
		properties.setProperty("mail.smtp.starttls.enable", "true");
		properties.setProperty("mail.smtp.starttls.required", "true");
		properties.setProperty("mail.smtp.ssl.enable", "true");
		sender.setJavaMailProperties(properties);
		
		return sender;
	}
}
