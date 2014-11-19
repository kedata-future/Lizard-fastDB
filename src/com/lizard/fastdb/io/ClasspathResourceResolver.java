package com.lizard.fastdb.io;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lizard.fastdb.util.ClassLoaderUtils;

/**
 * Classpath环境下资源解析器 <br>
 * <ul>
 * <li>支持解析文件名通配符匹配</li>
 * <li>支持匹配jar包中的所有文件</li>
 * </ul>
 * 
 * @author SHEN.GANG
 */
public class ClasspathResourceResolver implements ResourceResolver
{
	private static final Log	logger	= LogFactory.getLog(ClasspathResourceResolver.class);

	/**
	 * 获取匹配的多个资源
	 * 
	 * @param location
	 * @return
	 * @throws IOException
	 */
	public Set<URL> getResources(String location)
	{
		if ( null == location )
		{
			return null;
		}

		location = location.trim().replace(File.separator, "/");
		
		Set<URL> resources = null;
		
		try
		{
			// 通配符匹配
			if ( ResourceUtils.isPattern(location) )
			{
				resources = findAllPathMatchingResources(location);
			}
			else
			{
				resources = findAllClasspathResources(location);
			}
			
		} catch (IOException e)
		{
			logger.error("Resource Resolver parse the ["+ location +"] failed!", e);
		}
		
		return resources;
	}

	/**
	 * 获取所有通配符匹配的文件资源(包括jar包中的)
	 * 
	 * @param locationPattern
	 *            含有通配符的文件路径
	 * @return 匹配的资源
	 * @throws IOException
	 */
	protected Set<URL> findAllPathMatchingResources(String locationPattern) throws IOException
	{
		// 获取协议
		String protocol = ResourceUtils.getProtocol(locationPattern);
		
		// 去除过滤协议后的资源路径
		String real_path = ResourceUtils.clearProtocol(locationPattern);
		
		// 顶级目录：config/*.xml --> rootDirPath = config/
		String rootDirPath = ResourceUtils.getRootDir(real_path);
		// 匹配文件名: config/*.xml --> subPattern = *.xml
		String subPattern = real_path.substring(rootDirPath.length());
		
		// 通过 rootDirPath 获取所有目录资源
		//Resource[] rootDirResources = getResources( protocol + rootDirPath );
		// 由于rootDirPath不可能也不允许含有通配符，所以直接findAllClasspathResources
		Set<URL> rootDirResources = findAllClasspathResources( protocol + rootDirPath );
		
		// 所有Jar包root资源
		Set<URL> allJarRootResources = null;
		
		// 如果 rootDirPath == "" ，则只会加载当前classpath下的资源，不会加载jar包
		// 这里需要对 rootDirPath == "" 在处理jar时进行特殊处理：加载所有jar资源
		if( "".equals(rootDirPath) && (ResourceUtils.isJarProtocol(protocol) || protocol.length() == 0 ) )
		{
			// 获取当前环境下的所有jar包资源
			// allJarRootResources = getResources(ResourceUtils.ALL_JAR_PATTERN);
			// 由于 ResourceUtils.ALL_JAR_PATTERN 是完整资源路径，所以直接 ResourceUtils.ALL_JAR_PATTERN
			allJarRootResources = findAllClasspathResources(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.ALL_JAR_PATTERN);
		}
		
		Set<URL> result = new LinkedHashSet<URL>();

		// 在获取的所有资源中查找匹配的文件资源
		if( null != rootDirResources && rootDirResources.size() > 0 )
		{
			for ( URL rootDirURL : rootDirResources )
			{
				// 如果是jar包资源，则在jar包资源中匹配
				if ( ResourceUtils.isJarURL(rootDirURL) )
				{
					result.addAll(doFindPathMatchingJarResources(rootDirURL, subPattern));
				}
				// 在文件系统中匹配
				else
				{
					result.addAll(doFindPathMatchingFileResources(rootDirURL, subPattern));
				}
			}
		}
		
		// 如果查找了所有jar资源
		if( null != allJarRootResources && allJarRootResources.size() > 0 )
		{
			for ( URL rootJarURL : allJarRootResources )
			{
				result.addAll(doFindPathMatchingJarResources(rootJarURL, subPattern));
			}
		}

		if ( logger.isDebugEnabled() )
		{
			logger.debug("Resolved location pattern [" + locationPattern + "] resources: "+ result.size());
		}

		return result;
	}
	
	/**
	 * 根据资源完整路径(不含通配符)，获取classpath下所有相应资源(包括jar包)
	 * 
	 * @param location 资源完整路径(不含通配符)，其可能含有classpath:, jar:协议或没有
	 * @return 匹配的资源
	 * @throws IOException
	 */
	protected Set<URL> findAllClasspathResources(String location) throws IOException
	{
		String protocol = ResourceUtils.getProtocol(location);
		String path = ResourceUtils.clearProtocol(location);
		
		if ( path.startsWith("/") )
		{
			path = path.substring(1);
		}
		
		Set<URL> result = new LinkedHashSet<URL>(16);
		
		Enumeration<URL> resourceUrls = ClassLoaderUtils.getClassLoader(ClasspathResourceResolver.class).getResources(path);
		URL url = null;
		
		// 无协议，加载全部资源
		if( "".equals(protocol) )
		{
			while (resourceUrls.hasMoreElements())
			{
				url = resourceUrls.nextElement();
				result.add(url);
			}
		}
		// 只加载jar包资源
		else if( ResourceUtils.isJarProtocol(protocol) )
		{
			while (resourceUrls.hasMoreElements())
			{
				url = resourceUrls.nextElement();
				if( ResourceUtils.isJarURL(url) )
				{
					result.add(url);
				}
			}
		}
		// 只加载classpath资源
		else if( ResourceUtils.isClasspathProtocol(protocol) )
		{
			while (resourceUrls.hasMoreElements())
			{
				url = resourceUrls.nextElement();
				if( ResourceUtils.isFileURL(url) )
				{
					result.add(url);
				}
			}
		}
		
		return result;
	}

	/**
	 * 在jar包资源中查找匹配的资源
	 * 
	 * @param rootDirResource
	 *            待查找的jar目录资源
	 * @param subPattern
	 *            匹配模式
	 * @return 匹配的资源
	 * @throws IOException
	 */
	protected Set<URL> doFindPathMatchingJarResources(URL rootJarDirURL, String subPattern)
			throws IOException
	{
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean newJarFile = false;
		
		String jar_path = rootJarDirURL.getPath();
		
		// 如果一个JAR URL资源指向了 META-INF/MANIFEST.MF 说明是为了获取所有的JAR包资源而是用的特殊方式；
		// 需要将这个 JAR URL 重置为 去除 META-INF/MANIFEST.MF 后的新 URL
		if( jar_path.endsWith(ResourceUtils.ALL_JAR_PATTERN) )
		{
			jar_path = jar_path.substring(0, jar_path.lastIndexOf(ResourceUtils.ALL_JAR_PATTERN));
			rootJarDirURL = new URL( ResourceUtils.JAR_URL_PREFIX + jar_path );
		}
		
		URLConnection con = rootJarDirURL.openConnection();

		if ( con instanceof JarURLConnection )
		{
			// Should usually be the case for traditional JAR files.
			JarURLConnection jarCon = (JarURLConnection) con;
			jarCon.setUseCaches(false);
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
		}
		else
		{
			// No JarURLConnection -> need to resort to URL file parsing.
			// We'll assume URLs of the format "jar:path!/entry", with the protocol
			// being arbitrary as long as following the entry format.
			// We'll also handle paths with and without leading "file:" prefix.
			String urlFile = rootJarDirURL.getFile();
			int separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
			if (separatorIndex != -1) 
			{
				jarFileUrl = urlFile.substring(0, separatorIndex);
				rootEntryPath = urlFile.substring(separatorIndex + ResourceUtils.JAR_URL_SEPARATOR.length());
				jarFile = getJarFile(jarFileUrl);
			}
			else 
			{
				jarFile = new JarFile(urlFile);
				jarFileUrl = urlFile;
				rootEntryPath = "";
			}
			newJarFile = true;
		}

		try
		{
			if ( logger.isDebugEnabled() )
			{
				logger.debug("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}

			if ( !"".equals(rootEntryPath) && !rootEntryPath.endsWith("/") )
			{
				// Root entry path must end with slash to allow for proper matching.
				rootEntryPath = rootEntryPath + "/";
			}
			
			// 处理匹配正则
			String _subpattern = ResourceUtils.dealPattern(subPattern);
			
			Set<URL> result = new LinkedHashSet<URL>(8);
			// 遍历jarEntry 匹配文件资源
			for ( Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); )
			{
				JarEntry entry = entries.nextElement();
				if( entry.isDirectory() )
				{
					continue;
				}
				
				String entryPath = entry.getName();
				if ( entryPath.startsWith(rootEntryPath) )
				{
					String relativePath = entryPath.substring(rootEntryPath.length());
					// 如果文件匹配
					if ( relativePath.matches(_subpattern) )
					{
						result.add(toJarURL(rootJarDirURL, relativePath));
					}
				}
			}

			return result;
		} finally
		{
			// Close jar file, but only if freshly obtained - not from
			// JarURLConnection, which might cache the file reference.
			if ( newJarFile )
			{
				jarFile.close();
			}
		}
	}

	/**
	 * 在系统中查找匹配的资源
	 * 
	 * @param rootDirResource
	 *            待查找的jar目录资源
	 * @param subPattern
	 *            匹配模式
	 * @return 匹配的资源
	 */
	protected Set<URL> doFindPathMatchingFileResources(URL rootDirURL, String subPattern)
	{
		File rootDir;
		try
		{
			rootDir = ResourceUtils.getFile(rootDirURL);

			if ( !rootDir.exists() )
			{
				if ( logger.isDebugEnabled() )
				{
					logger.debug("Skipping [" + rootDir.getAbsolutePath() + "] because it does not exist");
				}
				return Collections.emptySet();
			}

			if ( !rootDir.isDirectory() )
			{
				if ( logger.isWarnEnabled() )
				{
					logger.warn("Skipping [" + rootDir.getAbsolutePath() + "] because it does not denote a directory");
				}
				return Collections.emptySet();
			}

			if ( !rootDir.canRead() )
			{
				if ( logger.isWarnEnabled() )
				{
					logger.warn("Cannot search for matching files underneath directory [" + rootDir.getAbsolutePath()
							+ "] because the application is not allowed to read the directory");
				}
				return Collections.emptySet();
			}

			Set<URL> results = new LinkedHashSet<URL>(8);
			doRetrieveMatchingFiles(rootDir, subPattern, results);
			
			return results;
			
		} catch (IOException ex)
		{
			if ( logger.isWarnEnabled() )
			{
				logger.warn("Cannot search for matching files underneath " + rootDirURL
						+ " because it does not correspond to a directory in the file system", ex);
			}

			return Collections.emptySet();
		}
	}

	/**
	 * 递归遍历文件系统目录，匹配文件
	 * 
	 * @param dir 目录
	 * @param subPattern 匹配模式
	 * @param results 返回匹配命中的文件
	 * @throws MalformedURLException 
	 */
	protected void doRetrieveMatchingFiles(File dir, String subPattern, Set<URL> results) throws MalformedURLException
	{
		if ( logger.isDebugEnabled() )
		{
			logger.debug("Searching directory [" + dir.getAbsolutePath() + "] for files matching pattern ["+ subPattern + "]");
		}

		File[] dirContents = dir.listFiles();
		if ( dirContents == null || dirContents.length == 0 )
		{
			return;
		}

		// 是否需要递归子目录
		boolean isCascade = subPattern.startsWith(ResourceUtils.ALL_FILE_PATTERN);
		String _subPattern = ResourceUtils.dealPattern( subPattern );
		
		// 遍历目录匹配文件
		for ( File _file : dirContents )
		{
			// 是否需要级联递归子目录
			if ( isCascade && _file.isDirectory())
			{
				if ( !_file.canRead() )
				{
					if ( logger.isDebugEnabled() )
					{
						logger.debug("Skipping subdirectory [" + dir.getAbsolutePath()+ "] because the application is not allowed to read the directory");
					}
				}
				else
				{
					doRetrieveMatchingFiles( _file, subPattern, results);
				}
			}
			
			if ( _file.isFile() && _file.getName().matches(_subPattern) )
			{
				results.add(_file.toURI().toURL());
			}
		}
	}

	/**
	 * 对一个jar URL进行特殊处理：对使用ResourceUtils.ALL_JAR_PATTERN特殊获取的所有jar URL 修改为jar root URL.
	 * <br>
	 * 例如：使用ResourceUtils.ALL_JAR_PATTERN查找所有jar中的a.xml，返回的URL是：..jar!/META-INF/a.xml，
	 * 需要处理为：..jar!/a.xml。
	 * 
	 * @param jarRootUrl
	 * @param filename
	 * @return
	 * @throws MalformedURLException
	 */
	protected URL toJarURL( URL jarRootUrl, String filename ) throws MalformedURLException
	{
		if( jarRootUrl.getPath().endsWith(ResourceUtils.ALL_JAR_PATTERN) )
		{
			String jarPath = jarRootUrl.getPath();
			jarPath = jarPath.substring(0, jarPath.lastIndexOf(ResourceUtils.ALL_JAR_PATTERN));
			jarPath = ResourceUtils.JAR_URL_PREFIX + jarPath;
			return new URL( jarPath + filename );
		}
		else
		{
			return new URL( jarRootUrl, filename );
		}
	}

	/**
	 * 根据jar文件路径，获取jar文件对象
	 * 
	 * @param jarFilePath
	 *            jar文件路径
	 * @return JarFile
	 * @throws IOException
	 */
	private JarFile getJarFile(String jarFileUrl) throws IOException
	{
		if ( jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX) )
		{
			try
			{
				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
			} catch (URISyntaxException ex)
			{
				// Fallback for URLs that are not valid URIs (should hardly ever happen).
				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
			}
		}
		else
		{
			return new JarFile(jarFileUrl);
		}
	}
	
}
