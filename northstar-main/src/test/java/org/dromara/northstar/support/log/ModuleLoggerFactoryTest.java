package org.dromara.northstar.support.log;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

class ModuleLoggerFactoryTest {
	
	static {
		System.setProperty("LOG_PATH", "logs");
	}
	ModuleLoggerFactory factory = new ModuleLoggerFactory("单元测试");

	@Test
	void test() {
		long start = System.currentTimeMillis();
		AtomicInteger cnt = new AtomicInteger();
		Logger logger = (Logger) factory.getLogger("test");
		logger.setLevel(Level.TRACE);
		while(System.currentTimeMillis() - start < 1000) {
			logger.info("why no output? {}", cnt);
		}
		
	}

}
