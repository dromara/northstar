package tech.quantit.northstar.main.mail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.IMailMessageContentHandler;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.MailConfigDescription;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
public class MailDeliveryManager {
	
	private MailConfigDescription emailConfig;
	
	private JavaMailSender sender;
	
	private MailSenderFactory factory;
	
	private IMailMessageContentHandler contentHandler;
	
	private EnumSet<NorthstarEventType> interestedEvents = EnumSet.noneOf(NorthstarEventType.class);
	
	private static final String UTF8 = "UTF-8";
	
	private static final long FRESH_TIMEOUT = 10000;
	
	private boolean enabled;
	
	private Executor exec = Executors.newSingleThreadExecutor(); //专门用于发送邮件，理论上不应该有很多任务，所以单个线程足够
	
	public MailDeliveryManager(MailSenderFactory factory, IMailMessageContentHandler contentHandler) {
		this.factory = factory;
		this.contentHandler = contentHandler;
	}
	
	public void setEmailConfig(MailConfigDescription emailConfig) {
		this.emailConfig = emailConfig;
		this.enabled = !emailConfig.isDisabled();
		this.sender = factory.newInstance(emailConfig);
		emailConfig.getInterestTopicList().stream().forEach(interestedEvents::add);
	}
	
	public void onEvent(NorthstarEvent event) {
		if(!enabled
				|| !interestedEvents.contains(event.getEvent())
				|| event.getData() instanceof TradeField trade && !isFreshTrade(trade)
				|| event.getData() instanceof OrderField order && (!isFreshOrder(order) || order.getOrderStatus() == OrderStatusEnum.OS_AllTraded)) {
			return;
		}
		
		String content = switch(event.getEvent()) {
		case CONNECTED -> contentHandler.onConnected((String) event.getData());
		case DISCONNECTED -> contentHandler.onDisconnected((String) event.getData());
		case TRADE -> contentHandler.onEvent((TradeField) event.getData());
		case ORDER -> contentHandler.onEvent((OrderField) event.getData());
		case NOTICE -> contentHandler.onEvent((TradeField) event.getData());
		default -> throw new IllegalArgumentException("Unexpected value: " + event.getEvent());
		};
		exec.execute(() -> {
			MimeMessageHelper msg = new MimeMessageHelper(sender.createMimeMessage(), UTF8);
			try {
				msg.setSubject("Northstar邮件提示");
				msg.setFrom(emailConfig.getEmailUsername());
				for(String mailTo : emailConfig.getSubscriberList()) {
					msg.addTo(mailTo);
				}
				msg.setText(content);
				sender.send(msg.getMimeMessage());
			} catch (MessagingException e) {
				log.error("邮件发送异常", e);
			}
		});
	}
	
	// 判断成交是否为新成交
	private boolean isFreshTrade(TradeField trade) {
		return System.currentTimeMillis() - trade.getTradeTimestamp() < FRESH_TIMEOUT;
	}
	
	// 判断订单是否为新订单
	private boolean isFreshOrder(OrderField order) {
		LocalDate date = LocalDate.parse(order.getOrderDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		LocalTime time = LocalTime.parse(order.getOrderTime(), DateTimeConstant.T_FORMAT_FORMATTER);
		LocalDateTime ldt = LocalDateTime.of(date, time);
		return System.currentTimeMillis() - ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli() < FRESH_TIMEOUT;
	}
}
