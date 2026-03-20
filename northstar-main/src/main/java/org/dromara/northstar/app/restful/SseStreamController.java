package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.model.GatewayVO;
import org.dromara.northstar.common.model.ModuleVO;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.support.utils.EventEmitterHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("northstar/sse")
public class SseStreamController {
	
	@Autowired
	EventEmitterHolder holder;
	
	@Autowired
	GatewayManager gatewayMgr;
	
	@Autowired
	ModuleManager moduleMgr;
	
	@GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSse(HttpServletRequest req) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        holder.add(emitter);
        
        log.info("当前用户IP: {}", req.getRemoteAddr());
        log.info("当前在线人数：{}", holder.getEmitters().size());
        
        sendGatewayStatus();
        sendModuleStatus();
        
        return emitter;
    }
	
	@Scheduled(cron="0/5 * * * * *")
	public void broadcastGatewayStatus() {
		sendGatewayStatus();
	}
	
	public void sendGatewayStatus() {
		List<GatewayVO> gateways = gatewayMgr.findAll()
				.stream()
				.map(gw -> GatewayVO.builder()
						.active(gw instanceof MarketGateway mkt && mkt.isActive())
						.connection(gw.getConnection())
						.description(gw.getDescription())
						.build())
				.toList();
		holder.send(SseEmitter.event().name("GATEWAYS").data(gateways));
	}
	
	@Scheduled(cron="0/5 * * * * *")
	public void broadcastModuleStatus() {
		sendModuleStatus();
	}
	
	public void sendModuleStatus() {
		List<ModuleVO> modules = moduleMgr.findAll()
				.stream()
				.map(m -> ModuleVO.builder()
						.description(m.getModuleContext().getModuleDescription())
						.enabled(m.getModuleContext().isEnabled())
						.state(m.getModuleContext().getState())
						.build())
				.toList();
		holder.send(SseEmitter.event().name("MODULES").data(modules));
	}
}
