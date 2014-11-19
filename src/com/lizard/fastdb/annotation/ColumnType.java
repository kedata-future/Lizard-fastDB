package com.lizard.fastdb.annotation;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class ColumnType
{
	/**
	 * 基本数据类型定义
	 */
	private final static List<Class<?>>	PrimitiveClasses	= new ArrayList<Class<?>>() {
																{
																	add(Long.class);
																	add(Integer.class);
																	add(Double.class);
																	add(Float.class);
																	add(Short.class);
																	add(Character.class);
																	add(Boolean.class);
																	add(String.class);
																	add(java.util.Date.class);
																	add(java.sql.Date.class);
																	add(java.sql.Timestamp.class);
																}
															};

	/**
	 * 数值型
	 */
	private final static List<String>	NumberTypes			= new ArrayList<String>() {
																{
																	add("int");
																	add("integer");
																	add("biginteger");
																	add("long");
																	add("double");
																	add("float");
																	add("short");
																	add("char");
																	add("number");
																}
															};

	/**
	 * 判断一个Class是否是基本数据类型
	 * 
	 * @param cls
	 *            要进行判断的类
	 * @return 是否是基本数据类型
	 */
	public final static boolean isPrimitiveClass(Class<?> cls)
	{
		return cls.isPrimitive() || PrimitiveClasses.contains(cls);
	}

	/**
	 * 判断一个变量类型是否是数值型
	 * 
	 * @param cls
	 *            变量类型
	 * @return true -- 是数值类型， 否则 false
	 */
	public final static boolean isNumberType(Class<?> cls)
	{
		return NumberTypes.contains((cls.getSimpleName()).toLowerCase());
	}

}
