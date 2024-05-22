package org.dromara.northstar.support.log;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

class ModuleLoggerFactoryTest {
	
	static {
		System.setProperty("LOG_PATH", "logs");
	}
	ModuleLoggerFactory factory = new ModuleLoggerFactory("单元测试", "INFO");

	@Test
	void test() {
		Logger logger = (Logger) factory.getLogger("test");
		logger.setLevel(Level.TRACE);
		logger.trace("TRACE LOG");
		logger.debug("DEBUG LOG");
		logger.info("INFO LOG");
	}

}
