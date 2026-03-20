package org.dromara.northstar.app.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MaliciousIPCollector {

	private ConcurrentMap<String, ConcurrentLinkedQueue<Long>> suspectIPMap = new ConcurrentHashMap<>(); 
	
	private static final long DAY = TimeUnit.DAYS.toMillis(1);
	
	private static final int MAX_REQ = 10;
	
	public void log(HttpServletRequest req) {
		String ip = req.getRemoteAddr();
		log.info("记录可疑IP：{}", ip);
		suspectIPMap.computeIfAbsent(ip, k -> new ConcurrentLinkedQueue<>());
		ConcurrentLinkedQueue<Long> cacheQ = suspectIPMap.get(ip);
		cacheQ.offer(System.currentTimeMillis());
		if(cacheQ.size() > MAX_REQ) {
			cacheQ.poll();
		}
	}
	
	public boolean shouldBlock(HttpServletRequest req) {
		String ip = req.getRemoteAddr();
		return suspectIPMap.containsKey(ip) 
				&& suspectIPMap.get(ip).size() == MAX_REQ
				&& System.currentTimeMillis() - suspectIPMap.get(ip).peek() < DAY;
	}
}
