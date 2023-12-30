package org.dromara.northstar.rl.agent;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAgent implements Closeable{
	
	private Process proc;

	protected AbstractAgent(String name, Class<?> clz) throws Exception {
		checkPythonEnv();
		start(resourcePath(name, clz));
	}

	private void checkPythonEnv() throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder("python", "--version");
		Process p = pb.start();
		if(p.waitFor() != 0) {
			throw new IllegalStateException("缺少PYTHON环境，请自行安装");
		}
	}
	
	private void start(String path) throws IOException, InterruptedException {
		log.info("启动agent，加载py文件：{}", path);
	    ProcessBuilder pb = new ProcessBuilder("python", path);
	    proc = pb.start();
	    // 等待10秒，加载tensorflow时间比较长
	    if(proc.waitFor(10, TimeUnit.SECONDS)) {
	    	BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while((line = errorReader.readLine()) != null) {
            	sb.append(line).append("\n");
            }
            throw new IllegalStateException(sb.toString());
	    }
	}
	
	private String resourcePath(String name, Class<?> clz) {
		String packageName = clz.getPackageName();
		String resourcePath = String.format("%s/%s.py", packageName.replace(".", "/"), name);
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		log.info("查询py文件：{}", resourcePath);
		String absolutePath = classLoader.getResource(resourcePath).getFile();
		if(System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS")) {
			absolutePath = absolutePath.replaceFirst("/", "");
		} 
		return absolutePath;
	}

	@Override
	public void close() throws IOException {
		if(proc != null) {
			proc.destroyForcibly();
		}
	}
	
}
