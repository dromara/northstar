package tech.quantit.northstar.main;

import java.io.File;
import java.net.URL;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExternalJarListener implements CommandLineRunner{
	
	@Getter
	private ClassLoader externalClassLoader;

	@Override
	public void run(String... args) throws Exception {
		ApplicationHome appHome = new ApplicationHome(getClass());
		File appPath = appHome.getDir();
		for(File file : appPath.listFiles()) {
			if(file.getName().contains("northstar-external") && Files.getFileExtension(file.getName()).equalsIgnoreCase("jar") && !file.isDirectory()) {
				log.info("加载northstar-external扩展包");
				ExternalJarClassLoader clzLoader = new ExternalJarClassLoader(new URL[] {file.toURI().toURL()}, getClass().getClassLoader());
				SpringContextUtil.getBeanFactory().setBeanClassLoader(clzLoader);
				Thread.currentThread().setContextClassLoader(clzLoader);
				clzLoader.initBean();
				externalClassLoader = clzLoader;
				return;
			}
		}
	}

}
