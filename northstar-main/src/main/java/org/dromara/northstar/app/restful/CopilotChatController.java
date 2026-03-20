package org.dromara.northstar.app.restful;

import java.util.List;

import org.dromara.northstar.app.service.CopilotChatService;
import org.dromara.northstar.common.model.ChatCompletionDTO;
import org.dromara.northstar.common.model.core.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("northstar/copilot")
@RestController
public class CopilotChatController {
	
	@Autowired(required = true)
	CopilotChatService service;

	@PostMapping(value = "chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter chatCompletion(@RequestBody ChatCompletionDTO chat) {
		return service.chatCompletion(chat);
	}
	
	@GetMapping("platforms")
	public List<String> platforms(){
		return service.availablePlatforms();
	}
	
	@GetMapping("function/queryContract")
	public List<Contract> queryContract(String query){
		return service.queryContract(query);
	}
	
}
