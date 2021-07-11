package tech.xuanwu.northstar.plugin.mail;

import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MailMessageSender implements MessageSender{
	
	@Autowired
	private JavaMailSender mailSender; 
	
	@Value("${spring.mail.subscribed}")
	private String subscriber;
	
	@Value("${spring.mail.username}")
	private String senderMail;

	@Override
	public void send(Message message) {
		
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(senderMail);
		mail.setSentDate(new Date(message.getDateTime().toEpochSecond(ZoneOffset.of("+8"))));
		mail.setTo(subscriber);
		mail.setSubject(message.getTitle());
		mail.setText(message.getContent());
		
		try {			
			mailSender.send(mail);
		} catch(Exception e) {
			log.warn("邮件发送异常：{} -> {}", message.getTitle(), message.getContent());
		}
	}

}
