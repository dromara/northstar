package org.dromara.northstar.common.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;

public class CommonUtils {
	
	private CommonUtils() {}

	/**
	 * 毫秒转LocalDateTime
	 * @param millis
	 * @return
	 */
	public static LocalDateTime millsToLocalDateTime(long millis) {
		Instant instant = Instant.ofEpochMilli(millis);
		return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	/**
	 * LocalDateTime转毫秒
	 * @param ldt
	 * @return
	 */
	public static long localDateTimeToMills(LocalDateTime ldt) {
		return ldt.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli();
	}

	/**
	 * 浮点数判等
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static boolean isEquals(double d1, double d2) {
		double eps = 1e-6;
		return Math.abs(d1 - d2) < eps;
	}
	
	/**
	 * 复制URL到临时文件夹,例如从war包中
	 * 
	 * @param targetDir
	 * @param sourceURL
	 * @throws IOException
	 */
	public static void copyURLToFileForTmp(String targetDir, URL sourceURL) throws IOException {
		File orginFile = new File(sourceURL.getFile());
		File targetFile = new File(targetDir + File.separator + orginFile.getName());
		if (!targetFile.getParentFile().exists()) {
			targetFile.getParentFile().mkdirs();
		}
		Files.deleteIfExists(targetFile.toPath());
		FileUtils.copyURLToFile(sourceURL, targetFile);

		targetFile.deleteOnExit();
	}
	
	/**
	 * 计算价格精度
	 * @return
	 */
	public static int precisionOf(double priceTick) {
		Assert.isTrue(priceTick > 0, "最小变动价位应该为正数");
		for(int i = 0; i<10; i++) {
			if(priceTick * Math.pow(10, i) >= 1) {
				return i;
			}
		}
		throw new IllegalArgumentException("价格精度最多为小数点后九位");
	}

	/**
	 * @param c Class
	 * @return ThreadFactory
	 * 创建虚拟线程工厂并进行命名，类名前十位字符加-virtual
	 */
	public static ThreadFactory virtualThreadFactory(Class c) {
		String className = c.getSimpleName(); // 获取MyClass类的类名
		if (className.length() > 10) {
			className = className.substring(0, 10);
		}
		className += "-virtual";
		ThreadFactory factory = Thread.ofVirtual().name(className).factory();
		return factory;
	}
}
