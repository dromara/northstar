package org.dromara.northstar.support.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.alibaba.fastjson2.JSON;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class EventEmitterHolder {
	
	private Set<SseEmitter> emitters = new HashSet<>();
	
	public void add(SseEmitter emitter) {
		synchronized (this) {
			emitters.add(emitter);
		}
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onError(ex -> {
        	if(log.isDebugEnabled()) {        		
        		log.debug("SSE错误：{}", ex.getMessage());
        	}
        	synchronized (this) {
        		emitters.remove(emitter);
			}
        });
	}
	
	public void send(SseEventBuilder event) {
		synchronized (this) {
			Iterator<SseEmitter> itEmtr = emitters.iterator();
			while(itEmtr.hasNext()) {
				SseEmitter emitter = itEmtr.next();
				try {
					emitter.send(event.reconnectTime(3000L));
				} catch (Exception e) {
					if(log.isDebugEnabled()) {						
						log.debug("事件分发异常，原因：{}", e.getMessage());
						String details = JSON.toJSONString(event.build().stream().map(DataWithMediaType::getData).toList());
						log.debug("详情：{}", (details.length() > 200 ? details.substring(0, 200) + "..." : details));
					}
					itEmtr.remove();
				}
			}
		}
	}
	
	public enum Event {
		TICKS,
	}
}
