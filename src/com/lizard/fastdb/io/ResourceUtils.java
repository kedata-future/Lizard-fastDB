package com.lizard.fastdb.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ResourceUtils
{
	/** 
	 * URL 文件系统协议文件标识，用于标识一个资源URL是文件系统下的文件资源 
	 */
	public static final String	URL_PROTOCOL_FILE		= "file";

	/**
	 * 加载文件系统下文件资源的URL协议前缀
	 * 
	 */
	public static final String	FILE_URL_PREFIX			= "file:";
	
	/** 
	 * URL Jar协议文件标识，用于标识一个资源URL是Jar Entry
	 */
	public static final String	URL_PROTOCOL_JAR		= "jar";
	
	/**
	 * 加载Jar资源的URL协议前缀
	 */
	public static final String	JAR_URL_PREFIX			= "jar:";

	/**
	 * JAR URL资源的文件分割符：aa.jar!/config/cfg.xml
	 */
	public static final String	JAR_URL_SEPARATOR		= "!/";

	/**
	 * 只解析当前classpath下的资源
	 */
	public static final String	CLASSPATH_URL_PREFIX	= "classpath:";

	/**
	 * 匹配所有文件，包括递归下级所有目录下的文件
	 */
	public static final String	ALL_FILE_PATTERN		= "**";

	/**
	 * 由于无法通过/获取当前环境下的jar资源，这些需要使用jar包中特殊的 META-INF/MANIFEST.MF文件间接加载
	 */
	public static final String	ALL_JAR_PATTERN			= "META-INF/MANIFEST.MF";

	
	/**
	 * 将一个URL转换为File
	 * <br>
	 * 对于jar资源无法转换为 File，因为jar资源不是标准的File
	 * 
	 * @param resourceUrl
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getFile(URL resourceUrl) throws FileNotFoundException
	{
		if ( !URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol()) )
		{
			throw new FileNotFoundException("Cannot be resolved to absolute file path "
					+ "because it does not reside in the file system: " + resourceUrl);
		}
		try
		{
			return new File(toURI(resourceUrl).getSchemeSpecificPart());
		} catch (URISyntaxException ex)
		{
			return new File(resourceUrl.getFile());
		}
	}

	/**
	 * 将一个URI转换为File
	 * <br>
	 * 对于jar资源无法转换为 File，因为jar资源不是标准的File
	 * 
	 * @param resourceUri
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getFile(URI resourceUri) throws FileNotFoundException
	{
		if ( !URL_PROTOCOL_FILE.equals(resourceUri.getScheme()) )
		{
			throw new FileNotFoundException("Cannot be resolved to absolute file path "
					+ "because it does not reside in the file system: " + resourceUri);
		}

		return new File(resourceUri.getSchemeSpecificPart());
	}

	/**
	 * 将一个URL转换为URI
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI toURI(URL url) throws URISyntaxException
	{
		return toURI(url.toString());
	}

	/**
	 * 将一个路径资源符转换为URI
	 * 
	 * @param location
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI toURI(String location) throws URISyntaxException
	{
		return new URI(location.replaceAll(" ", "%20"));
	}

	/**
	 * 判断一个URL是否是jar URL
	 * 
	 * @param url
	 * @return
	 */
	public static boolean isJarURL(URL url)
	{
		return URL_PROTOCOL_JAR.equals(url.getProtocol());
	}
	
	/**
	 * 判断一个URL是否是系统文件 URL
	 * 
	 * @param url
	 * @return
	 */
	public static boolean isFileURL(URL url)
	{
		return URL_PROTOCOL_FILE.equals(url.getProtocol());
	}
	
	/**
	 * 获取资源前面的过滤协议：jar, classpath
	 * 
	 * @param location 资源路径
	 * @return
	 */
	public static String getProtocol( String location )
	{
		int protocol_pos = location.indexOf(":");
		if( protocol_pos != -1 )
		{
			return location.substring(0, protocol_pos + 1).trim();
		}
		
		return "";
	}
	
	/**
	 * 去掉资源路径前面的协议
	 * 
	 * @param location
	 * @return
	 */
	public static String clearProtocol( String location )
	{
		int protocol_pos = location.indexOf(":");
		if( protocol_pos != -1 )
		{
			return location.substring(protocol_pos + 1).trim();
		}
		
		return location;
	}
	
	/**
	 * 获取文件路径的顶级目录 <br>
	 * 例如：config/*.xml --> rootDir = config/
	 * 
	 * @param location 文件路径
	 * @return 顶级目录
	 */
	public static String getRootDir(String location)
	{
		String path = clearProtocol(location);
		int last_slash = path.lastIndexOf("/");
		path = path.substring(0, last_slash+1);
		
		return path;
	}

	/**
	 * 判断一个文件路径是否是通配符模糊路径
	 * 
	 * @param path
	 * @return
	 */
	public static boolean isPattern(String path)
	{
		return path.indexOf("*") != -1;
	}
	
	/**
	 * 处理匹配正则
	 * 
	 * @param path
	 * @return
	 */
	public static String dealPattern(String path)
	{
		if ( path.startsWith(ResourceUtils.ALL_FILE_PATTERN) )
		{
			path = path.replace(".", "\\.").replace("**", ".*");
		}
		else
		{
			// 使用[^/]* --> * 表示0次或多次，这样：a*.xml 就可以匹配 a.xml
			// path = path.replace(".", "\\.").replace("*", "[^/]+");
			path = path.replace(".", "\\.").replace("*", "[^/]*");
		}
		
		return path;
	}
	
	public static boolean isJarProtocol( String protocol )
	{
		return JAR_URL_PREFIX.equals(protocol);
	}
	
	public static boolean isClasspathProtocol( String protocol )
	{
		return CLASSPATH_URL_PREFIX.equals(protocol);
	}
	
}
