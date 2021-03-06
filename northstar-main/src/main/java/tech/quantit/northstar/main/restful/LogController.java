package tech.quantit.northstar.main.restful;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.model.LogDescription;
import tech.quantit.northstar.common.model.ResultBean;
import tech.quantit.northstar.main.service.LogService;

@RequestMapping("/northstar/log")
@RestController
public class LogController {

	@Autowired
	private LogService service;
	
	@GetMapping
	public ResultBean<LogDescription> tailNorthstarLog(long positionOffset) throws IOException{
		LocalDate today = LocalDate.now();
		File logPath = new File(System.getProperty("LOG_PATH"));
		File logFile = new File(logPath, "Northstar_" + today.format(DateTimeConstant.D_FORMAT_FORMATTER) + ".log");
		return new ResultBean<>(service.tailLogFile(logFile, positionOffset));
	}
	
	@GetMapping("/module")
	public ResultBean<LogDescription> tailModuleLog(String name, long positionOffset) throws IOException{
		File moduleLogPath = new File(System.getProperty("LOG_PATH"), name);
		File moduleLogFile = new File(moduleLogPath, name + ".log");
		return new ResultBean<>(service.tailLogFile(moduleLogFile, positionOffset));
	}
	
	@PutMapping("/level")
	public ResultBean<Void> setPlatformLogLevel(LogLevel level){
		service.setPlatformLogLevel(level);
		return new ResultBean<Void>(null);
	}
	
	@GetMapping("/level")
	public ResultBean<LogLevel> getPlatformLogLevel(){
		return new ResultBean<>(service.getPlatformLogLevel());
	}
	
	@PutMapping("/{moduleName}/level")
	public ResultBean<Void> setModuleLogLevel(@PathVariable String moduleName, LogLevel level){
		service.setModuleLogLevel(moduleName, level);
		return new ResultBean<Void>(null);
	}
	
	@GetMapping("/{moduleName}/level")
	public ResultBean<LogLevel> getModuleLogLevel(@PathVariable String moduleName){
		return new ResultBean<>(service.getModuleLogLevel(moduleName));
	}
}
