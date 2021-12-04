package tech.quantit.northstar.common.constant;

import java.time.format.DateTimeFormatter;

public interface DateTimeConstant {
	static final String DT_FORMAT_WITH_MS = "yyyy-MM-dd HH:mm:ss.SSS";
	static final String DT_FORMAT_WITH_MS_INT = "yyyyMMddHHmmssSSS";
	static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
	static final String DT_FORMAT_INT = "yyyyMMddHHmmss";

	static final String T_FORMAT_WITH_MS_INT = "HHmmssSSS";
	static final String T_FORMAT_WITH_MS = "HH:mm:ss.SSS";
	static final String T_FORMAT_INT = "HHmmss";
	static final String T_FORMAT = "HH:mm:ss";
	static final String D_FORMAT_INT = "yyyyMMdd";
	static final String D_FORMAT = "yyyy-MM-dd";

	static final DateTimeFormatter DT_FORMAT_WITH_MS_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_WITH_MS);
	static final DateTimeFormatter DT_FORMAT_WITH_MS_INT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_WITH_MS_INT);
	static final DateTimeFormatter DT_FORMAT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT);
	static final DateTimeFormatter DT_FORMAT_INT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_INT);

	static final DateTimeFormatter T_FORMAT_WITH_MS_INT_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT_WITH_MS_INT);
	static final DateTimeFormatter T_FORMAT_WITH_MS_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT_WITH_MS);
	static final DateTimeFormatter T_FORMAT_INT_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT_INT);
	static final DateTimeFormatter T_FORMAT_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT);

	static final DateTimeFormatter D_FORMAT_INT_FORMATTER = DateTimeFormatter.ofPattern(D_FORMAT_INT);
	static final DateTimeFormatter D_FORMAT_FORMATTER = DateTimeFormatter.ofPattern(D_FORMAT);

}
