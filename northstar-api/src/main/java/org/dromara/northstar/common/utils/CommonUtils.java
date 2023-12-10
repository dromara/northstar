package org.dromara.northstar.common.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.apache.commons.io.FileUtils;

public class CommonUtils {
	
	private CommonUtils() {}

	/**
	 * 毫秒转LocalDateTime
	 * @param millis
	 * @return
	 */
	public static LocalDateTime millsToLocalDateTime(long millis) {
		Instant instant = Instant.ofEpochMilli(millis);
		LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
		return date;
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
}
