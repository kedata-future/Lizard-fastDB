package com.lizard.fastdb.util;

import java.util.Random;

/**
 * 随机字符串工具类，用于按指定条件获得随机字符串
 * 
 * @author SHEN.GANG
 */
public class RandomUtils
{

	/**
	 * 随机字符数字串库
	 */
	private static final String RANDOM_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	/**
	 * 随机数字
	 */
	private static final String RANDOM_NUMBER = "0123456789";

	/**
	 * 创建随机数，由a-zA-Z0-9字串随机组成
	 * 
	 * @param len 随机数长度
	 * @return 随机数
	 */
	public static String randomString(int len) {
		if (len <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Random radom = new Random();
		for (int r = 0; r < len; r++) {
			// 返回[0 62)之间的随机int数
			int at = radom.nextInt(62);
			// 从 RANDOM_STRING 中取出随机的字符
			sb.append(RANDOM_STRING.charAt(at));
		}

		return sb.toString();
	}

	/**
	 * 创建随机数，由0-9数字随机组成
	 * 
	 * @param len 随机数长度
	 * @return 随机数
	 */
	public static String randomNumber(int len) {
		if (len <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Random radom = new Random();
		for (int r = 0; r < len; r++) {
			// 返回[0 62)之间的随机int数
			int at = radom.nextInt(10);
			// 从 RANDOM_STRING 中取出随机的字符
			sb.append(RANDOM_NUMBER.charAt(at));
		}

		return sb.toString();
	}
	
}
