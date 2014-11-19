package com.lizard.fastdb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Cache {

	/**
	 * 缓存名称,默认为空
	 */
	String cacheName() default "";

	/** 
	 * 增加缓存还是删除缓存，默认为增加缓存
	 */
	boolean addOrdel() default true;
	
	/**
	 * 临时缓存还是永久缓存，默认为永久缓存
	 */
	boolean permanent() default true;
	
}
