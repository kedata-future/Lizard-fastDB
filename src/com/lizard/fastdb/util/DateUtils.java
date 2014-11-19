package com.lizard.fastdb.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 日期操作工具类，用于各种日期的获取和转换
 * 
 * @author SHEN.GANG
 */
public class DateUtils {

	
	/**
	 * 转换long型(秒数)日期格式为字符型日期，精确到时分秒，如：convertTimeToString( 1264834129 ) -->2012-01-30 14:48:49
	 * @param longTime 需要转换的long型日期
	 * @return 格式为yyyy-MM-dd HH:mm:ss的字符型日期
	 */
	public static String convertTimeToString(long longTime) {
		return convertTimeToString(longTime, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 转换long型(秒数)日期格式为字符型日期，日期格式通过 format 定义，如：convertTimeToString( 1264834129 ,"yyyy-MM-dd" ) --> 2012-01-30
	 * @param longTime  需要转换的long型日期
	 * @param format 日期格式 年：yyyy 月：MM 日：dd 小时：HH 分钟：mm 秒：ss
	 * @return 格式为yyyy-MM-dd HH:mm:ss的字符型日期
	 */
	public static String convertTimeToString(long longTime, String format) {
		try {
			Timestamp t = new Timestamp(longTime * 1000);
			SimpleDateFormat sDateFormat = new SimpleDateFormat(format);
			return sDateFormat.format(t);
		} catch (Exception ex) {
			throw new RuntimeException("Can't format the time by format["+format+"]!");
		}

	}

	/**
	 * 转换字符型日期(精确到时分秒)为long型日期(秒数)，如：convertTimeToLong( "2013-01-30 14:48:49" )--> 1264834129
	 * @param dateTime 需要转换的Str型日期
	 * @return long型日期
	 */
	public static long convertTimeToLong(String dateTime) {
		return convertTimeToLong(dateTime, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 转换字符型日期(日期格式通过format定义)为long型日期(秒数)，如：convertTimeToLong( "2012-01-30","yyyy-MM-dd" ) --> 1264780800
	 * @param dateTime 需要转换的Str型日期
	 * @param format 日期格式 年：yyyy 月：MM 日：dd 小时：HH 分钟：mm 秒：ss
	 * @return long型日期
	 */
	public static long convertTimeToLong(String dateTime, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		long date = 0;
		try {
			Date d = sdf.parse(dateTime);
			date = d.getTime() / 1000;
		} catch (Exception ex) {
			throw new RuntimeException("Can't format the time by format["+format+"]!");
		}
		return date;
	}

	/**
	 * 获取当前时间的long型值(秒数)。
	 * @return long型日期
	 */
	public static long getCurrentLongTime() {
		return System.currentTimeMillis() / 1000;
	}

	/**
	 * 获取当前时间字符串表示，精确到时分秒，如：2013-01-30 14:48:49
	 * @return 字符型日期
	 */
	public static String getCurrentTime() {
		return getCurrentTime("yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 获取当前时间字符串表示，日期格式由format定义，如：getCurrentTime("yyyy-MM-dd") --> 2013-01-30
	 * @param format 日期格式 年：yyyy 月：MM 日：dd 小时：HH 分钟：mm 秒：ss";
	 * @return 日期字符串
	 */
	public static String getCurrentTime(String format) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.format(new Date());
		} catch (Exception e) {
			throw new RuntimeException("Can't format the time by format["+format+"]!");
		}
	}

	/**
	 * 获取2个日期之间间隔的天数，如：getDayByDateToDate("2009-12-1","2009-9-29"); --> -63
	 * @param startDate 起始时间
	 * @param endDate 结束时间
	 * @return 间隔天数（long型）
	 */
	public static long getDayBetweenDateAndDate(String startDate, String endDate) {

		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
		long day = 0;
		try {
			Date date = myFormatter.parse(endDate);
			Date mydate = myFormatter.parse(startDate);
			day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
		} catch (Exception ex) {
			throw new RuntimeException("Time format["+startDate+"]["+endDate+"] is error ! format must be 'yyyy-MM-dd'！");
		}
		return day;
	}

	/**
	 * 根据日期获取是星期几 0,1,2,3,4,5,6 分别对应 礼拜日,礼拜一，礼拜二，礼拜三，礼拜四，礼拜五，礼拜六
	 * @return 0-6 
	 */
	public static int getWeekday(String dateTime) {
		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date date = myFormatter.parse(dateTime);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			return cal.get(Calendar.DAY_OF_WEEK) - 1< 0 ? 0 : cal.get(Calendar.DAY_OF_WEEK) - 1;
		} catch (ParseException e) {
			throw new RuntimeException("Time format["+dateTime+"] is error ! format must be 'yyyy-MM-dd'！");
		}
		
	}

	/**
	 * 获得上周一的日期字符串
	 * @return 上周星期一的日期字符串
	 */
	public static String getPreWeekFirstDay() {
		Calendar cd = Calendar.getInstance();
		int week= cd.get(Calendar.DAY_OF_WEEK) == 1 ? 8 : cd.get(Calendar.DAY_OF_WEEK);
		int mondayPlus = week - 1 == 1 ? 0 : 1 - (week - 1);
		GregorianCalendar currentDate = new GregorianCalendar();
		currentDate.add(GregorianCalendar.DATE, mondayPlus - 7);
		Date monday = currentDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(monday);
	}

	/**
	 * 获得上周日的日期字符串
	 * @return 上周星期日的日期字符串
	 */
	public static String getPreWeekLastDay() {
		Calendar cd = Calendar.getInstance();
		int week= cd.get(Calendar.DAY_OF_WEEK) == 1 ? 8 : cd.get(Calendar.DAY_OF_WEEK);
		int mondayPlus = week - 1 == 1 ? 0 : 1 - (week - 1);
		GregorianCalendar currentDate = new GregorianCalendar();
		currentDate.add(GregorianCalendar.DATE, mondayPlus -1);
		Date monday = currentDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(monday);
	}

	/**
	 * 获得下周一的日期字符串
	 * @return 下周星期一的日期字符串
	 */
	public static String getNextWeekFirstDay() {
		Calendar cd = Calendar.getInstance();
		int week= cd.get(Calendar.DAY_OF_WEEK) == 1 ? 8 : cd.get(Calendar.DAY_OF_WEEK);
		int mondayPlus = week - 1 == 1 ? 0 : 1 - (week - 1);
		GregorianCalendar currentDate = new GregorianCalendar();
		currentDate.add(GregorianCalendar.DATE, mondayPlus + 7);
		Date monday = currentDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(monday);
	}

	/**
	 * 获得下周日的日期字符串
	 * @return 下周星期日的日期字符串
	 */
	public static String getNextWeekLastDay() {
		Calendar cd = Calendar.getInstance();
		int week= cd.get(Calendar.DAY_OF_WEEK) == 1 ? 8 : cd.get(Calendar.DAY_OF_WEEK);
		int mondayPlus = week - 1 == 1 ? 0 : 1 - (week - 1);
		GregorianCalendar currentDate = new GregorianCalendar();
		currentDate.add(GregorianCalendar.DATE, mondayPlus + 7 + 6);
		Date monday = currentDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(monday);
	}

	/**
	 * 获得本周一的日期字符串
	 * @return 本周星期一的日期字符串
	 */
	public static String getWeekFirstDay() {
		Calendar cd = Calendar.getInstance();
		int week= cd.get(Calendar.DAY_OF_WEEK) == 1 ? 8 : cd.get(Calendar.DAY_OF_WEEK);
		int mondayPlus = week - 1 == 1 ? 0 : 1 - (week - 1);
		GregorianCalendar currentDate = new GregorianCalendar();
		currentDate.add(GregorianCalendar.DATE, mondayPlus);
		Date monday = currentDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(monday);
	}

	/**
	 * 获得本周日的日期字符串
	 * @return 本周星期日的日期字符串
	 */
	public static String getWeekLastDay() {
		Calendar cd = Calendar.getInstance();
		int week= cd.get(Calendar.DAY_OF_WEEK) == 1 ? 8 : cd.get(Calendar.DAY_OF_WEEK);
		int mondayPlus = week - 1 == 1 ? 0 : 1 - (week - 1);
		GregorianCalendar currentDate = new GregorianCalendar();
		currentDate.add(GregorianCalendar.DATE, mondayPlus + 6);
		Date monday = currentDate.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(monday);
	}

	/**
	 * 获取上个月第一天日期字符串
	 * @return 上个月第一天日期字符串
	 */
	public static String getPreMonthFirstDay() {
		String str = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar lastDate = Calendar.getInstance();
		lastDate.set(Calendar.DATE, 1);
		lastDate.add(Calendar.MONTH, -1);
		str = sdf.format(lastDate.getTime());
		return str;
	}

	/**
	 * 获取上个月最后一天日期字符串
	 * @return 上个月最后一天日期字符串
	 */
	public static String getPreMonthLastDay() {
		String str = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar lastDate = Calendar.getInstance();
		lastDate.add(Calendar.MONTH, -1);
		lastDate.set(Calendar.DATE, 1);
		lastDate.roll(Calendar.DATE, -1);
		str = sdf.format(lastDate.getTime());
		return str;
	}

	/**
	 * 获取本月第一天日期字符串
	 * @return 本月第一天日期字符串
	 */
	public static String getMonthFirstDay() {
		String str = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar lastDate = Calendar.getInstance();
		lastDate.set(Calendar.DATE, 1);
		str = sdf.format(lastDate.getTime());
		return str;
	}

	/**
	 * 获取本月最后一天日期字符串
	 * @return 本月最后一天日期字符串
	 */
	public static String getMonthLastDay() {
		String str = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar lastDate = Calendar.getInstance();
		lastDate.set(Calendar.DATE, 1);
		lastDate.add(Calendar.MONTH, 1);
		lastDate.add(Calendar.DATE, -1);
		str = sdf.format(lastDate.getTime());
		return str;
	}

	/**
	 * 获取下个月第一天日期字符串
	 * @return 下个月第一天日期字符串
	 */
	public static String getNextMonthFirstDay() {
		String str = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar lastDate = Calendar.getInstance();
		lastDate.add(Calendar.MONTH, 1);
		lastDate.set(Calendar.DATE, 1);
		str = sdf.format(lastDate.getTime());
		return str;
	}

	/**
	 * 获取下个月最后一天日期字符串
	 * @return 下个月最后一天日期字符串
	 */
	public static String getNextMonthLastDay() {
		String str = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar lastDate = Calendar.getInstance();
		lastDate.add(Calendar.MONTH, 1);
		lastDate.set(Calendar.DATE, 1);
		lastDate.roll(Calendar.DATE, -1);
		str = sdf.format(lastDate.getTime());
		return str;
	}

	/**
	 * 换取指定日期前多少天后多少天的日期，如DateUtils.getBeforeAfterDate("2013-02-08",-1)-->2013-02-07
	 * @param dateTime 日期字符串
	 * @param day 相对天数，为正数表示之后，为负数表示之前
	 * @return 指定日期字符串n天之前或者之后的日期
	 */
	public static String getBeforeAfterDate(String dateTime, int day) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date oldDate = null;
		try {
			df.setLenient(false);
			oldDate = new Date(df.parse(dateTime).getTime());
		} catch (ParseException e) {
			throw new RuntimeException("Time format["+dateTime+"] is error ! format must be 'yyyy-MM-dd'！");
		}
		Calendar cal = new GregorianCalendar();
		cal.setTime(oldDate);
		int Year = cal.get(Calendar.YEAR);
		int Month = cal.get(Calendar.MONTH);
		int Day = cal.get(Calendar.DAY_OF_MONTH);
		int NewDay = Day + day;
		cal.set(Calendar.YEAR, Year);
		cal.set(Calendar.MONTH, Month);
		cal.set(Calendar.DAY_OF_MONTH, NewDay);
		Date date = new Date(cal.getTimeInMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(date);
	}



}
