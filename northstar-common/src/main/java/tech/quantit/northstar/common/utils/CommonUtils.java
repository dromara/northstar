package tech.quantit.northstar.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class CommonUtils {
	
	private CommonUtils() {}

	public static LocalDateTime millsToLocalDateTime(long millis) {
		Instant instant = Instant.ofEpochMilli(millis);
		LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
		return date;
	}

	public static long localDateTimeToMills(LocalDateTime ldt, String offsetId) {
		return ldt.toInstant(ZoneOffset.of(offsetId)).toEpochMilli();
	}

	public static long localDateTimeToMills(LocalDateTime ldt) {
		return ldt.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli();
	}

	public static boolean isEquals(double d1, double d2) {
		double eps = 1e-6;
		return Math.abs(d1 - d2) < eps;
	}
}
