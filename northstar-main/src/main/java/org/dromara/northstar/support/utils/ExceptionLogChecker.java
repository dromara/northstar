package org.dromara.northstar.support.utils;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * 异常日志检查器
 * @author KevinHuangwl
 *
 */
public class ExceptionLogChecker {
	
	private Reader reader;
	
	private static final Pattern timePtn = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}).\\d{3}");
	private static final Pattern levelPtn = Pattern.compile("ERROR");

	public ExceptionLogChecker(Reader reader) {
		this.reader = reader;
	}
	
	public List<String> getExceptionLog(LocalTime fromTime, LocalTime toTime) throws IOException{
		return IOUtils.readLines(reader).stream()
				.filter(line -> levelPtn.matcher(line).find())
				.filter(line -> {
					Matcher m = timePtn.matcher(line);
					if(m.find()) {
						int hour = Integer.parseInt(m.group(1));
						int min = Integer.parseInt(m.group(2));
						int sec = Integer.parseInt(m.group(3));
						LocalTime t = LocalTime.of(hour, min, sec);
						return fromTime.isBefore(t) && toTime.isAfter(t);
					}
					return false;
				})
				.toList();
	}
	
}
