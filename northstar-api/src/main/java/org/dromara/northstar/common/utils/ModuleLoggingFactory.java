package org.dromara.northstar.common.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.dromara.northstar.common.ModuleLoggerAware;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

/**
 * 每个模组都有自己的Logger与Appender，
 * 为了让模组对象内部的对象都能获取到所属模组的Logger，只能通过模组名称来区分
 */
public class ModuleLoggingFactory {
	
	public static final String ROOT = "org.dromara.northstar";
	
	private static Map<String, ILoggerFactory> factoryMap = new HashMap<>();
	
	private ModuleLoggingFactory() {}
	
	public static ILoggerFactory getFactory(String moduleName) {
		synchronized (factoryMap) {			
			if(factoryMap.containsKey(moduleName)) {
				return factoryMap.get(moduleName);
			}
			
			LoggerContext loggerContext = new LoggerContext();
			PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			
			encoder.setPattern("%d - %level - [%t][" + moduleName + "][%logger{10}] : %m%n");
			encoder.setCharset(StandardCharsets.UTF_8);
			encoder.setContext(loggerContext);
			encoder.start();
			
			loggerContext.setMDCAdapter(new LogbackMDCAdapter());

			String logPath = System.getProperty("LOG_PATH");
			RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
			if (!rollingFileAppender.isStarted()) {
				rollingFileAppender.setContext(loggerContext);
				rollingFileAppender.setAppend(true);
				rollingFileAppender.setName(ROOT);
				rollingFileAppender.setFile(logPath + File.separator + moduleName + File.separator + "module.log");

				TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
				rollingPolicy.setFileNamePattern(
						logPath + File.separator + moduleName + File.separator + "module_%d{yyyy-MM-dd}.log");
				rollingPolicy.setMaxHistory(10);
				rollingPolicy.setContext(loggerContext);
				rollingPolicy.setParent(rollingFileAppender);
				rollingPolicy.start();
				rollingFileAppender.setRollingPolicy(rollingPolicy);

				rollingFileAppender.setEncoder(encoder);
				rollingFileAppender.start();
			}

			LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			ch.qos.logback.classic.Logger nslogger = context.getLogger(ROOT);
			
			ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ROOT);
			rootLogger.setLevel(Optional.ofNullable(nslogger.getLevel()).orElse(Level.INFO));
			rootLogger.addAppender(rollingFileAppender);
			
			factoryMap.put(moduleName, loggerContext);
			return loggerContext;
		}
	}
	
	public static Logger getLogger(String moduleName, String name) {
		return getFactory(moduleName).getLogger(name);
	}
	
	public static Logger getLogger(String moduleName, Class<?> clz) {
		return getFactory(moduleName).getLogger(clz.getName());
	}
	
	public static void setLevel(String moduleName, Level level) {
		LoggerContext ctx = (LoggerContext) getFactory(moduleName);
		ch.qos.logback.classic.Logger rootLogger = ctx.getLogger(ROOT);
		rootLogger.setLevel(level);
	}

	public static void register(String moduleName, ModuleLoggerAware obj){
		Logger logger = getLogger(moduleName, obj.getClass());
		obj.setLogger(logger);
	}

}
