package tech.quantit.northstar.main.notification;

import java.time.ZoneOffset;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.IMailSender;
import tech.quantit.northstar.common.model.Message;

@Slf4j
@Component
public class NorthstarMailSender implements IMailSender{
	
	@Autowired
	private JavaMailSender sender; 
	
	@Value("${spring.mail.username}")
	private String senderMail;
	
	@Override
	public void send(Message message) {
		if(StringUtils.isEmpty(senderMail)) {
			throw new IllegalStateException("未设置发件人邮箱");
		}
		
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(senderMail);
		mail.setSentDate(new Date(message.getDateTime().toEpochSecond(ZoneOffset.of("+8"))));
		mail.setTo(message.getReceivers());
		mail.setSubject(message.getTitle());
		mail.setText(message.getContent());
		mail.setSentDate(new Date());
		
		try {			
			sender.send(mail);
		} catch(Exception e) {
			log.warn("邮件发送异常：{} -> {}", message.getTitle(), message.getContent());
		}
	}

}
