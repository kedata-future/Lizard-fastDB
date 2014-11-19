package com.lizard.fastdb.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * ClassLoader工具类
 * 
 * @author SHEN.GANG
 */
@SuppressWarnings("rawtypes")
public final class ClassLoaderUtils
{
	/**
	 * 加载一个给定的资源. 加载时将尝试通过下面的方法步骤获取：
	 * <ul>
	 * <li>From Thread.currentThread().getContextClassLoader()
	 * <li>From ClassLoaderUtils.class.getClassLoader()
	 * <li>callingClass.getClassLoader()
	 * </ul>
	 * 
	 * @param resourceName
	 *            资源名称(包含路径)
	 * @param callingClass
	 *            调用类class
	 * @return URL资源
	 */
	public static URL getResource(String resourceName, Class callingClass)
	{
		URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);

		if ( url == null )
		{
			url = ClassLoaderUtils.class.getClassLoader().getResource(resourceName);
		}

		if ( url == null )
		{
			ClassLoader cl = callingClass.getClassLoader();

			if ( cl != null )
			{
				url = cl.getResource(resourceName);
			}
		}

		if ( (url == null) && (resourceName != null)
				&& ((resourceName.length() == 0) || (resourceName.charAt(0) != '/')) )
		{
			return getResource('/' + resourceName, callingClass);
		}

		return url;
	}

	/**
	 * 加载给定的资源并通过流方式打开<br/> 注意：需要调用者手工关闭流，避免文件流泄露。
	 * 
	 * @param resourceName
	 *            资源名称(包含路径)
	 * @param callingClass
	 *            调用类class
	 * @return InputStream
	 */
	public static InputStream getResourceAsStream(String resourceName, Class callingClass)
	{
		URL url = getResource(resourceName, callingClass);

		try
		{
			return (url != null) ? url.openStream() : null;
		} catch (IOException e)
		{
			return null;
		}
	}

	/**
	 * 通过给定的class类名称创建其Class对象. 创建Class过程将按照下面的步骤尝试：
	 * <ul>
	 * <li>From Thread.currentThread().getContextClassLoader()
	 * <li>Using the basic Class.forName()
	 * <li>From ClassLoaderUtils.class.getClassLoader()
	 * <li>From the callingClass.getClassLoader()
	 * </ul>
	 * 
	 * @param className
	 *            完整类名称
	 * @param callingClass
	 *            调用类class
	 * @throws ClassNotFoundException
	 *             如果给定的类找不到，则抛出该异常
	 */
	public static Class loadClass(String className, Class callingClass) throws ClassNotFoundException
	{
		try
		{
			return Thread.currentThread().getContextClassLoader().loadClass(className);
		} catch (ClassNotFoundException e)
		{
			try
			{
				return Class.forName(className);
			} catch (ClassNotFoundException ex)
			{
				try
				{
					return ClassLoaderUtils.class.getClassLoader().loadClass(className);
				} catch (ClassNotFoundException exc)
				{
					return callingClass.getClassLoader().loadClass(className);
				}
			}
		}
	}

	/**
	 * 获取ClassLoader
	 * 
	 * @param callingClass
	 * @return
	 */
	public static ClassLoader getClassLoader(  Class callingClass )
	{
		ClassLoader cl;
		cl = Thread.currentThread().getContextClassLoader();
		if( null == cl )
		{
			cl = ClassLoaderUtils.class.getClassLoader();
		}
		
		if( null == cl )
		{
			cl = callingClass.getClassLoader();
		}
		
		return cl;
	}
	
	/**
	 * 获取默认ClassLoader
	 * 
	 * @return
	 */
	public static ClassLoader getDefaultClassLoader()
	{
		return getClassLoader( ClassLoaderUtils.class );
	}
}
