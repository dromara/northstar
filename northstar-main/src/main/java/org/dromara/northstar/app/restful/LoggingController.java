package org.dromara.northstar.app.restful;

import java.io.IOException;

import org.dromara.northstar.app.service.LoggingService;
import org.dromara.northstar.common.model.LogViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.classic.Level;

@RequestMapping("northstar/logging")
@RestController
public class LoggingController {
	
	@Autowired
	private LoggingService service;

	@GetMapping("tail/{offset}")
	public LogViewVO tail(@PathVariable long offset) throws IOException {
		return service.tailRootLogFile(offset);
	}
	
	@GetMapping("tail/{offset}/{moduleName}")
	public LogViewVO tail(@PathVariable String moduleName, @PathVariable long offset) throws IOException {
		return service.tailModuleLogFile(moduleName, offset);
	}
	
	@PutMapping("setLevel/{level}")
	public void setLogLevel(@PathVariable Level level) {
		service.setRootLogLevel(level);
	}
	
	@PutMapping("setLevel/{level}/{moduleName}")
	public void setLogLevel(@PathVariable String moduleName, @PathVariable Level level) {
		service.setModuleLogLevel(moduleName, level);
	}
}
