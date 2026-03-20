package org.dromara.northstar.common.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	public static boolean equals(double d1, double d2, double eps) {
		return Math.abs(d1 - d2) < eps;
	}
	
	/**
	 * 判断compareWithVal是否在v1和v2之间
	 * @param v1
	 * @param v2
	 * @param compareWithVal
	 * @return
	 */
	public static boolean isBetween(double v1, double v2, double compareWithVal) {
		return compareWithVal < Math.max(v1, v2) && compareWithVal > Math.min(v1, v2);
	}
	
	/**
	 * 判断value是否是baseVal的整数倍，precision表示小数点后精度
	 * @param value
	 * @param baseVal
	 * @param precision
	 * @return
	 */
	public static boolean isNTimesOf(double value, double baseVal) {
		if (baseVal == 0) {
			return value == 0;
		}
		int precision = precisionOf(baseVal) + 3;
		double nTimes = value / baseVal;
		BigDecimal compareVal = BigDecimal.valueOf(nTimes).setScale(0, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(baseVal)).setScale(precision, RoundingMode.HALF_UP);
		BigDecimal actualVal = BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
		return actualVal.equals(compareVal);
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
	 * 创建虚拟线程工厂并进行命名，类名前十位字符加-virtual
	 * @param clz
	 * @return
	 */
	public static ThreadFactory virtualThreadFactory(Class<?> clz) {
		String className = clz.getSimpleName(); // 获取类名
		if (className.length() > 10) {
			className = className.substring(0, 10);
		}
		className += "-virtual";
		return Thread.ofVirtual().name(className).factory();
	}
	
	/**
	 * 创建虚拟线程服务
	 * @param clz
	 * @return
	 */
	public static ExecutorService newThreadPerTaskExecutor(Class<?> clz) {
		return Executors.newThreadPerTaskExecutor(virtualThreadFactory(clz));
	}
}
