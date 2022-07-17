package tech.quantit.northstar.strategy.api.log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class ModuleLoggerFactory implements ILoggerFactory{
	
	private static Map<String, Logger> loggerMap = new HashMap<>();
	private static Map<String, RollingFileAppender<ILoggingEvent>> appenderMap = new HashMap<>();
	private static LoggerContext loggerContext = new LoggerContext();
	private static PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	
	static {
		encoder.setPattern("%d - %-5level --- [%10.10t] [%40.40logger{39}] : %m%n");
	    encoder.setCharset(StandardCharsets.UTF_8);
	    encoder.setContext(loggerContext);
	    encoder.start();
	}
	
	@Override
	public synchronized Logger getLogger(String name) {
		String logPath = System.getProperty("LOG_PATH");
		String logLevel = System.getProperty("LOG_LEVEL");
		if(loggerMap.containsKey(name)) {
			return loggerMap.get(name);
		}
		
		RollingFileAppender<ILoggingEvent> rollingFileAppender = appenderMap.computeIfAbsent(name, k -> new RollingFileAppender<>());
		if(!rollingFileAppender.isStarted()) {
			rollingFileAppender.setContext(loggerContext);
			rollingFileAppender.setAppend(true);
			rollingFileAppender.setName(name);
			rollingFileAppender.setFile(logPath + File.separator +  name + File.separator + name + ".log");
			
			TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
			rollingPolicy.setFileNamePattern(logPath + File.separator +  name + File.separator + name + "_%d{yyyy-MM-dd}.log");
			rollingPolicy.setMaxHistory(10);
			rollingPolicy.setContext(loggerContext);
			rollingPolicy.setParent(rollingFileAppender);
			rollingPolicy.start();
			rollingFileAppender.setRollingPolicy(rollingPolicy);
			
			rollingFileAppender.setEncoder(encoder);
			rollingFileAppender.start();
		}
	    
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(name);
        rootLogger.setLevel(Level.toLevel(logLevel));
        rootLogger.addAppender(rollingFileAppender);
        
		loggerMap.put(name, rootLogger);
		return rootLogger;
	}

}
