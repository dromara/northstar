package org.dromara.northstar.data.jdbc;

import java.util.List;
import java.util.Optional;

import org.dromara.northstar.common.MessageSenderSettings;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.data.IMessageSenderRepository;
import org.dromara.northstar.data.jdbc.entity.MessageSenderSettingsDO;
import org.dromara.northstar.data.jdbc.entity.SubscriptionEventsDO;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.JSON;


public class MessageSenderSettingsRepoAdapter implements IMessageSenderRepository{
	
	private MessageSenderSettingsRepository delegate;
	
	private NotificationEventRepository notificationRepo;
	
	public MessageSenderSettingsRepoAdapter(MessageSenderSettingsRepository delegate, NotificationEventRepository notificationRepo) {
		this.delegate = delegate;
		this.notificationRepo = notificationRepo;
	}

	@Override
	public void save(MessageSenderSettings settings) {
		String clzName = settings.getClass().getName();
		MessageSenderSettingsDO settingsDO = new MessageSenderSettingsDO();
		settingsDO.setClassName(clzName);
		settingsDO.setSettingsData(JSON.toJSONString(settings));
		delegate.save(settingsDO);
	}

	@Override
	public MessageSenderSettings get(Class<?> settingsClz) {
		String clzName = settingsClz.getName();
		Optional<MessageSenderSettingsDO> settingsDO = delegate.findById(clzName);
		if(settingsDO.isEmpty()) {
			return null;
		}
		try {
			Class<?> clz = Class.forName(clzName);
			MessageSenderSettings settingsSrc = (MessageSenderSettings) JSON.parseObject(settingsDO.get().getSettingsData(), clz);
			MessageSenderSettings settings = (MessageSenderSettings) clz.getDeclaredConstructor().newInstance();
			BeanUtils.copyProperties(settingsSrc, settings);
			return settings;
		} catch (Exception e) {
			throw new IllegalStateException("", e);
		}
	}

	@Override
	public void save(List<NorthstarEventType> subEvents) {
		SubscriptionEventsDO sub = new SubscriptionEventsDO();
		sub.setSubEvents(JSON.toJSONString(subEvents));
		notificationRepo.save(sub);
	}

	@Override
	public List<NorthstarEventType> getSubEvents() {
		Optional<SubscriptionEventsDO> sub = notificationRepo.findById(SubscriptionEventsDO.FIXED_ID);
		if(sub.isEmpty()) {
			return null; // 故意与空列表区分开
		}
		String listStr = sub.get().getSubEvents();
		return JSON.parseObject(listStr, new TypeReference<List<NorthstarEventType>>() {});
	}

}
