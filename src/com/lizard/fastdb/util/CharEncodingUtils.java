package com.lizard.fastdb.util;

/**
 * 字符编码转换工具类
 * 
 * @author SHEN.GANG
 */
public final class CharEncodingUtils {

	/**
	 * GB2312 --> ISO-8859-1
	 * @param str 需要转换的字符串,字符串的字符集为GB2312
	 * @return 字符集为GB2312的字符串为ISO-8859-1的字符串
	 */
	public static String convertGBToISO(String str) {
		if (str == null) {
			return "";
		} else {
			try {
				return new String(str.getBytes("GB2312"), "ISO-8859-1");
			} catch (Exception uee) {
				uee.printStackTrace();
				return "";
			}
		}
	}

	
	/**
	 * GB2312 --> UTF-8
	 * @param str 需要转换的字符串,字符串的字符集为GB2312
	 * @return 字符集为GB2312的字符串为UTF-8的字符串
	 */
	public static String convertGBToUTF8(String str) {
		if (str == null) {
			return "";
		} else {
			try {
				return new String(str.getBytes("GB2312"), "UTF-8");
			} catch (Exception uee) {
				uee.printStackTrace();
				return "";
			}
		}
	}

	/**
	 * ISO-8859-1 --> UTF-8
	 * @param str 需要转换的字符串,字符串的字符集为ISO-8859-1
	 * @return 字符集为ISO-8859-1的字符串为UTF-8的字符串
	 */
	public static String convertISOToUTF8(String str) {
		if (str == null) {
			return "";
		} else {
			try {
				return new String(str.getBytes("ISO-8859-1"), "UTF-8");
			} catch (Exception uee) {
				uee.printStackTrace();
				return "";
			}
		}
	}

	
	/**
	 * ISO-8859-1 --> GB2312
	 * @param str 需要转换的字符串,字符串的字符集为ISO-8859-1
	 * @return 字符集为ISO-8859-1的字符串为GB2312的字符串
	 */
	public static String convertISOToGB(String str) {
		if (str == null) {
			return "";
		} else {
			try {
				return new String(str.getBytes("ISO-8859-1"), "GB2312");
			} catch (Exception uee) {
				uee.printStackTrace();
				return "";
			}
		}
	}

	
	/**
	 * UTF-8 --> ISO-8859-1
	 * @param str 需要转换的字符串,字符串的字符集为UTF-8
	 * @return 字符集为UTF-8的字符串为ISO-8859-1的字符串
	 */
	public static String convertUTF8ToISO(String str) {
		if (str == null) {
			return "";
		} else {
			try {
				return new String(str.getBytes("UTF-8"), "ISO-8859-1");
			} catch (Exception uee) {
				uee.printStackTrace();
				return "";
			}
		}
	}

	/**
	 * UTF-8 --> GB2312
	 * @param str 需要转换的字符串,字符串的字符集为UTF-8
	 * @return 字符集为UTF-8的字符串为GB2312的字符串
	 */
	public static String convertUTF8ToGB(String str) {
		if (str == null) {
			return "";
		} else {
			try {
				return new String(str.getBytes("UTF-8"), "GB2312");
			} catch (Exception uee) {
				uee.printStackTrace();
				return "";
			}
		}
	}

}
