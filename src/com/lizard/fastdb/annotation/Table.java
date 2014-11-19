package com.lizard.fastdb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table
{
	/**
	 * 数据库表名
	 */
	public abstract String name();
	
	/**
	 * 表别名，常用于在自动生成SELECT语句时用于AS使用
	 */
	public abstract String alias() default "";
}
