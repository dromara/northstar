package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

class ModuleLoggingFactoryTest {

	@Test
	void testLogFile() {
		System.setProperty("LOG_PATH", "target");
		Logger log1 = ModuleLoggingFactory.getLogger("testLogging", this.getClass());
		Logger log2 = ModuleLoggingFactory.getLogger("testLogging2", this.getClass());
		log1.info("This is INFO message");
		log2.info("This is INFO message");
		
		assertThat(new File("target/testLogging/module.log")).exists();
		assertThat(new File("target/testLogging2/module.log")).exists();
	}
	
	@Test
	void testLogInitLevel() {
		System.setProperty("LOG_PATH", "target");
		ch.qos.logback.classic.Logger nslogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ModuleLoggingFactory.ROOT);
		nslogger.setLevel(Level.INFO);
		Logger log = ModuleLoggingFactory.getLogger("testLogging3", this.getClass());
		assertThat(log.isDebugEnabled()).isFalse();
		
		nslogger.setLevel(Level.TRACE);
		Logger log2 = ModuleLoggingFactory.getLogger("testLogging2", this.getClass());
		assertThat(log2.isTraceEnabled()).isTrue();
	}
	
	@Test
	void testLogSetLevel() {
		System.setProperty("LOG_PATH", "target");
		ModuleLoggingFactory.setLevel("testLogging", Level.WARN);
		Logger log = ModuleLoggingFactory.getLogger("testLogging", this.getClass());
		assertThat(log.isWarnEnabled()).isTrue();
		assertThat(log.isInfoEnabled()).isFalse();
		
		ModuleLoggingFactory.setLevel("testLogging", Level.DEBUG);
		assertThat(log.isDebugEnabled()).isTrue();
		assertThat(log.isTraceEnabled()).isFalse();
	}

}
