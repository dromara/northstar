package tech.quantit.northstar.main.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import cn.hutool.core.io.FileUtil;
import tech.quantit.northstar.common.model.LogDescription;
import tech.quantit.northstar.strategy.api.log.ModuleLoggerFactory;

public class LogService {
	
	private static final String NORTHSTAR_ROOT = "tech.quantit.northstar";
	
	private ModuleLoggerFactory moduleLoggerFactory = new ModuleLoggerFactory();
	
	private LoggingSystem loggingSystem;
	
	public LogService(LoggingSystem logginSystem) {
		this.loggingSystem = logginSystem;
	}
	
	public LogDescription tailLogFile(File logFile, long positionOffset) throws IOException {
		LinkedList<String> list = new LinkedList<>();
		LogDescription result = new LogDescription();
		result.setStartPosition(positionOffset);
		try(RandomAccessFile raf = new RandomAccessFile(logFile, "r")){
			long lenOfFile = raf.length();
			result.setEndPosition(lenOfFile);
			raf.seek(positionOffset);
			while(raf.getFilePointer() < lenOfFile) {
				String line = FileUtil.readLine(raf, StandardCharsets.UTF_8);
				if(StringUtils.isNotEmpty(line)) {					
					list.add(line);
				}
			}
			
			result.setLinesOfLog(list);
			return result;
		}
	}
	
	public void setPlatformLogLevel(LogLevel level) {
		loggingSystem.setLogLevel(NORTHSTAR_ROOT, level);
	}
	
	public void setModuleLogLevel(String moduleName, LogLevel level) {
		Logger logger = (Logger) moduleLoggerFactory.getLogger(moduleName);
		logger.setLevel(Level.toLevel(level.toString()));
	}
	
	public LogLevel getPlatformLogLevel() {
		return loggingSystem.getLoggerConfiguration(NORTHSTAR_ROOT).getEffectiveLevel();
	}
	
	public LogLevel getModuleLogLevel(String moduleName) {
		Logger logger = (Logger) moduleLoggerFactory.getLogger(moduleName);
		return LogLevel.valueOf(logger.getLevel().toString());
	}
}
