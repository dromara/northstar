package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import org.dromara.northstar.common.MessageSenderSettings;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.data.IMessageSenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@DataJpaTest
class MessageSenderSettingsRepoAdapterTest {

	@Autowired
	private MessageSenderSettingsRepository delegeate;
	
	@Autowired
	private NotificationEventRepository notificationRepo;
	
	private IMessageSenderRepository repo;
	
	@BeforeEach
	void prepare() {
		repo = new MessageSenderSettingsRepoAdapter(delegeate, notificationRepo);
	}
	
	@Test
	void test() {
		EmailConfiguration cfg = new EmailConfiguration();
		cfg.smtpHost = "127.0.0.1";
		cfg.emailAddress = "email@northstar.tech";
		cfg.mailTo = List.of("someone","sometwo");
		cfg.subEvents = List.of(NorthstarEventType.ACCOUNT);
		assertDoesNotThrow(() -> {
			repo.save(cfg);
		});
		
		EmailConfiguration settings = (EmailConfiguration) repo.get(EmailConfiguration.class);
		
		assertThat(cfg.smtpHost).isEqualTo(settings.smtpHost);
		assertThat(cfg.emailAddress).isEqualTo(settings.emailAddress);
		assertThat(cfg.mailTo).hasSameSizeAs(settings.mailTo);
		assertThat(cfg.subEvents).hasSameSizeAs(settings.subEvents);
	}
	
	@Test
	void test2() {
		assertDoesNotThrow(() -> {
			repo.save(List.of(NorthstarEventType.NOTICE, NorthstarEventType.LOGGED_IN));
		});
		
		List<NorthstarEventType> result = repo.getSubEvents();
		assertThat(result).hasSize(2);
	}
	
	@Getter
	@Setter
	@EqualsAndHashCode(callSuper = false)
	public static class EmailConfiguration extends DynamicParams implements MessageSenderSettings{

		/**
		 * SMTP服务器地址
		 */
		@Setting(label = "SMTP主机", order = 10)
		private String smtpHost;
		/**
		 * 邮箱地址
		 */
		@Setting(label = "邮箱地址", order = 20)
		private String emailAddress;
		/**
		 * 邮箱授权码
		 */
		@Setting(label = "邮箱授权码", order = 30)
		private String authCode;
		/**
		 * 订阅人邮箱列表
		 */
		@Setting(label = "订阅人邮箱", order = 40)
		private List<String> mailTo;
		/**
		 * 订阅事件列表
		 */
		@Setting(label = "订阅事件", order = 50)
		private List<NorthstarEventType> subEvents;
	}
}
