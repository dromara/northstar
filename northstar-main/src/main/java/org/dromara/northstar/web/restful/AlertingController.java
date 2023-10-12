package org.dromara.northstar.web.restful;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.MessageSenderSettings;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.data.IMessageSenderRepository;
import org.dromara.northstar.strategy.IMessageSender;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/northstar/alerting")
@RestController
public class AlertingController {

	private static final String COMMON_ERR = "没有提供Alerting相关实现类";
	
	@Autowired(required = false)
	private MessageSenderSettings meta;
	
	@Autowired(required = false)
	private IMessageSender sender;
	
	@Autowired
	private IMessageSenderRepository repo;
	
	@Autowired
	private Set<NorthstarEventType> subEvents;
	
	@GetMapping("/events")
	public ResultBean<List<NorthstarEventType>> subEvents(){
		return new ResultBean<>(subEvents.stream().toList());
	}
	
	@PostMapping("/events")
	public ResultBean<Boolean> saveSubEvents(@RequestBody List<NorthstarEventType> events){
		Set<NorthstarEventType> newEvents = new HashSet<>(events);
		if(!newEvents.equals(subEvents)) {
			repo.save(events);
			subEvents.clear();
			subEvents.addAll(events);
			log.info("全局订阅事件变更：{}", events);
		}
		return new ResultBean<>(true);
	}
	
	@GetMapping("/settings")
	public ResultBean<Map<String, ComponentField>> alertingSettingsMeta(){
		if(Objects.isNull(meta)) {
			return new ResultBean<>(Map.of());
		}
		DynamicParams params = (DynamicParams) meta;
		return new ResultBean<>(params.getMetaInfo());
	}
	
	@PostMapping("/settings")
	public ResultBean<Boolean> saveSettings(@RequestBody Map<String,ComponentField> settings){
		Assert.notNull(meta, COMMON_ERR);
		DynamicParams params = (DynamicParams) meta;
		try {
			MessageSenderSettings newSettings = (MessageSenderSettings) params.resolveFromSource(settings);
			BeanUtils.copyProperties(newSettings, meta);
			repo.save(newSettings);
		} catch (Exception e) {
			throw new IllegalStateException("配置信息解析异常", e);
		}
		return new ResultBean<>(true);
	}
	
	private AtomicInteger testCounter = new AtomicInteger();
	
	@PostMapping("/test")
	public ResultBean<Boolean> testSettings(@RequestBody Map<String,ComponentField> settings){
		Assert.notNull(meta, COMMON_ERR);
		DynamicParams params = (DynamicParams) meta;
		try {
			MessageSenderSettings newSettings = (MessageSenderSettings) params.resolveFromSource(settings);
			BeanUtils.copyProperties(newSettings, meta);
			sender.send(String.format("%s 告警测试%d", LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER), testCounter.incrementAndGet()));
		} catch (Exception e) {
			throw new IllegalStateException("配置信息解析异常", e);
		}
		return new ResultBean<>(true);
	}
}
