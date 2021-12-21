package tech.quantit.northstar.strategy.api.log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class NorthstarLoggerFactory {
	
	private NorthstarLoggerFactory() {}
	private static Map<String, Logger> loggerMap = new HashMap<>();
	private static Map<String, RollingFileAppender<ILoggingEvent>> appenderMap = new HashMap<>();
	private static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
	private static PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	
	static {
		encoder.setPattern("%d - %-5level --- [%10.10t] [%40.40logger{39}] : %m%n");
	    encoder.setCharset(StandardCharsets.UTF_8);
	    encoder.setContext(loggerContext);
	    encoder.start();
	}
	
	public static synchronized Logger getLogger(String moduleName, Class<?> clz) {
		String loggerName = String.format("%s.%s", moduleName, clz.getName());
		String logPath = System.getProperty("LOG_PATH");

		RollingFileAppender<ILoggingEvent> rollingFileAppender = appenderMap.computeIfAbsent(moduleName, k -> new RollingFileAppender<>());
		if(!rollingFileAppender.isStarted()) {
			rollingFileAppender.setContext(loggerContext);
			rollingFileAppender.setAppend(true);
			rollingFileAppender.setName(moduleName);
			rollingFileAppender.setFile(logPath + File.separator +  moduleName + File.separator + moduleName);
			
			TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
			rollingPolicy.setFileNamePattern(logPath + File.separator +  moduleName + File.separator + moduleName + "_%d{yyyy-MM-dd}.log");
			rollingPolicy.setMaxHistory(10);
			rollingPolicy.setContext(loggerContext);
			rollingPolicy.setParent(rollingFileAppender);
			rollingPolicy.start();
			rollingFileAppender.setRollingPolicy(rollingPolicy);
			
			rollingFileAppender.setEncoder(encoder);
			rollingFileAppender.start();
		}
	    
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(loggerName);
        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(rollingFileAppender);
		
        if(loggerMap.containsKey(loggerName)) {
			return loggerMap.get(loggerName);
		}
		loggerMap.put(loggerName, rootLogger);
		return rootLogger;
	}

}
