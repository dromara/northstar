package org.dromara.northstar.app.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.dromara.northstar.common.model.LogViewVO;
import org.dromara.northstar.common.utils.ModuleLoggingFactory;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LoggingService {
	
	private static final String NS_ROOT = "org.dromara.northstar";
	
	public LogViewVO tailRootLogFile(long offset) throws IOException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = context.getLogger(NS_ROOT);
		FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) logger.getAppender("FILE");
		File logFile = new File(fileAppender.getFile());
		LogViewVO vo = tail(logFile, offset);
		vo.setLogLevel(logger.getLevel().toString());
		return vo;
	}

	public LogViewVO tailModuleLogFile(String moduleName, long offset) throws IOException {
		Logger logger = (Logger) ModuleLoggingFactory.getFactory(moduleName).getLogger(ModuleLoggingFactory.ROOT);
		FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) logger.getAppender(ModuleLoggingFactory.ROOT);
		File logFile = new File(fileAppender.getFile());
		LogViewVO vo = tail(logFile, offset);
		vo.setLogLevel(logger.getLevel().toString());
		return vo;
	}
	
	private LogViewVO tail(File logFile, long offset) throws IOException {
		LogViewVO logView = LogViewVO.builder().build();
		try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
			long lenOfFile = raf.length();
			logView.setFileOffset(lenOfFile);
			if(lenOfFile > offset) {				
				long realOffset = Math.max(offset, lenOfFile - 50000); // 最多加载50K的日志数据
				raf.seek(realOffset);
				StringBuilder sb = new StringBuilder();
				while (raf.getFilePointer() < lenOfFile) {
					String line = FileUtil.readLine(raf, StandardCharsets.UTF_8);
					if (StringUtils.hasText(line)) {
						sb.append(line).append("\n");
					}
				}
				logView.setContent(sb.toString());
			}
		}
		return logView;
	}

	public void setRootLogLevel(Level level) {
		log.warn("平台日志打印级别设置为：{}", level);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = context.getLogger(NS_ROOT);
		logger.setLevel(level);
	}

	public void setModuleLogLevel(String moduleName, Level level) {
		ModuleLoggingFactory.setLevel(moduleName, level);
		ModuleLoggingFactory.getLogger(moduleName, getClass()).info("模组日志打印级别设置为：{}", level);
	}
}
