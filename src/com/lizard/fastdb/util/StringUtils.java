package com.lizard.fastdb.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 常用字符串操作类，继承自apache的StringUtils
 * 
 * @author lqin
 * 
 */
public class StringUtils extends org.apache.commons.lang.StringUtils {

	private static final String EMPTY = "";
	
	/**
	 * 检测字符串是否为空,对于 null、空格、空串、"NULL" 均认定为空白字符串
	 * 
	 * @param str
	 *            需要检测的字符串
	 * @return true=所检测字符串为空 false=所检测字符串不为空
	 */
	public static boolean isEmptyString(String str) {
		return (str == null 
				|| "".equals(str.trim()) 
				|| str.length() <= 0 
				|| "NULL".equals(str.toUpperCase()));
	}

	/**
	 * 将字符串中html特殊字符替换成转义字符
	 * 
	 * @param str
	 *            需要处理的字符串
	 * @return 替换后的字符串
	 */
	public static String replaceHtmlChars(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return EMPTY;
		} 
		else {
			return str.replaceAll("&", "&amp;").replaceAll("\"", "&quot;")
					.replaceAll("\'", "&#039;").replaceAll("<", "&lt;")
					.replaceAll(">", "&gt;");
		}
	}

	/**
	 * 将字符串中被替换的html转义字符还原成html特殊字符
	 * 
	 * @param str
	 *            需要处理的字符串
	 * @return 还原后的字符串
	 */
	public static String restoreHtmlChars(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return EMPTY;
		} 
		else {
			return str.replaceAll("&amp;", "&").replaceAll("&quot;", "\"")
					.replaceAll("&#039;", "\'").replaceAll("&lt;", "<")
					.replaceAll("&gt;", ">");
		}
	}

	/**
	 * 检测字符串是否具有正确的Email格式
	 * 
	 * @param str
	 *            需要检测的字符串
	 * @return true=是 false=否
	 */
	public static boolean isEmail(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return false;
		} 
		else {
			str = str.trim();
			String test = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";
			Pattern pattern = Pattern.compile(test);
			Matcher matcher = pattern.matcher(str);

			return matcher.matches();
		}
	}

	/**
	 * 检测字符串是否具有正确的IP地址格式
	 * 
	 * @param str
	 *            需要检测的字符串
	 * @return true=是 false=否
	 */
	public static boolean isIP(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return false;
		} 
		else {
			str = str.trim();
			String test = "^(\\d|[0-9]\\d|[01]\\d\\d|[2][0-4]\\d|[2][5][0-5])"
					+ "(\\.(\\d|[0-9]\\d|[01]\\d\\d|[2][0-4]\\d|[2][5][0-5])){3}$";
			Pattern pattern = Pattern.compile(test);
			Matcher matcher = pattern.matcher(str);

			return matcher.matches();
		}
	}

	/**
	 * 检测字符串是否是汉字
	 * 
	 * @param str
	 *            需要检测的字符串
	 * @return true=是 false=否
	 */
	public static boolean isChinese(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return false;
		} 
		else {
			str = str.trim();
			Pattern pattern = Pattern.compile("^[\\u4e00-\\u9fa5]+$");
			Matcher matcher = pattern.matcher(str);
			return matcher.matches();
		}
	}

	/**
	 * 提取字符串中的中文信息
	 * 
	 * @param str
	 *            需要提取的字符串
	 * @return 字符串中的中文内容
	 */
	public static String extractChinese(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return EMPTY;
		} else {
			return str.trim().replaceAll("[^\\u4e00-\\u9fa5]", "");// 去除非中文字符
		}
	}
	
	/**
	 * 提取html节点中的文本内容
	 * 
	 * @param htmlContent
	 * @return 纯文本内容
	 */
	public static String extractText( String htmlContent )
	{
		String text = restoreHtmlChars(htmlContent).replaceAll("<\\/?[^>]+>", "");
		
		return text;
	}
	
	/**
	 * 不区分大小写高亮关键词<br/>
	 * 支持多个关键词，多个关键词使用 空格 分隔；例如： 关键词1 关键词2
	 * 
	 * @param content 内容
	 * @param key 关键词
	 * @param htmlFormatter 自定义的html标签高亮格式，例如：&lt;font color='red'&gt;&lt;/font&gt;<br/>
	 * 默认高亮格式：&lt;b style="color:#ff0000;"&gt;&lt;/b&gt;
	 * 
	 * @return 高亮后的内容
	 */
	public static String highlight( String content, String key, String... htmlFormatter )
	{
		return highlight( content, key, true, htmlFormatter );
	}
	
	/**
	 * 高亮关键词<br/>
	 * 支持多个关键词，多个关键词使用 空格 分隔；例如： 关键词1 关键词2
	 * 
	 * @param content 内容
	 * @param key 关键词
	 * @param ignoreCase 是否区分大小写
	 * @param htmlFormatter 自定义的html标签高亮格式，例如：&lt;font color='red'&gt;&lt;/font&gt;<br/>
	 * 默认高亮格式：&lt;b style="color:#ff0000;"&gt;&lt;/b&gt;
	 * 
	 * @return 高亮后的内容
	 */
	public static String highlight( String content, String key, boolean ignoreCase, String... htmlFormatter )
	{
		if( isEmptyString( key ) ){
			return content;
		}
		
		String pt = (htmlFormatter != null&&htmlFormatter.length > 0) ? htmlFormatter[0] : "<b style=\"color:#ff0000;\"></b>";
		pt = restoreHtmlChars(pt).replaceAll( "(?i)(<(\\w+)(\\s+[^<]*)*>)([^<]*)(</\\2>)", "$1\\$1$5" );
		
		// 处理以空格分隔的多关键词，需要对正则特殊字符进行转移
		String[] keys = encodeRegExpChar(key).split("\\s+");
		
		List<String> keyList = new ArrayList<String>();
		String _key = "";
		for( int i = 0, len = keys.length; i < len; i++ )
		{
			_key = keys[i].trim();
			
			if( !isEmptyString( _key ) && !keyList.contains( _key ) )
			{
				keyList.add( _key );
			}
		}
		_key = null;
		
		String _content = content;
		// 高亮关键词
		for( int i = 0, len = keyList.size(); i < len; i++)
		{
			//_content = _content.replaceAll("(?![^&;]+;)(?!<[^<>]*)("+(ignoreCase?"(?i)":"") + keyList.get(i) + ")(?![^<>]*>)(?![^&;]+;)", pt);
			_content = _content.replaceAll("(?!<[^<>]*)("+(ignoreCase?"(?i)":"") + keyList.get(i) + ")(?![^<>]*>)", pt);
		}
		
		return _content;
	}
	
	/**
	 * 转义关键词中含有的正则特殊字符
	 * 
	 * @param str
	 * @return
	 */
	private static String encodeRegExpChar( String str )
	{
		return str.replaceAll("\\\\", "\\\\\\\\")
				  .replaceAll("\\.", "\\\\.")
				  .replaceAll("\\$", "\\\\\\$")
				  .replaceAll("\\^", "\\\\^")
				  .replaceAll("\\{", "\\\\{")
				  .replaceAll("\\}", "\\\\}")
				  .replaceAll("\\[", "\\\\[")
				  .replaceAll("\\]", "\\\\]")
				  .replaceAll("\\(", "\\\\(")
				  .replaceAll("\\)", "\\\\)")
				  .replaceAll("\\|", "\\\\|")
				  .replaceAll("\\*", "\\\\*")
				  .replaceAll("\\+", "\\\\+")
				  .replaceAll("\\?", "\\\\?");
	}

	/**
	 * 检测字符串是否具有正确的日期格式，支持"年-月-日"和"年/月/日"日期格式
	 * 
	 * @param str
	 *            需要检测的字符串
	 * @return true=正确 false=不正确
	 */
	public static boolean isDateTime(String str) {
		if (isEmpty(str)){// 检查是否空字符串
			return false;
		} 
		else {
			str = str.trim();
			String test = "^\\d{4}-\\d{1,2}-\\d{1,2}|\\d{4}/\\d{1,2}/\\d{1,2}$";
			Pattern pattern = Pattern.compile(test);
			Matcher matcher = pattern.matcher(str);

			if (!matcher.matches()){// 检查格式是否正确
				return false;
			} 
			else {
				String strSeparator = str.substring(4, 5);
				String[] date = str.split(strSeparator);
				int year = Integer.parseInt(date[0]);
				int month = Integer.parseInt(date[1]);
				int day = Integer.parseInt(date[2]);

				if (month < 1 || month > 12){// 检查月份知否正确
					return false;
				} 
				else {
					int[] monthLengths = { 0, 31, isLeapYear(year) ? 29 : 28,
							31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

					if (day < 1 || day > monthLengths[month]){// 检查日期是否正确
						return false;
					} 
					else {
						return true;
					}
				}
			}
		}
	}

	/**
	 * 判断年份是否闰年
	 * 
	 * @param year
	 *            需要判断的年份值
	 * @return true=是闰年 false=不是闰年
	 */
	private static boolean isLeapYear(int year) {
		return ((0 == year % 4 && 0 != year % 100) || (0 == year % 4 && 0 == year % 400));
	}

	/**
	 * 返回指定数组内容的字符串表示形式,元素间以逗号分隔。若array为null，返回空字符串
	 * 
	 * @param array 返回其字符串表示形式的数组，支持基本类型数组和对象数组
	 * @param asString 数组元素保存格式,true--字符串 false--数字。若是字符串，则以单引号包围数组元素。
	 * 
	 * @return array的字符串表示形式
	 * @throws IllegalArgumentException -若array不是数组
	 */
	public static String join(Object array, boolean asString){
		return join(array, ",", asString);
	}
	
	/**
	 * 返回指定数组内容的字符串表示形式,元素间以separator分隔。若array为null，返回空字符串
	 * 
	 * @param array 返回其字符串表示形式的数组，支持基本类型数组和对象数组
	 * @param separator 数组元素分隔符
	 * @param asString 数组元素保存格式,true--字符串 false--数字。若是字符串，则以单引号包围数组元素。
	 * 
	 * @return array的字符串表示形式
	 * @throws IllegalArgumentException -若array不是数组
	 */
	public static String join(Object array, String separator, boolean asString){
		if (array == null) {
			return EMPTY;
		}
		
		if (array.getClass().isArray()) {
			int arraylength = Array.getLength(array);
			return join(array,separator,asString,0,arraylength-1);
		}
		else {
			throw new IllegalArgumentException("StringUtils.join():The param [Object array] must be a array!");
		}
	}
	
	/**
	 * 返回指定数组中索引startIndex到endIndex的内容的字符串表示形式,元素间以separator分隔。若array为null，返回空字符串
	 * 
	 * @param array 返回其字符串表示形式的数组，支持基本类型数组和对象数组
	 * @param separator 数组元素分隔符
	 * @param asString 数组元素保存格式,true--字符串 false--数字。若是字符串，则以单引号包围数组元素。
	 * @param startIndex 起始元素索引
	 * @param endIndex 结束元素索引
	 * 
	 * @return array的字符串表示形式
	 * @throws IllegalArgumentException -若array不是数组
	 */
	public static String join(Object array, String separator, boolean asString, int startIndex, int endIndex) {
		if (array == null) {
			return EMPTY;
		}
		
		if (array.getClass().isArray()) {
			StringBuilder buffer = new StringBuilder();
			
			separator = (separator == null) ? "" : separator;
			 
			for( int i = startIndex; i <= endIndex; i++)
			{
				Object dataEle = Array.get(array, i);
				
				if(dataEle != null){
					dataEle = asString ? "'"+ dataEle +"'" : dataEle.toString();
					buffer.append(dataEle);
				}
				
				if( i < endIndex ){
					buffer.append(separator);
				}
			}
			
			return buffer.toString();
		} 
		else {
			throw new IllegalArgumentException("StringUtils.join():The param [Object array] must be a array!");
		}
	}

	/**
	 * 返回指定集合中内容的字符串表示形式，元素间以逗号分隔。若collection为null，则返回空字符串
	 * 
	 * @param collection 返回其字符串表示形式的集合
	 * @param asString 数组元素保存格式,true--字符串 false--数字。若是字符串，则以单引号包围数组元素。
	 * 
	 * @return collection的字符串表示形式
	 */
	public static <T> String join(Collection<T> collection, boolean asString){
		return join(collection,",",asString);
	}
	
	/**
	 * 返回指定集合中内容的字符串表示形式，元素间以separator分隔。若collection为null，则返回空字符串
	 * 
	 * @param collection 返回其字符串表示形式的集合
	 * @param separator 集合元素分隔符
	 * @param asString 数组元素保存格式,true--字符串 false--数字。若是字符串，则以单引号包围数组元素。
	 * 
	 * @return collection的字符串表示形式
	 */
	public static <T> String join(Collection<T> collection, String separator, boolean asString){
		if(collection == null){
			return EMPTY;
		}
		
		return join(collection.toArray(),separator,asString);
	}
	
	
	/**
	 * 将中文转换成UTF8编码
	 * 
	 * @param str
	 * @return
	 */
	public static String toUTF8( String str )
	{
		if( isEmptyString(str) )
		{
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		char _char;
		for( int i = 0, len = str.length(); i < len; ++i )
		{
			_char = str.charAt(i);
			if( _char >= 0 && _char <= 255 )
			{
				sb.append(_char);
			}
			else
			{
				byte[] bs;
				try
				{
					bs = Character.toString(_char).getBytes("utf-8");
					for( int j = 0, blen = bs.length; j < blen; ++j )
					{
						int b = bs[j];
						if( b < 0 )
						{
							b += 256;
						}
						sb.append("%");
						sb.append(Integer.toHexString(b).toUpperCase());
					}
				} catch (UnsupportedEncodingException e)
				{
				}
			}
		}
		
		return sb.toString();
	}
}
