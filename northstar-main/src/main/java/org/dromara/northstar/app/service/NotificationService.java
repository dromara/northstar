package org.dromara.northstar.app.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.dromara.northstar.common.MessageSenderFactory;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.NotificationSettingsDescription;
import org.dromara.northstar.common.utils.MessageSenderUtils;
import org.dromara.northstar.data.jdbc.NotificationSettingsRepository;
import org.dromara.northstar.data.jdbc.entity.NotificationSettingsDO;
import org.dromara.northstar.event.EventNotificationHandler;
import org.dromara.northstar.meta.MsgSenderMetaProvider;
import org.dromara.northstar.strategy.IMessageSender;
import org.dromara.northstar.support.msg.MessageSenderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(1)
@Service
public class NotificationService implements CommandLineRunner{

	private static final String SETTINGS_ID = "NS_SETTINGS";
	
	@Autowired
	private NotificationSettingsRepository repo;
	
	@Autowired
	private MsgSenderMetaProvider mmPvd;
	
	@Autowired
	private MessageSenderManager senderMgr;
	
	@Autowired
	private EventNotificationHandler notificationHandler;
	
	public NotificationSettingsDescription loadSettings() {
		AtomicReference<NotificationSettingsDescription> nsd = new AtomicReference<>();
		repo.findById(SETTINGS_ID).ifPresent(ndo -> nsd.set(JSON.parseObject(ndo.getSettingsData(), NotificationSettingsDescription.class)));
		if(nsd.get() == null) {
			return defaultSettings();
		}
		NotificationSettingsDescription settings = nsd.get();
		Iterator<Entry<String, Collection<ComponentField>>> itEntry = settings.getSettingsMap().entrySet().iterator();
		while(itEntry.hasNext()) {
			Entry<String, Collection<ComponentField>> e = itEntry.next();
			if(mmPvd.getMessageSenderFactory(e.getKey()) == null) {
				itEntry.remove();
			}
		}
		return settings;
	}
	
	public void saveSettings(NotificationSettingsDescription settings) {
		log.info("保存消息配置");
		NotificationSettingsDO ndo = new NotificationSettingsDO(SETTINGS_ID, JSON.toJSONString(settings));
		repo.save(ndo);
		
		settings.getSettingsMap().entrySet().forEach(e -> {
			String name = e.getKey();
			Identifier id = Identifier.of(name);
			if(senderMgr.contains(id)) {
				senderMgr.get(id).setMsgSenderSettings(e.getValue());
			} else {
				IMessageSender sender = mmPvd.getMessageSenderFactory(name).newInstance(settings);
				senderMgr.add(id, sender);
			}
		});
	}
	
	public void testSettings(NotificationSettingsDescription settings) {
		saveSettings(settings);
		log.info("发送测试消息");
		senderMgr.findAll().stream().forEach(sender -> sender.asyncSend("报警测试", LocalDateTime.now().toString()));
	}
	
	private NotificationSettingsDescription defaultSettings() {
		NotificationSettingsDescription settings = new NotificationSettingsDescription();
		settings.setSubscribedEvents(notificationHandler.subcribedEvents().stream().toList());
		settings.setSettingsMap(mmPvd.msgSenderSettingsMeta());
		return settings;
	}
	
	public NotificationSettingsDescription resetSettings() {
		log.info("重置消息配置");
		repo.deleteAll();
		return defaultSettings();
	}

	@Override
	public void run(String... args) throws Exception {
		repo.findById(SETTINGS_ID).ifPresent(ndo -> {
			NotificationSettingsDescription settings = JSON.parseObject(ndo.getSettingsData(), NotificationSettingsDescription.class);
			settings.getSettingsMap().entrySet().forEach(e ->{
				String name = e.getKey();
				MessageSenderFactory factory = mmPvd.getMessageSenderFactory(name);
				if(factory == null) {
					return;
				}
				log.info("初始化【{}】消息报警实例", name);
				IMessageSender sender = factory.newInstance(settings);
				senderMgr.add(Identifier.of(name), sender);
			});
		});
		
		MessageSenderUtils.setSenderManager(senderMgr);
	}

	
}
