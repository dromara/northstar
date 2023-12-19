package org.dromara.northstar.support.log;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class ModuleLoggerFactory implements ILoggerFactory {

	private LoggerContext loggerContext = new LoggerContext();
	private PatternLayoutEncoder encoder = new PatternLayoutEncoder();

	public ModuleLoggerFactory(String moduleName) {
		encoder.setPattern("%d - %-level - [%t][" + moduleName + "][%logger{10}] : %m%n");
		encoder.setCharset(StandardCharsets.UTF_8);
		encoder.setContext(loggerContext);
		encoder.start();

		String logPath = System.getProperty("LOG_PATH");
		RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
		if (!rollingFileAppender.isStarted()) {
			rollingFileAppender.setContext(loggerContext);
			rollingFileAppender.setAppend(true);
			rollingFileAppender.setName(moduleName);
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

		ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(Level.INFO);
		rootLogger.addAppender(rollingFileAppender);
	}

	@Override
	public synchronized Logger getLogger(String name) {
		return loggerContext.getLogger(name);
	}

}
