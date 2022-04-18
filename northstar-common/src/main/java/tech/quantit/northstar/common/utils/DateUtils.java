package tech.quantit.northstar.common.utils;

import java.beans.PropertyEditorSupport;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 类描述：时间操作定义类
 *
 * @Author: changsong
 * @Date:2012-12-8 12:15:03
 * @Version 1.0
 */
public class DateUtils extends PropertyEditorSupport {
	/**
	 * 日期格式
	 *
	 * @author song.chang
	 * @create 2015年8月26日
	 */
	public static enum DTFormat {
		MM_dd("MM-dd"),
		yy_MM_dd("yy-MM-dd"),
		yy_MM_dd_HH("yy-MM-dd HH"),
		yy_MM_dd_HH_mm("yy-MM-dd HH:mm"),
		yy_MM_dd_HH_mm_ss("yy-MM-dd HH:mm:ss"),
		yyyy("yyyy"),
		yyyy_MM("yyyy-MM"),
		yyyy_MM_dd("yyyy-MM-dd"),
		yyyy_MM_dd_HH("yyyy-MM-dd HH"),
		yyyy_MM_dd_HH_mm("yyyy-MM-dd HH:mm"),
		yyyy_MM_dd_HH_mm_ss("yyyy-MM-dd HH:mm:ss"),
		MM_dd_HH_mm("MM-dd HH:mm"),
		HH_mm("HH:mm"),
		HH_mm_ss("HH:mm:ss"),
		yyMMdd("yyMMdd"),
		yyMM("yyMM"),
		yyyyMMdd("yyyyMMdd"),
		yyyyMMddHHmmss("yyyyMMddHHmmss"),
		HHmmss("HHmmss"),
		HH("HH"),
		yyyy_MM_dd_Chinese("yyyy年MM月dd日"),
		yyyy_MM_dd_HH_Chinese("yyyy年MM月dd日HH时"),
		yyyy_MM_dd_HH_mm_Chinese("yyyy年MM月dd日HH时mm分"),
		yyyy_MM_dd_HH_mm_ss_Chinese("yyyy年MM月dd日HH时mm分ss秒"),
		MM_dd_HH_Chinese("MM月dd日HH时"),
		MM_dd_HH_mm_ss_Chinese("MM月dd日HH:mm:ss"),
		dd_Chinese("dd号"),
		dd_MMMMM_dd_yyyy_US("MMMMM dd, yyyy"),
		;

		private String format;
		private int length;

		DTFormat(String format) {
			this.format = format;
			this.length = format.length();
		}

		public String getFormat() {
			return format;
		}

		public int getLength() {
			return length;
		}
	}

	/**
	 * 以毫秒表示的时间
	 */
	private static final long DAY_IN_MILLIS = 24 * 3600 * 1000;
	private static final long HOUR_IN_MILLIS = 3600 * 1000;
	private static final long MINUTE_IN_MILLIS = 60 * 1000;
	private static final long SECOND_IN_MILLIS = 1000;

	/**
	 * 指定模式的时间格式
	 *
	 * @param pattern
	 * @return
	 */
	private static SimpleDateFormat getSDFormat(String pattern) {
		return new SimpleDateFormat(pattern);
	}

	/**
	 * 当前日历，这里用中国时间表示
	 *
	 * @return 以当地时区表示的系统当前日历
	 */
	public static Calendar getCalendar() {
		return Calendar.getInstance();
	}

	/**
	 * 指定毫秒数表示的日历
	 *
	 * @param millis 毫秒数
	 * @return 指定毫秒数表示的日历
	 */
	public static Calendar getCalendar(long millis) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(millis));
		return cal;
	}

	// ////////////////////////////////////////////////////////////////////////////
	// getDate
	// 各种方式获取的Date
	// ////////////////////////////////////////////////////////////////////////////

	/**
	 * 当前日期
	 *
	 * @return 系统当前时间
	 */
	public static Date getDate() {
		return new Date();
	}

	/**
	 * 指定毫秒数表示的日期
	 *
	 * @param millis 毫秒数
	 * @return 指定毫秒数表示的日期
	 */
	public static Date getDate(long millis) {
		return new Date(millis);
	}

	/**
	 * 字符串转换成日期
	 *
	 * @param str
	 * @param sdf
	 * @return
	 */
	public static Date str2Date(String str, SimpleDateFormat sdf) {
		if (null == str || "".equals(str)) {
			return null;
		}
		Date date = null;
		try {
			date = sdf.parse(str);
			return date;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * 返回当前日期
	 *
	 * @return
	 */
	public static String now() {
		return formatDate(new Date(), DTFormat.yyyyMMddHHmmss.toString());
	}

	/**
	 * 日期转换为字符串
	 *
	 * @return 字符串
	 */
	public static String date2Str(SimpleDateFormat date_sdf) {
		Date date = getDate();
		if (null == date) {
			return null;
		}
		return date_sdf.format(date);
	}

	/**
	 * 格式化时间
	 *
	 * @param date
	 * @param format
	 * @return
	 */
	public static String dateformat(String date, String format) {
		SimpleDateFormat sformat = new SimpleDateFormat(format);
		Date _date = null;
		try {
			_date = sformat.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sformat.format(_date);
	}

	/**
	 * 日期转换为字符串
	 *
	 * @param date   日期
	 * @return 字符串
	 */
	public static String date2Str(Date date, SimpleDateFormat date_sdf) {
		if (null == date) {
			return null;
		}
		return date_sdf.format(date);
	}

	/**
	 * 日期转换为字符串
	 *
	 * @param format 日期格式
	 * @return 字符串
	 */
	public static String getDate(String format) {
		Date date = new Date();
		if (null == date) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	/**
	 * 指定毫秒数的时间戳
	 *
	 * @param millis 毫秒数
	 * @return 指定毫秒数的时间戳
	 */
	public static Timestamp getTimestamp(long millis) {
		return new Timestamp(millis);
	}

	/**
	 * 以字符形式表示的时间戳
	 *
	 * @param time 毫秒数
	 * @return 以字符形式表示的时间戳
	 */
	public static Timestamp getTimestamp(String time) {
		return new Timestamp(Long.parseLong(time));
	}

	/**
	 * 系统当前的时间戳
	 *
	 * @return 系统当前的时间戳
	 */
	public static Timestamp getTimestamp() {
		return new Timestamp(System.currentTimeMillis());
	}

	/**
	 * 指定日期的时间戳
	 *
	 * @param date 指定日期
	 * @return 指定日期的时间戳
	 */
	public static Timestamp getTimestamp(Date date) {
		return new Timestamp(date.getTime());
	}

	/**
	 * 指定日历的时间戳
	 *
	 * @param cal 指定日历
	 * @return 指定日历的时间戳
	 */
	public static Timestamp getCalendarTimestamp(Calendar cal) {
		return new Timestamp(cal.getTime().getTime());
	}

	public static Timestamp gettimestamp() {
		Date dt = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowTime = df.format(dt);
		Timestamp buydate = Timestamp.valueOf(nowTime);
		return buydate;
	}

	// ////////////////////////////////////////////////////////////////////////////
	// getMillis
	// 各种方式获取的Millis
	// ////////////////////////////////////////////////////////////////////////////
	/**
	 * 系统时间的毫秒数
	 *
	 * @return 系统时间的毫秒数
	 */
	public static long getMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * 指定日历的毫秒数
	 *
	 * @param cal 指定日历
	 * @return 指定日历的毫秒数
	 */
	public static long getMillis(Calendar cal) {
		return cal.getTime().getTime();
	}

	/**
	 * 指定日期的毫秒数
	 *
	 * @param date 指定日期
	 * @return 指定日期的毫秒数
	 */
	public static long getMillis(Date date) {
		return date.getTime();
	}

	/**
	 * 指定时间戳的毫秒数
	 *
	 * @param ts 指定时间戳
	 * @return 指定时间戳的毫秒数
	 */
	public static long getMillis(Timestamp ts) {
		return ts.getTime();
	}

	/**
	 * 获取时间字符串
	 */
	public static String getDataString(SimpleDateFormat formatstr) {
		return formatstr.format(getCalendar().getTime());
	}

	/**
	 * 默认日期按指定格式显示
	 *
	 * @param pattern 指定的格式
	 * @return 默认日期按指定格式显示
	 */
	public static String formatDate(String pattern) {
		return getSDFormat(pattern).format(getCalendar().getTime());
	}

	/**
	 * 指定日期按指定格式显示
	 *
	 * @param cal     指定的日期
	 * @param pattern 指定的格式
	 * @return 指定日期按指定格式显示
	 */
	public static String formatDate(Calendar cal, String pattern) {
		return getSDFormat(pattern).format(cal.getTime());
	}

	/**
	 * 指定日期按指定格式显示
	 *
	 * @param date    指定的日期
	 * @param pattern 指定的格式
	 * @return 指定日期按指定格式显示
	 */
	public static String formatDate(Date date, String pattern) {
		return getSDFormat(pattern).format(date);
	}

	// ////////////////////////////////////////////////////////////////////////////
	// formatTime
	// 将日期按照一定的格式转化为字符串
	// ////////////////////////////////////////////////////////////////////////////
	/**
	 * 根据指定的格式将字符串转换成Date 如输入：2003-11-19 11:20:20将按照这个转成时间
	 *
	 * @param src     将要转换的原始字符窜
	 * @param pattern 转换的匹配格式
	 * @return 如果转换成功则返回转换后的日期
	 * @throws ParseException
	 */
	public static Date parseDate(String src, String pattern)  {
		try {
			return getSDFormat(pattern).parse(src);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据指定的格式将字符串转换成Date 如输入：2003-11-19 11:20:20将按照这个转成时间
	 *
	 * @param src     将要转换的原始字符窜
	 * @param pattern 转换的匹配格式
	 * @return 如果转换成功则返回转换后的日期
	 * @throws ParseException
	 */
	public static Calendar parseCalendar(String src, String pattern) throws ParseException {

		Date date = parseDate(src, pattern);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	/**
	 * 根据指定的格式将字符串转换成Date 如输入：2003-11-19 11:20:20将按照这个转成时间
	 *
	 * @param src     将要转换的原始字符窜
	 * @param pattern 转换的匹配格式
	 * @return 如果转换成功则返回转换后的时间戳
	 * @throws ParseException
	 */
	public static Timestamp parseTimestamp(String src, String pattern) throws ParseException {
		Date date = parseDate(src, pattern);
		return new Timestamp(date.getTime());
	}

	// ////////////////////////////////////////////////////////////////////////////
	// dateDiff
	// 计算两个日期之间的差值
	// ////////////////////////////////////////////////////////////////////////////

	/**
	 * 计算两个时间之间的差值，根据标志的不同而不同
	 *
	 * @param flag   计算标志，表示按照年/月/日/时/分/秒等计算
	 * @param calSrc 减数
	 * @param calDes 被减数
	 * @return 两个日期之间的差值
	 */
	public static int dateDiff(char flag, Calendar calSrc, Calendar calDes) {

		long millisDiff = getMillis(calSrc) - getMillis(calDes);

		if (flag == 'y') {
			return (calSrc.get(Calendar.YEAR) - calDes.get(Calendar.YEAR));
		}

		if (flag == 'd') {
			return (int) (millisDiff / DAY_IN_MILLIS);
		}

		if (flag == 'h') {
			return (int) (millisDiff / HOUR_IN_MILLIS);
		}

		if (flag == 'm') {
			return (int) (millisDiff / MINUTE_IN_MILLIS);
		}

		if (flag == 's') {
			return (int) (millisDiff / SECOND_IN_MILLIS);
		}

		return 0;
	}

	/**
	 * 取得当前年份
	 *
	 * @return
	 */
	public static int getYear() {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(getDate());
		return calendar.get(Calendar.YEAR);
	}

	/**
	 * 按天数增加日期
	 *
	 * @author haibin.xiong
	 * @param date
	 * @param days
	 * @return
	 */
	public static Date addDateByDay(Date date, int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// 把日期增加相应天数。整数往后推,负数往前移动
		calendar.add(Calendar.DATE, days);
		return calendar.getTime();
	}

	/**
	 * 按小时增加日期
	 *
	 * @author haibin.xiong
	 * @param date
	 * @param hours
	 * @return
	 */
	public static Date addDateByHour(Date date, int hours) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// 把日期增加相应天数。整数往后推,负数往前移动
		calendar.add(Calendar.HOUR, hours);
		return calendar.getTime();
	}

	/**
	 * 按日期取得开始及结束时间
	 *
	 * @param date 日期
	 * @return
	 */
	public static String[] getSpanTime(Date date, int days) {
		date = addDateByDay(date, days);
		String day = DateUtils.formatDate(date, DTFormat.yyyy_MM_dd.toString());
		String[] retArr = new String[2];
		retArr[0]= day + " 00:00:00";
		retArr[1]= day + " 23:59:59";
		return retArr;
	}
}
